package jp.co.fullness.ddd.infrastructure.stock;

import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.DomainBiMapper;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockId;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductStockRecord;

/**
 * jOOQ の {@link ProductStockRecord} とエンティティ {@link Stock} を相互変換する Mapper。
 *
 * <p>DDD の腐敗防止層（Anti-Corruption Layer）として機能し、
 * 永続化構造（{@link ProductStockRecord}）とドメイン構造（{@link Stock}）の依存を絶つ。</p>
 */
@Mapper(componentModel = "spring") // Spring 管理 Bean として実装を生成する
public interface StockRecordMapper extends DomainBiMapper<ProductStockRecord, Stock> {

    /**
     * jOOQ の {@link ProductStockRecord} からドメインエンティティ {@link Stock} を再構築する。
     *
     * @param input ProductStockRecord
     * @return 再構築された Stock エンティティ
     * @throws DomainException 必須項目が null または不正な場合
     */
    @Override
    default Stock toDomain(ProductStockRecord input) {
        if (input == null) {
            throw new DomainException("在庫情報が取得できません。");
        }

        // stock_uuid は VARCHAR(36) なので getStockUuid() は String を返す
        String stockUuid = input.getStockUuid();
        Integer quantity = input.getStock();

        if (stockUuid == null || stockUuid.isBlank()) {
            throw new DomainException("在庫UUIDが不正です。");
        }
        if (quantity == null) {
            throw new DomainException("在庫数が未設定です。");
        }

        // VO のファクトリを通すことで、復元時にも不変条件を再検証する
        return Stock.restore(
                StockId.fromString(stockUuid),
                StockQuantity.of(quantity));
    }

    /**
     * ドメインエンティティ {@link Stock} を jOOQ の {@link ProductStockRecord} に変換する。
     *
     * <p>INSERT/UPDATE 用。DB 側の主キー（id）や外部キー（product_id）は
     * この Mapper では設定しないため、呼び出し元（Repository）で補完すること。</p>
     *
     * @param domain エンティティ Stock
     * @return jOOQ の ProductStockRecord
     * @throws DomainException Stock が null の場合
     */
    @Override
    default ProductStockRecord fromDomain(Stock domain) {
        if (domain == null) {
            throw new DomainException("Stock エンティティが null です。");
        }

        ProductStockRecord rec = new ProductStockRecord();
        // stock_uuid は VARCHAR(36)。value() が canonical な文字列を返すのでそのまま渡す
        rec.setStockUuid(domain.getStockId().value());
        rec.setStock(domain.getQuantity().value());
        return rec;
    }
}
