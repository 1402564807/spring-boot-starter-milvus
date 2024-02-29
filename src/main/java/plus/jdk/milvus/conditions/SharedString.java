package plus.jdk.milvus.conditions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import plus.jdk.milvus.toolkit.StringPool;

import java.io.Serializable;

/**
 * 共享查询字段
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class SharedString implements Serializable {
    private static final long serialVersionUID = -1536422416594422874L;

    /**
     * 共享的 string 值
     */
    private String stringValue;

    /**
     * SharedString 里是 ""
     *
     * @return SharedString 里是 ""
     */
    public static SharedString emptyString() {
        return new SharedString(StringPool.EMPTY);
    }

    /**
     * 置 empty
     */
    public void toEmpty() {
        stringValue = StringPool.EMPTY;
    }

    /**
     * 置 null
     */
    public void toNull() {
        stringValue = null;
    }
}
