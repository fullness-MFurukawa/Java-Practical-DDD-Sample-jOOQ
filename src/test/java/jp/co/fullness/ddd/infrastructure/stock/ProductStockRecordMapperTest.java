package jp.co.fullness.ddd.infrastructure.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockId;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductStockRecord;

/**
 * {@link ProductStockRecordMapper} の単体テスト（DB 不要）。
 *
 * <p>Stock は永続化が必要（在庫の登録）なため、Mapper は双方向
 * （{@code toDomain} / {@code fromDomain}）で検証する。
 * MapStruct が生成する実装クラス {@code StockRecordMapperImpl} を直接 new して確認する。</p>
 */
@DisplayName("StockRecordMapper: jOOQ Record ⇔ Stock の相互変換")
class ProductStockRecordMapperTest {

    private final ProductStockRecordMapper mapper = new ProductStockRecordMapperImpl();

    /** canonical な UUID 文字列（テスト用の固定値） */
    private static final String UUID_STR = "22222222-2222-2222-2222-222222222222";

    private ProductStockRecord record(String stockUuid, Integer stock) {
        ProductStockRecord r = new ProductStockRecord();
        r.setStockUuid(stockUuid);
        r.setStock(stock);
        return r;
    }

    @Nested
    @DisplayName("toDomain: Record → Stock")
    class ToDomain {

        @Test
        @DisplayName("有効な Record を Stock に変換できる")
        void valid() {
            Stock stock = mapper.toDomain(record(UUID_STR, 50));

            assertEquals(UUID_STR, stock.getStockId().value());
            assertEquals(50, stock.getQuantity().value().intValue());
        }

        @Test
        @DisplayName("Record が null なら例外")
        void nullRecord() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("stock_uuid が空白なら例外")
        void blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record("  ", 50)));
        }

        @Test
        @DisplayName("在庫数が null なら例外")
        void nullQuantity() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record(UUID_STR, null)));
        }

        @Test
        @DisplayName("在庫数が範囲外（100 超）なら例外（VO のバリデーション）")
        void outOfRangeQuantity() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record(UUID_STR, 101)));
        }
    }

    @Nested
    @DisplayName("fromDomain: Stock → Record")
    class FromDomain {

        @Test
        @DisplayName("Stock を Record に変換できる（product_id は未設定）")
        void valid() {
            Stock stock = Stock.restore(StockId.fromString(UUID_STR), StockQuantity.of(30));

            ProductStockRecord rec = mapper.fromDomain(stock);

            // stock_uuid は VARCHAR(36) → value() の文字列がそのまま入る
            assertEquals(UUID_STR, rec.getStockUuid());
            assertEquals(30, rec.getStock().intValue());
            // 外部キー product_id は Mapper では設定しない（Repository が補完する）
            org.junit.jupiter.api.Assertions.assertNull(rec.getProductId());
        }

        @Test
        @DisplayName("Stock が null なら例外")
        void nullDomain() {
            assertThrows(DomainException.class, () -> mapper.fromDomain(null));
        }
    }
}

