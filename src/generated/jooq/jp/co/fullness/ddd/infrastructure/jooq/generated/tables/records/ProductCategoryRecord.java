package jp.co.fullness.ddd.infrastructure.jooq.generated.tables.records;


import jp.co.fullness.ddd.infrastructure.jooq.generated.tables.ProductCategory;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * カテゴリ
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ProductCategoryRecord extends UpdatableRecordImpl<ProductCategoryRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.product_category.id</code>. カテゴリId（主キー・自動生成）
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.product_category.id</code>. カテゴリId（主キー・自動生成）
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.product_category.category_uuid</code>.
     * 識別Id（コード上の識別に利用）
     */
    public void setCategoryUuid(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.product_category.category_uuid</code>.
     * 識別Id（コード上の識別に利用）
     */
    public String getCategoryUuid() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.product_category.name</code>. カテゴリ名
     */
    public void setName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.product_category.name</code>. カテゴリ名
     */
    public String getName() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ProductCategoryRecord
     */
    public ProductCategoryRecord() {
        super(ProductCategory.PRODUCT_CATEGORY);
    }

    /**
     * Create a detached, initialised ProductCategoryRecord
     */
    public ProductCategoryRecord(Integer id, String categoryUuid, String name) {
        super(ProductCategory.PRODUCT_CATEGORY);

        setId(id);
        setCategoryUuid(categoryUuid);
        setName(name);
        resetChangedOnNotNull();
    }
}
