package jp.co.fullness.ddd.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records.ProductCategoryRecord;

/**
 * {@link CategoryRecordMapper} の単体テスト（Spring コンテナ経由）。
 *
 * <p>この Mapper は {@code @Mapper(componentModel = "spring")} により Spring の Bean として
 * 生成される（実装は MapStruct が生成する {@code CategoryRecordMapperImpl}）。本テストでは
 * DI で実際に注入される Bean をそのまま検証するため、{@code @SpringBootTest} を用いる。</p>
 *
 * <p>変換ロジック自体は DB を参照しないが、{@code @SpringBootTest} はアプリケーション
 * コンテキスト全体を起動するため、実行時はローカル PostgreSQL が起動している必要がある
 * （DataSource の自動構成が行われるため）。</p>
 */
@SpringBootTest
@DisplayName("CategoryRecordMapper: jOOQ Record → Category の変換（DI 経由）")
class CategoryRecordMapperTest {

    /** Spring コンテナから注入される MapStruct 実装 Bean */
    @Autowired
    private CategoryRecordMapper mapper;

    /** テスト用に ProductCategoryRecord を組み立てるヘルパ */
    private ProductCategoryRecord record(String categoryUuid, String name) {
        ProductCategoryRecord r = new ProductCategoryRecord();
        r.setCategoryUuid(categoryUuid);
        r.setName(name);
        return r;
    }

    @Nested
    @DisplayName("正常系")
    class Success {

        @Test
        @DisplayName("有効な Record を Category に変換できる")
        void toDomain_valid() {
            // canonical（小文字・8-4-4-4-12）な UUID 文字列
            String uuid = "11111111-1111-1111-1111-111111111111";

            Category category = mapper.toDomain(record(uuid, "文房具"));

            // VO のファクトリを通して復元されていること
            assertEquals(uuid, category.getCategoryId().value());
            assertEquals("文房具", category.getName().value());
        }
    }

    @Nested
    @DisplayName("異常系（DomainException を送出する）")
    class Failure {

        @Test
        @DisplayName("Record が null なら例外")
        void toDomain_nullRecord() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("category_uuid が空文字/空白なら例外")
        void toDomain_blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record("   ", "文房具")));
        }

        @Test
        @DisplayName("name が空文字/空白なら例外")
        void toDomain_blankName() {
            String uuid = "11111111-1111-1111-1111-111111111111";
            assertThrows(DomainException.class, () -> mapper.toDomain(record(uuid, "   ")));
        }

        @Test
        @DisplayName("category_uuid が UUID 形式でないなら例外（VO のバリデーション）")
        void toDomain_invalidUuidFormat() {
            assertThrows(DomainException.class, () -> mapper.toDomain(record("not-a-uuid", "文房具")));
        }
    }
}