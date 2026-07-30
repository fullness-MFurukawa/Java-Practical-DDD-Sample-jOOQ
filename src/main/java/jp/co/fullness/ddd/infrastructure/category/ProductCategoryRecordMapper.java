package jp.co.fullness.ddd.infrastructure.category;

import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.ToDomainMapper;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryId;
import jp.co.fullness.ddd.domain.model.category.CategoryName;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;

/**
 * jOOQが生成した {@link ProductCategoryRecord} から、
 * ドメインエンティティ {@link Category} を再構築(マッピング)するためのMapper。
 *
 * <p>DDDにおける「腐敗防止層(Anti-Corruption Layer)」として機能し、
 * jOOQの永続化レコードを、ドメインモデルが理解できる型
 * ({@link CategoryId}, {@link CategoryName} を持つ {@code Category})に変換します。
 * <br>これにより、上位層はDB構造やjOOQの内部実装を直接意識する必要がなくなります。
 *
 * <p><b>設計意図：</b><br>
 * - 永続化技術(jOOQ)とドメインモデルの疎結合を保つ。<br>
 * - ドメインの語彙(CategoryId, CategoryName)で正しい状態のエンティティだけを生成する。<br>
 * - データ不整合や欠損があれば {@link DomainException} を送出し、早期に検知する。
 *
 * <p><b>利用例：</b>
 * <pre>{@code
 * ProductCategoryRecord record = dsl
 *     .selectFrom(Tables.PRODUCT_CATEGORY)
 *     .where(Tables.PRODUCT_CATEGORY.ID.eq(1))
 *     .fetchOne();
 *
 * Category category = mapper.toDomain(record);
 * }</pre>
 *
 * <p>MapStruct により Spring 管理下のBeanとして利用するため、
 * {@code @Mapper(componentModel = "spring")} を指定しています。
 *
 * @see ToDomainMapper
 * @see Category
 * @see DomainException
 */
@Mapper(componentModel = "spring")
public interface ProductCategoryRecordMapper extends ToDomainMapper<ProductCategoryRecord, Category> {

    /**
     * jOOQの {@link ProductCategoryRecord} を {@link Category} エンティティに変換する。
     *
     * <p>以下を検証し、ドメインルールを満たさない場合は {@link DomainException} をスローする。
     * <ul>
     *   <li>レコードが null でないこと</li>
     *   <li>カテゴリUUIDが null でないこと</li>
     *   <li>カテゴリ名が null または空でないこと</li>
     * </ul>
     *
     * @param input jOOQ により取得された {@link ProductCategoryRecord}
     * @return 検証済みの {@link Category} エンティティ
     * @throws DomainException カラム値が null または不正形式の場合
     */
    @Override
    default Category toDomain(ProductCategoryRecord input) {
        if (input == null) {
            throw new DomainException("カテゴリ情報が取得できません。");
        }
        String categoryUuid = input.getCategoryUuid();
        String name = input.getName();
        if (categoryUuid == null) {
            throw new DomainException("カテゴリUUIDが不正です。");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("カテゴリ名が未設定です。");
        }
        // ProductCategoryRecord から Category を再構築する
        return Category.restore(
            CategoryId.fromString(categoryUuid),
            CategoryName.of(name));
    }
}
