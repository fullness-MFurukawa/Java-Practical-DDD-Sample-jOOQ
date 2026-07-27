package jp.co.fullness.ddd.infrastructure.product;

import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.DomainBiMapper;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductRecord;

/**
 * jOOQ が生成した {@link ProductRecord} と
 * ドメインエンティティ {@link Product} を相互変換する Mapper インターフェイス。
 *
 * <p>DDD における「腐敗防止層（Anti-Corruption Layer）」として機能し、
 * 永続化層（jOOQ の Record 構造）とドメイン層（不変なエンティティ構造）を分離する。</p>
 *
 * <p><b>責務：</b></p>
 * <ul>
 *   <li>Product テーブル行（{@code ProductRecord}）を、カテゴリ・在庫を伴わない
 *       骨格の {@link Product} に再構築する（toDomain）。カテゴリ・在庫は別 Mapper で
 *       変換し、リポジトリ／Assembler で合成する。</li>
 *   <li>ドメインの {@link Product} を保存用 {@link ProductRecord} に変換する（fromDomain）。
 *       ただし外部キー {@code category_id} は設定しない（下記参照）。</li>
 * </ul>
 *
 * <p>MapStruct により実装クラス（{@code ProductRecordMapperImpl}）が生成され、
 * Spring 管理下の Bean として利用できる。</p>
 *
 * @see Product
 * @see ProductRecord
 * @see jp.co.fullness.ddd.domain.mapper.DomainBiMapper
 */
@Mapper(componentModel = "spring") // Spring 管理 Bean として実装を生成する
public interface ProductRecordMapper extends DomainBiMapper<ProductRecord, Product> {

    /**
     * jOOQ の {@link ProductRecord} からエンティティ {@link Product} を再構築する。
     *
     * <p>本メソッドは商品テーブル単体の行のみを扱い、カテゴリ・在庫は含めない
     * 「骨格」の {@link Product} を返す（{@code restoreSkeleton}）。</p>
     *
     * @param input jOOQ により取得された {@link ProductRecord}
     * @return カテゴリ・在庫を伴わない骨格の {@link Product}
     * @throws DomainException 必須項目が null または不正形式の場合
     */
    @Override
    default Product toDomain(ProductRecord input) {
        if (input == null) {
            throw new DomainException("商品情報が取得できません。");
        }

        // product_uuid は VARCHAR(36) なので getProductUuid() は String を返す
        String productUuid = input.getProductUuid();
        String name = input.getName();
        Integer price = input.getPrice();

        if (productUuid == null || productUuid.isBlank()) {
            throw new DomainException("商品UUIDが不正です。");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("商品名が未設定です。");
        }
        if (price == null) {
            throw new DomainException("商品価格が未設定です。");
        }

        // カテゴリ・在庫は別 Mapper で変換し、後段（Assembler）で合成する
        return Product.restoreSkeleton(
                ProductId.fromString(productUuid),
                ProductName.of(name),
                ProductPrice.of(price));
    }

    /**
     * エンティティ {@link Product} を jOOQ の {@link ProductRecord} に変換する。
     *
     * <p><b>注意：</b>本メソッドは {@code product_uuid} / {@code name} / {@code price} のみを
     * 設定する。外部キー {@code category_id} は NOT NULL だが、ここでは設定しない。
     * 呼び出し元（Repository）で {@code Product} が保持する {@code Category} から
     * {@code category_id} を解決して補完すること。補完せずに INSERT すると NOT NULL 制約に違反する。</p>
     *
     * @param domain ドメインエンティティ {@link Product}
     * @return 永続化用 {@link ProductRecord}（category_id は未設定）
     * @throws DomainException 引数が null の場合
     */
    @Override
    default ProductRecord fromDomain(Product domain) {
        if (domain == null) {
            throw new DomainException("Product エンティティが null です。");
        }

        ProductRecord rec = new ProductRecord();
        // product_uuid は VARCHAR(36)。value() が canonical な文字列を返すのでそのまま渡す
        rec.setProductUuid(domain.getProductId().value());
        rec.setName(domain.getName().value());
        rec.setPrice(domain.getPrice().value());
        // category_id はここでは設定しない（Repository で補完）
        return rec;
    }
}