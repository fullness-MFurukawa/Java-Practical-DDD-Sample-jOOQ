package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryName;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductRecord;

/**
 * {@link ProductRecordMapper} の単体テスト（DB 不要）。
 *
 * <p>{@code toDomain} は「カテゴリ・在庫を伴わない骨格（skeleton）」の {@link Product} を返すこと、
 * {@code fromDomain} は {@code category_id} を設定しないこと（Repository が補完する）を検証する。
 * MapStruct が生成する実装クラス {@code ProductRecordMapperImpl} を直接 new して確認する。</p>
 */
@DisplayName("ProductRecordMapper: jOOQ Record ⇔ Product（骨格）の相互変換")
class ProductRecordMapperTest {

    private final ProductRecordMapper mapper = new ProductRecordMapperImpl();

    private static final String UUID_STR = "33333333-3333-3333-3333-333333333333";

    private ProductRecord record(String productUuid, String name, Integer price) {
        ProductRecord r = new ProductRecord();
        r.setProductUuid(productUuid);
        r.setName(name);
        r.setPrice(price);
        return r;
    }

    @Nested
    @DisplayName("toDomain: Record → Product（骨格）")
    class ToDomain {

        @Test
        @DisplayName("有効な Record を骨格 Product に変換できる（カテゴリ・在庫は null）")
        void valid() {
            Product product = mapper.toDomain(record(UUID_STR, "油性ボールペン", 120));

            assertEquals(UUID_STR, product.getProductId().value());
            assertEquals("油性ボールペン", product.getName().value());
            assertEquals(120, product.getPrice().value().intValue());
            // skeleton なのでカテゴリ・在庫は未設定（後段の Assembler が attach する）
            assertNull(product.getCategory());
            assertNull(product.getStock());
        }

        @Test
        @DisplayName("Record が null なら例外")
        void nullRecord() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("product_uuid が空白なら例外")
        void blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record("  ", "商品", 120)));
        }

        @Test
        @DisplayName("name が空白なら例外")
        void blankName() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record(UUID_STR, "  ", 120)));
        }

        @Test
        @DisplayName("price が null なら例外")
        void nullPrice() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record(UUID_STR, "商品", null)));
        }

        @Test
        @DisplayName("price が範囲外（50 未満）なら例外（VO のバリデーション）")
        void outOfRangePrice() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record(UUID_STR, "商品", 10)));
        }
    }

    @Nested
    @DisplayName("fromDomain: Product → Record")
    class FromDomain {

        @Test
        @DisplayName("Product を Record に変換できる（category_id は未設定）")
        void valid() {
            Category category = Category.createNew(CategoryName.of("文房具"));
            Product product = Product.createNew(
                    ProductName.of("油性ボールペン"),
                    ProductPrice.of(120),
                    category,
                    StockQuantity.of(80));

            ProductRecord rec = mapper.fromDomain(product);

            assertEquals(product.getProductId().value(), rec.getProductUuid());
            assertEquals("油性ボールペン", rec.getName());
            assertEquals(120, rec.getPrice().intValue());
            // 外部キー category_id は Mapper では設定しない（Repository が補完する）
            assertNull(rec.getCategoryId());
        }

        @Test
        @DisplayName("Product が null なら例外")
        void nullDomain() {
            assertThrows(DomainException.class, () -> mapper.fromDomain(null));
        }
    }
}

