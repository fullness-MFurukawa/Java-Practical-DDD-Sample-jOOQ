package jp.co.fullness.ddd.infrastructure.category;

import static jp.co.fullness.ddd.infrastructure.jooq.generated.Tables.PRODUCT_CATEGORY;

import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.ToDomainMapper;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryId;
import jp.co.fullness.ddd.domain.model.category.CategoryRepository;
import jp.co.fullness.ddd.infrastructure.exception.InternalException;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link CategoryRepository} の実装（jOOQ）。
 *
 * <p>カテゴリの取得（findById / findAll）を担う。Record → Category の変換は
 * {@link ToDomainMapper}（CategoryRecordMapper）に委譲する。読み取りのみのため
 * 合成用の Assembler は不要。</p>
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    /** jOOQ のクエリ実行を担う DSLContext */
    private final DSLContext dsl;

    /** Record → Category の変換を担う Mapper */
    private final ToDomainMapper<ProductCategoryRecord, Category> mapper;

    /**
     * 指定された商品カテゴリIdのカテゴリを取得する。
     *
     * @param categoryId 商品カテゴリId（VO）
     * @return 存在する場合は Category を保持する Optional、存在しない場合は空の Optional
     */
    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        if (categoryId == null) {
            throw new DomainException("商品カテゴリIdは必須です。");
        }
        try {
            // category_uuid は VARCHAR(36) なので String のまま比較する
            return dsl.selectFrom(PRODUCT_CATEGORY)
                    .where(PRODUCT_CATEGORY.CATEGORY_UUID.eq(categoryId.value()))
                    .fetchOptional()
                    .map(mapper::toDomain);
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("カテゴリ情報の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("カテゴリ情報の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }

    /**
     * すべての商品カテゴリを取得する。
     *
     * @return すべての商品カテゴリのリスト
     */
    @Override
    public List<Category> findAll() {
        try {
            return dsl.selectFrom(PRODUCT_CATEGORY)
                    .orderBy(PRODUCT_CATEGORY.ID.asc())
                    .fetch(mapper::toDomain);
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("カテゴリ一覧の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("カテゴリ一覧の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }
}