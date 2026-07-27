package jp.co.fullness.ddd.infrastructure.product;

import static jp.co.fullness.ddd.infrastructure.jooq.generated.Tables.PRODUCT;
import static jp.co.fullness.ddd.infrastructure.jooq.generated.Tables.PRODUCT_CATEGORY;
import static jp.co.fullness.ddd.infrastructure.jooq.generated.Tables.PRODUCT_STOCK;

import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductRepository;
import jp.co.fullness.ddd.infrastructure.exception.InternalException;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductRecord;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductStockRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link ProductRepository} の jOOQ による実装。
 *
 * <p>Product 集約（商品・カテゴリ・在庫）の永続化を担う。Record ↔ 集約 の
 * 合成・分解は {@link ProductAssembler} に委譲する。</p>
 */
@Repository
@Profile("jooq")
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    /** jOOQ のクエリ実行を担う DSLContext */
    private final DSLContext dsl;

    /** Record ↔ 集約 の合成・分解を担うアセンブラ */
    private final ProductAssembler assembler;

    // Lombok を使わない場合は下記の明示コンストラクタで代替できる
    // public ProductJooqRepository(DSLContext dsl, ProductAssembler assembler) {
    //     this.dsl = dsl;
    //     this.assembler = assembler;
    // }

    /**
     * 新しい商品を永続化する。
     *
     * @param product 永続化する商品
     */
    @Override
    public void create(Product product) {
        if (product == null) {
            throw new DomainException("商品は必須です。");
        }
        try {
            // カテゴリUUID(文字列) → カテゴリの内部PK(INT) を解決
            // category_uuid は VARCHAR(36) なので String のまま比較する
            String categoryUuid = assembler.extractCategoryUuid(product);
            Integer categoryPk = dsl
                .select(PRODUCT_CATEGORY.ID)
                .from(PRODUCT_CATEGORY)
                .where(PRODUCT_CATEGORY.CATEGORY_UUID.eq(categoryUuid))
                .fetchOneInto(Integer.class);
            if (categoryPk == null) {
                throw new DomainException("指定された商品カテゴリが存在しません。");
            }

            // 集約 → Record（外部キーは未設定）
            ProductRecord pr      = assembler.toProductRecord(product);
            ProductStockRecord sr = assembler.toStockRecord(product);

            // product に category_id を補完して INSERT（採番されたPKを受け取る）
            pr.setCategoryId(categoryPk);
            Integer productPk = dsl.insertInto(PRODUCT)
                .set(pr)
                .returning(PRODUCT.ID)
                .fetchOne()
                .getId();

            // stock に product_id を補完して INSERT
            sr.setProductId(productPk);
            dsl.insertInto(PRODUCT_STOCK)
                .set(sr)
                .execute();

        } catch (DomainException ex) {
            throw ex;   // ドメイン例外はそのまま伝播させる
        } catch (DataAccessException ex) {
            throw new InternalException("商品登録中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品登録処理中に予期しないエラーが発生しました。", ex);
        }
    }

    /**
     * 指定された商品名が存在するかを返す。
     *
     * @param productName 商品名
     * @return true:存在する / false:存在しない
     */
    @Override
    public boolean existsByName(ProductName productName) {   // Boolean → boolean
        if (productName == null) {
            throw new DomainException("商品名は必須です。");
        }
        try {
            return dsl.fetchExists(
                dsl.selectOne()
                .from(PRODUCT)
                .where(PRODUCT.NAME.eq(productName.value())));
        } catch (DataAccessException ex) {
            throw new InternalException("商品名の存在確認中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品名の存在確認処理中に予期しないエラーが発生しました。", ex);
        }
    }

    /**
     * 指定された商品Idの商品を取得する。
     *
     * @param productId 商品Id（VO）
     * @return 存在する場合は Product を保持する Optional、存在しない場合は空の Optional
     */
    @Override
    public Optional<Product> findById(ProductId productId) {
        if (productId == null) {
            throw new DomainException("商品Idは必須です。");
        }
        try {
            // product_uuid は VARCHAR(36) なので String のまま比較する
            var rec = dsl
                .select(PRODUCT.fields())
                .select(PRODUCT_STOCK.fields())
                .select(PRODUCT_CATEGORY.fields())
                .from(PRODUCT)
                .join(PRODUCT_STOCK)
                    .on(PRODUCT.ID.eq(PRODUCT_STOCK.PRODUCT_ID))
                .join(PRODUCT_CATEGORY)
                    .on(PRODUCT.CATEGORY_ID.eq(PRODUCT_CATEGORY.ID))
                .where(PRODUCT.PRODUCT_UUID.eq(productId.value()))
                .fetchOne();

            if (rec == null) {
                return Optional.empty();
            }

            // テーブルインスタンスで分解（重複カラム名を安全に振り分ける）
            ProductRecord pr         = rec.into(PRODUCT);
            ProductStockRecord sr    = rec.into(PRODUCT_STOCK);
            ProductCategoryRecord cr = rec.into(PRODUCT_CATEGORY);
            return Optional.of(assembler.assemble(pr, cr, sr));

        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("商品情報の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品情報の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }

    /**
     * 商品名で商品を取得する。
     *
     * @param productName 商品名（VO）
     * @return 存在する場合は Product を保持する Optional、存在しない場合は空の Optional
     */
    @Override
    public Optional<Product> findByName(ProductName productName) {
        if (productName == null) {
            throw new DomainException("商品名は必須です。");
        }
        try {
            var rec = dsl
                .select(PRODUCT.fields())
                .select(PRODUCT_STOCK.fields())
                .select(PRODUCT_CATEGORY.fields())
                .from(PRODUCT)
                .join(PRODUCT_STOCK)
                    .on(PRODUCT.ID.eq(PRODUCT_STOCK.PRODUCT_ID))
                .join(PRODUCT_CATEGORY)
                    .on(PRODUCT.CATEGORY_ID.eq(PRODUCT_CATEGORY.ID))
                .where(PRODUCT.NAME.eq(productName.value()))
                .fetchOne();

            if (rec == null) {
                return Optional.empty();
            }

            ProductRecord pr         = rec.into(PRODUCT);
            ProductStockRecord sr    = rec.into(PRODUCT_STOCK);
            ProductCategoryRecord cr = rec.into(PRODUCT_CATEGORY);
            return Optional.of(assembler.assemble(pr, cr, sr));

        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("商品名による検索中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品名による検索処理中に予期しないエラーが発生しました。", ex);
        }
    }
}
