package plus.jdk.milvus.annotation;

import io.milvus.grpc.DataType;
import plus.jdk.milvus.global.DefaultEmbeddingTypeHandler;
import plus.jdk.milvus.global.EmbeddingTypeHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorTableColumn {

    /**
     * 字段名
     */
    String name();

    /**
     * 数据类型
     */
    DataType dataType();

    /**
     * 数据向量化处理的handler
     */
    Class<? extends EmbeddingTypeHandler<?, ?>> EmbeddingTypeHandler() default DefaultEmbeddingTypeHandler.class;

    /**
     * 字段描述
     */
    String desc() default "";
}
