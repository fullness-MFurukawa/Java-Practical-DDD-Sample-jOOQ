package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryName;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;
import jp.co.fullness.ddd.infrastructure.category.ProductCategoryRecordMapper;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductStockRecord;
import jp.co.fullness.ddd.infrastructure.stock.ProductStockRecordMapper;

/**
 * {@link ProductAssembler} の単体テスト（DB 不要）。
 *
 * <p>Assembler の責務は「3つの Mapper への委譲」と「集約の合成/分解」なので、
 * 各 Mapper は Mockito でモック化し、Assembler 自身のロジック
 * （skeleton への attach 合成、null ガード、委譲）だけを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAssembler: Record群 ⇔ Product集約 の合成/分解")
class ProductAssemblerTest {

    @Mock
    private ProductRecordMapper productRecordMapper;
    @Mock
    private ProductCategoryRecordMapper categoryRecordMapper;
    @Mock
    private ProductStockRecordMapper stockRecordMapper;

    @InjectMocks
    private ProductAssembler assembler;

    private Category sampleCategory() {
        return Category.createNew(CategoryName.of("文房具"));
    }

    private Product sampleSkeleton() {
        return Product.restoreSkeleton(
                ProductId.createNew(),
                ProductName.of("油性ボールペン"),
                ProductPrice.of(120));
    }

    private Product sampleFullProduct() {
        // createNew はカテゴリと在庫を伴う完全な Product を生成する
        return Product.createNew(
                ProductName.of("油性ボールペン"),
                ProductPrice.of(120),
                sampleCategory(),
                StockQuantity.of(80));
    }

    @Nested
    @DisplayName("assemble: Record群 → Product集約 の合成")
    class Assemble {

        @Test
        @DisplayName("骨格に Category と Stock を attach して合成する")
        void success() {
            ProductRecord pr = new ProductRecord();
            ProductCategoryRecord cr = new ProductCategoryRecord();
            ProductStockRecord sr = new ProductStockRecord();

            Product skeleton = sampleSkeleton();
            Category category = sampleCategory();
            Stock stock = Stock.createNew(StockQuantity.of(80));

            // 各 Mapper は復元済みのドメインオブジェクトを返すものとしてスタブ
            when(productRecordMapper.toDomain(any())).thenReturn(skeleton);
            when(categoryRecordMapper.toDomain(any())).thenReturn(category);
            when(stockRecordMapper.toDomain(any())).thenReturn(stock);

            Product result = assembler.assemble(pr, cr, sr);

            // 返却されるのは attach 済みの skeleton そのもの
            assertSame(skeleton, result);
            assertSame(category, result.getCategory());
            assertSame(stock, result.getStock());
        }

        @Test
        @DisplayName("ProductRecord が null なら例外（Mapper は呼ばれない）")
        void nullProductRecord() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(null, new ProductCategoryRecord(), new ProductStockRecord()));
        }

        @Test
        @DisplayName("ProductCategoryRecord が null なら例外")
        void nullCategoryRecord() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(new ProductRecord(), null, new ProductStockRecord()));
        }

        @Test
        @DisplayName("ProductStockRecord が null なら例外")
        void nullStockRecord() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(new ProductRecord(), new ProductCategoryRecord(), null));
        }
    }

    @Nested
    @DisplayName("分解: Product集約 → Record")
    class Decompose {

        @Test
        @DisplayName("toProductRecord は ProductRecordMapper.fromDomain に委譲する")
        void toProductRecord_delegates() {
            Product product = sampleFullProduct();
            ProductRecord expected = new ProductRecord();
            when(productRecordMapper.fromDomain(product)).thenReturn(expected);

            assertSame(expected, assembler.toProductRecord(product));
        }

        @Test
        @DisplayName("toProductRecord は null なら例外")
        void toProductRecord_null() {
            assertThrows(DomainException.class, () -> assembler.toProductRecord(null));
        }

        @Test
        @DisplayName("toStockRecord は Product の Stock を取り出して委譲する")
        void toStockRecord_delegates() {
            Product product = sampleFullProduct();
            ProductStockRecord expected = new ProductStockRecord();
            when(stockRecordMapper.fromDomain(product.getStock())).thenReturn(expected);

            assertSame(expected, assembler.toStockRecord(product));
        }

        @Test
        @DisplayName("toStockRecord は Stock 未設定（骨格）なら例外")
        void toStockRecord_noStock() {
            assertThrows(DomainException.class, () -> assembler.toStockRecord(sampleSkeleton()));
        }

        @Test
        @DisplayName("extractCategoryUuid は Category の UUID 文字列を返す")
        void extractCategoryUuid_success() {
            Category category = sampleCategory();
            Product product = Product.createNew(
                    ProductName.of("油性ボールペン"),
                    ProductPrice.of(120),
                    category,
                    StockQuantity.of(80));

            assertEquals(category.getCategoryId().value(), assembler.extractCategoryUuid(product));
        }

        @Test
        @DisplayName("extractCategoryUuid は Category 未設定（骨格）なら例外")
        void extractCategoryUuid_noCategory() {
            assertThrows(DomainException.class, () -> assembler.extractCategoryUuid(sampleSkeleton()));
        }
    }
}
