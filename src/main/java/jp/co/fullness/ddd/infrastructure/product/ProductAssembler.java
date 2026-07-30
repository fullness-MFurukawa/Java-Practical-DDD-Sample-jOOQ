package jp.co.fullness.ddd.infrastructure.product;

import org.springframework.stereotype.Component;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.infrastructure.stock.ProductStockRecordMapper;
import jp.co.fullness.ddd.infrastructure.category.ProductCategoryRecordMapper;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductStockRecord;

import lombok.RequiredArgsConstructor;

/**
 * Product 集約の「合成（Record → 集約）」および「分解（集約 → Record）」を担うアセンブラ。
 *
 * <p>責務は<b>型変換と合成/分解のみ</b>。永続化（SQL 実行）は Repository が担う。</p>
 */
@Component
@RequiredArgsConstructor
public class ProductAssembler {

    /** ProductRecord ↔ Product */
    private final ProductRecordMapper productRecordMapper;
    /** ProductCategoryRecord → Category */
    private final ProductCategoryRecordMapper categoryRecordMapper;
    /** ProductStockRecord ↔ Stock */
    private final ProductStockRecordMapper stockRecordMapper;

    // Lombok を使わない場合は明示コンストラクタで代替可
    // public ProductAssembler(ProductRecordMapper productRecordMapper,
    //                         CategoryRecordMapper categoryRecordMapper,
    //                         StockRecordMapper stockRecordMapper) {
    //     this.productRecordMapper = productRecordMapper;
    //     this.categoryRecordMapper = categoryRecordMapper;
    //     this.stockRecordMapper = stockRecordMapper;
    // }

    // ----------------------------------------------------------------------
    // 合成（Record → 集約）
    // ----------------------------------------------------------------------

    /**
     * jOOQ 生成レコード3種から完全な {@link Product} を合成する。
     *
     * <p>Repository で JOIN 取得した各レコードを渡すと、Product を再構築（rehydrate）する。
     * 骨格（{@code restoreSkeleton}）にカテゴリ・在庫を {@code attach} して合成する。</p>
     *
     * @param pr Product の基本情報（product_uuid, name, price）
     * @param cr Category の基本情報（category_uuid, name）
     * @param sr Stock の基本情報（stock_uuid, stock）
     * @return 合成済みの Product 集約
     * @throws DomainException 必須項目欠落や不正値の場合
     */
    public Product assemble(ProductRecord pr, ProductCategoryRecord cr, ProductStockRecord sr) {
        if (pr == null) throw new DomainException("ProductRecord が null です。");
        if (cr == null) throw new DomainException("ProductCategoryRecord が null です。");
        if (sr == null) throw new DomainException("ProductStockRecord が null です。");

        // 骨格を復元し、カテゴリ・在庫を後から合成する
        var product = productRecordMapper.toDomain(pr);          // skeleton
        product.attachCategory(categoryRecordMapper.toDomain(cr));
        product.attachStock(stockRecordMapper.toDomain(sr));
        return product;
    }

    // ----------------------------------------------------------------------
    // 分解（集約 → Record）
    // ----------------------------------------------------------------------

    /**
     * 集約から ProductRecord を作る（INSERT/UPDATE 用）。
     *
     * <p>注意：外部キー category_id はここでは埋めない。Repository で補完する。</p>
     */
    public ProductRecord toProductRecord(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        return productRecordMapper.fromDomain(product);
    }

    /**
     * 集約から ProductStockRecord を作る（INSERT/UPDATE 用）。
     *
     * <p>注意：外部キー product_id はここでは埋めない。Repository で補完する。</p>
     */
    public ProductStockRecord toStockRecord(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        var stock = product.getStock();
        if (stock == null) throw new DomainException("Product に Stock が設定されていません。");
        return stockRecordMapper.fromDomain(stock);
    }

    /**
     * 集約から Category の UUID（文字列）を取り出すユーティリティ。
     * Repository で category_id（外部キー）を解決するために利用する。
     */
    public String extractCategoryUuid(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        var category = product.getCategory();
        if (category == null) throw new DomainException("Product に Category が設定されていません。");
        return category.getCategoryId().value();
    }
}