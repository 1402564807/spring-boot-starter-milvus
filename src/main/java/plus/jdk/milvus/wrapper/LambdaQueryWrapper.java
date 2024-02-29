package plus.jdk.milvus.wrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import plus.jdk.milvus.conditions.AbstractLambdaWrapper;
import plus.jdk.milvus.conditions.query.Query;
import plus.jdk.milvus.conditions.segments.MergeSegments;
import plus.jdk.milvus.record.VectorModel;
import plus.jdk.milvus.toolkit.support.SFunction;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lambda 语法使用 Wrapper
 */
@Setter
@Getter
public class LambdaQueryWrapper<T extends VectorModel<? extends VectorModel<?>>> extends AbstractLambdaWrapper<T, LambdaQueryWrapper<T>>
        implements Query<LambdaQueryWrapper<T>, T, SFunction<T, ?>> {

    @Accessors(chain = true)
    private Long offset;

    @Accessors(chain = true)
    private Long limit = 10L;

    public LambdaQueryWrapper() {
        this((T) null);
    }

    public LambdaQueryWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
    }

    public LambdaQueryWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
    }

    public LambdaQueryWrapper(T entity, Class<T> entityClass, AtomicInteger paramNameSeq, MergeSegments mergeSegments,
                              Long offset, Long limit
    ) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.expression = mergeSegments;
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * 用于生成嵌套 Expr
     * <p>故 ExprSelect 不向下传递</p>
     */
    @Override
    protected LambdaQueryWrapper<T> instance() {
        return new LambdaQueryWrapper<>(getEntity(), getEntityClass(), paramNameSeq,
                new MergeSegments(), offset, limit);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
