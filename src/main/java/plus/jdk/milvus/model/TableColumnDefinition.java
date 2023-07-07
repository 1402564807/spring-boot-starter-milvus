package plus.jdk.milvus.model;

import io.milvus.grpc.DataType;
import lombok.Data;
import plus.jdk.milvus.global.VectorTypeHandler;

import java.lang.reflect.Field;

@Data
public class TableColumnDefinition {

    /**
     * 是否是主键
     */
    private Boolean primary;

    /**
     * 字段名
     */
    private String name;

    /**
     * 数据类型
     */
    private DataType dataType;

    /**
     * 数据向量化处理的handler
     */
    private VectorTypeHandler<?, ?> vectorTypeHandler;

    /**
     * 字段描述
     */
    private String desc;


    /**
     * 字段
     */
    private Field field;
}
