package plus.jdk.milvus.conditions;

import io.milvus.common.clientenum.ConsistencyLevelEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;
import plus.jdk.milvus.conditions.interfaces.Compare;
import plus.jdk.milvus.conditions.interfaces.Func;
import plus.jdk.milvus.conditions.interfaces.Join;
import plus.jdk.milvus.conditions.interfaces.Nested;
import plus.jdk.milvus.conditions.segments.ColumnSegment;
import plus.jdk.milvus.conditions.segments.MergeSegments;
import plus.jdk.milvus.enums.ExprKeyword;
import plus.jdk.milvus.enums.ExprLike;
import plus.jdk.milvus.record.VectorModel;
import plus.jdk.milvus.toolkit.CollectionUtils;
import plus.jdk.milvus.toolkit.StringUtils;
import plus.jdk.milvus.toolkit.expr.ExprUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.stream.Collectors.joining;
import static plus.jdk.milvus.enums.ExprKeyword.*;
import static plus.jdk.milvus.enums.WrapperKeyword.APPLY;
import static plus.jdk.milvus.toolkit.StringPool.*;

/**
 * 查询条件封装
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractWrapper<T extends VectorModel<? extends VectorModel<?>>, R, C extends AbstractWrapper<T, R, C>> extends Wrapper<T>
        implements Compare<C, R>, Nested<C, C>, Join<C>, Func<C, R> {

    /**
     * 占位符
     */
    protected final C typedThis = (C) this;
    /**
     * 必要度量
     */
    protected AtomicInteger paramNameSeq;
    protected MergeSegments expression;
    /**
     * 数据库表映射实体类
     */
    private T entity;
    /**
     * 实体类型(主要用于确定泛型以及取CollectionInfo缓存)
     */
    private Class<T> entityClass;

    /**
     * 查询中使用的一致性等级
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    private ConsistencyLevelEnum consistencyLevel = ConsistencyLevelEnum.STRONG;

    @Getter
    @Setter
    @Accessors(chain = true)
    private List<String> partitionNames = new ArrayList<>();

    @Override
    public T getEntity() {
        return entity;
    }

    public C setEntity(T entity) {
        this.entity = entity;
        return typedThis;
    }

    public Class<T> getEntityClass() {
        if (entityClass == null && entity != null) {
            entityClass = (Class<T>) entity.getClass();
        }
        return entityClass;
    }

    public C setEntityClass(Class<T> entityClass) {
        if (entityClass != null) {
            this.entityClass = entityClass;
        }
        return typedThis;
    }

    @Override
    public C eq(boolean condition, R column, Object val) {
        return addCondition(condition, column, EQ, val);
    }

    @Override
    public C ne(boolean condition, R column, Object val) {
        return addCondition(condition, column, NE, val);
    }

    @Override
    public C gt(boolean condition, R column, Object val) {
        return addCondition(condition, column, GT, val);
    }

    @Override
    public C ge(boolean condition, R column, Object val) {
        return addCondition(condition, column, GE, val);
    }

    @Override
    public C lt(boolean condition, R column, Object val) {
        return addCondition(condition, column, LT, val);
    }

    @Override
    public C le(boolean condition, R column, Object val) {
        return addCondition(condition, column, LE, val);
    }

    @Override
    public C likeRight(boolean condition, R column, Object val) {
        return likeValue(condition, LIKE, column, val, ExprLike.RIGHT);
    }

    @Override
    public C notLikeRight(boolean condition, R column, Object val) {
        return likeValue(condition, NOT_LIKE, column, val, ExprLike.RIGHT);
    }

    @Override
    public C and(boolean condition, Consumer<C> consumer) {
        return and(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public C or(boolean condition, Consumer<C> consumer) {
        return or(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public C not(boolean condition, Consumer<C> consumer) {
        return not(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public C or(boolean condition) {
        return maybeDo(condition, () -> appendExprSegments(OR));
    }

    @Override
    public C apply(boolean condition, String applyExpr, Object... values) {
        return maybeDo(condition, () -> appendExprSegments(APPLY, () -> formatExprMaybeWithParam(applyExpr, values)));
    }

    @Override
    public C in(boolean condition, R column, Collection<?> coll) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), IN, inExpression(coll)));
    }

    @Override
    public C in(boolean condition, R column, Object... values) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), IN, inExpression(values)));
    }

    @Override
    public C notIn(boolean condition, R column, Collection<?> coll) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), NOT_IN, inExpression(coll)));
    }

    @Override
    public C notIn(boolean condition, R column, Object... values) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), NOT_IN, inExpression(values)));
    }

    @Override
    public C func(boolean condition, Consumer<C> consumer) {
        return maybeDo(condition, () -> consumer.accept(typedThis));
    }

    @Override
    public C jsonContains(boolean condition, R column, Object value, Object... identifier) {
        return maybeDo(condition, () -> appendExprSegments(JSON, jsonExpression(columnToString(column), value, identifier)));
    }

    @Override
    public C jsonContainsAll(boolean condition, R column, Collection<?> coll, Object... identifier) {
        return maybeDo(condition, () -> appendExprSegments(JSON_ALL, jsonExpression(columnToString(column), coll, identifier)));
    }

    @Override
    public C jsonContainsAny(boolean condition, R column, Collection<?> coll, Object... identifier) {
        return maybeDo(condition, () -> appendExprSegments(JSON_ANY, jsonExpression(columnToString(column), coll, identifier)));
    }

    @Override
    public C arrayContains(boolean condition, R column, Object value) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY, arrayExpression(columnToString(column), value)));
    }

    @Override
    public C arrayContainsAll(boolean condition, R column, Collection<?> coll) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY_ALL, arrayExpression(columnToString(column), coll)));
    }

    @Override
    public C arrayContainsAll(boolean condition, R column, Object... values) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY_ALL, arrayExpression(columnToString(column), values)));
    }

    @Override
    public C arrayContainsAny(boolean condition, R column, Collection<?> coll) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY_ANY, arrayExpression(columnToString(column), coll)));
    }

    @Override
    public C arrayContainsAny(boolean condition, R column, Object... values) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY_ANY, arrayExpression(columnToString(column), values)));
    }

    @Override
    public C arrayLength(boolean condition, R column, Number value) {
        return maybeDo(condition, () -> appendExprSegments(ARRAY_LENGTH, columnToExprSegment(column), value::toString));
    }

    /**
     * 内部自用
     * <p>NOT 关键词</p>
     *
     * @param condition 条件
     * @return wrapper
     */
    protected C not(boolean condition) {
        return maybeDo(condition, () -> appendExprSegments(NOT));
    }

    /**
     * 内部自用
     * <p>拼接 AND</p>
     *
     * @param condition 条件
     * @return wrapper
     */
    protected C and(boolean condition) {
        return maybeDo(condition, () -> appendExprSegments(ExprKeyword.AND));
    }

    /**
     * 内部自用
     * <p>拼接 LIKE 以及 值</p>
     *
     * @param condition 条件
     * @param column    属性
     * @param keyword   关键字
     * @param exprLike  Expr 关键词
     * @param val       条件值
     * @return wrapper
     */
    protected C likeValue(boolean condition, ExprKeyword keyword, R column, Object val, ExprLike exprLike) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), keyword,
                () -> ExprUtils.concatLike(val, exprLike)));
    }

    /**
     * 普通查询条件
     *
     * @param condition   是否执行
     * @param column      属性
     * @param exprKeyword Expr 关键词
     * @param val         条件值
     * @return wrapper
     */
    protected C addCondition(boolean condition, R column, ExprKeyword exprKeyword, Object val) {
        return maybeDo(condition, () -> appendExprSegments(columnToExprSegment(column), exprKeyword,
                () -> formatParam(val)));
    }

    /**
     * 多重嵌套查询条件
     *
     * @param condition 查询条件值
     * @param consumer  消费者
     * @return wrapper
     */
    protected C addNestedCondition(boolean condition, Consumer<C> consumer) {
        return maybeDo(condition, () -> {
            final C instance = instance();
            consumer.accept(instance);
            appendExprSegments(APPLY, instance);
        });
    }

    /**
     * 子类返回一个自己的新对象
     *
     * @return wrapper
     */
    protected abstract C instance();

    /**
     * 格式化 Expr
     * <p>
     * 支持 "{0}" 这种,或者 "Expr {0} Expr" 这种
     *
     * @param exprStr 可能是Expr片段
     * @param params  参数
     * @return Expr片段
     */
    @SuppressWarnings("SameParameterValue")
    protected final String formatExprMaybeWithParam(String exprStr, Object... params) {
        if (StringUtils.isBlank(exprStr)) {
            return null;
        }
        if (ArrayUtils.isNotEmpty(params)) {
            for (int i = 0; i < params.length; ++i) {
                String target = LEFT_BRACE + i + RIGHT_BRACE;
                if (exprStr.contains(target)) {
                    exprStr = exprStr.replace(target, (String) params[i]);
                } else {
                    break;
                }
            }
        }
        return exprStr;
    }

    /**
     * 处理入参
     *
     * @param param 参数
     * @return value
     */
    protected final String formatParam(Object param) {
        if (param instanceof String) {
            return SINGLE_QUOTE + param + SINGLE_QUOTE;
        }
        if (param instanceof Number) {
            return param.toString();
        }
        throw new IllegalArgumentException("参数只能是 String 或者 Number 类型");
    }

    /**
     * 函数化的做事
     *
     * @param condition 做不做
     * @param something 做什么
     * @return Children
     */
    protected final C maybeDo(boolean condition, DoSomething something) {
        if (condition) {
            something.doIt();
        }
        return typedThis;
    }

    /**
     * 获取in表达式 包含括号
     *
     * @param value 集合
     * @return in表达式
     */
    protected IExprSegment inExpression(Collection<?> value) {
        if (CollectionUtils.isEmpty(value)) {
            return () -> "[]";
        }
        return () -> value.stream().map(this::formatParam)
                .collect(joining(COMMA, LEFT_SQ_BRACKET, RIGHT_SQ_BRACKET));
    }

    /**
     * 获取in表达式 包含括号
     *
     * @param values 数组
     * @return in表达式
     */
    protected IExprSegment inExpression(Object[] values) {
        return this.inExpression(Arrays.asList(values));
    }

    /**
     * 获取json表达式 包含括号
     *
     * @param column     列
     * @param value      集合
     * @param identifier json key
     * @return in表达式
     */
    protected IExprSegment jsonExpression(String column, Collection<?> value, Object... identifier) {
        if (CollectionUtils.isEmpty(value)) {
            return () -> "()";
        }
        String jsonKey = getExpressionValue(identifier);

        String valueStr = value.stream().map(this::formatParam).collect(joining(COMMA, LEFT_SQ_BRACKET, RIGHT_SQ_BRACKET));

        return () -> LEFT_BRACKET + column + jsonKey + COMMA + SPACE + valueStr + RIGHT_BRACKET;
    }

    /**
     * 获取json表达式 包含括号
     *
     * @param column     列
     * @param value      集合
     * @param identifier json key
     * @return in表达式
     */
    protected IExprSegment jsonExpression(String column, Object value, Object... identifier) {
        if (ObjectUtils.isEmpty(value)) {
            return () -> "()";
        }
        String jsonKey = getExpressionValue(identifier);
        String valueStr = formatParam(value);

        return () -> LEFT_BRACKET + column + jsonKey + COMMA + SPACE + valueStr + RIGHT_BRACKET;
    }

    /**
     * 获取array表达式 包含括号
     *
     * @param column 列
     * @param value  值
     * @return in表达式
     */
    protected IExprSegment arrayExpression(String column, Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return () -> "()";
        }
        String valueStr = formatParam(value);
        return () -> LEFT_BRACKET + column + LEFT_SQ_BRACKET + valueStr + RIGHT_SQ_BRACKET + RIGHT_BRACKET;
    }

    /**
     * 获取array表达式 包含括号
     *
     * @param column 列
     * @param values 数组
     * @return in表达式
     */
    protected IExprSegment arrayExpression(String column, Object... values) {
        if (ArrayUtils.isEmpty(values)) {
            return () -> "()";
        }
        return () -> LEFT_BRACKET + column + Arrays.stream(values).map(this::formatParam).collect(joining(COMMA, LEFT_SQ_BRACKET, RIGHT_SQ_BRACKET)) + RIGHT_BRACKET;
    }

    private String getExpressionValue(Object[] values) {
        return Arrays.stream(values).map(param -> LEFT_SQ_BRACKET + this.formatParam(param) + RIGHT_SQ_BRACKET).collect(joining());
    }

    /**
     * 必要的初始化
     */
    protected void initNeed() {
        paramNameSeq = new AtomicInteger(0);
        expression = new MergeSegments();
    }

    @Override
    public void clear() {
        entity = null;
        paramNameSeq.set(0);
        expression.clear();
    }

    /**
     * 添加 where 片段
     *
     * @param exprSegments IExprSegment 数组
     */
    protected void appendExprSegments(IExprSegment... exprSegments) {
        expression.add(exprSegments);
    }

    @Override
    public String getExprSegment() {
        return expression.getExprSegment();
    }

    @Override
    public MergeSegments getExpression() {
        return expression;
    }


    /**
     * 获取 columnName
     *
     * @param column 列
     * @return 表达式
     */
    protected final ColumnSegment columnToExprSegment(R column) {
        return () -> columnToString(column);
    }

    /**
     * 获取 columnName
     *
     * @param column 列
     * @return 列名
     */
    protected String columnToString(R column) {
        return (String) column;
    }

    /**
     * 获取 columnNames
     *
     * @param columns 多列
     * @return 列名
     */
    protected String columnsToString(R... columns) {
        return Arrays.stream(columns).map(this::columnToString).collect(joining(COMMA));
    }

    /**
     * 多字段转换为逗号 "," 分割字符串
     *
     * @param columns 多字段
     * @return 列名
     */
    protected String columnsToString(List<R> columns) {
        return columns.stream().map(this::columnToString).collect(joining(COMMA));
    }

    @Override
    @SuppressWarnings("all")
    public C clone() {
        return SerializationUtils.clone(typedThis);
    }

    /**
     * 做事函数
     */
    @FunctionalInterface
    public interface DoSomething {

        void doIt();
    }
}
