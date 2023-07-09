
### 一、如何引入

```xml
<dependency>
    <groupId>plus.jdk</groupId>
    <artifactId>spring-boot-starter-milvus</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 二、milvus的引用配置

```bash
plus.jdk.milvus.enabled=true
plus.jdk.milvus.host=192.168.1.101
plus.jdk.milvus.port=19530
plus.jdk.milvus.user-name=root
plus.jdk.milvus.password=123456
```

### 三、定义ORM对象


```java
import io.milvus.grpc.DataType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import plus.jdk.milvus.annotation.VectorCollectionColumn;
import plus.jdk.milvus.annotation.VectorCollectionName;
import plus.jdk.milvus.record.VectorModel;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@VectorCollectionName(name = "user_blog", description = "用户博文向量表")
public class UserBlogVector extends VectorModel<UserBlogVector> {

    /**
     * 主键
     */
    @VectorCollectionColumn(name = "id", dataType = DataType.Int64, primary = true)
    private Long id;

    /**
     * uid
     */
    @VectorCollectionColumn(name = "uid", dataType = DataType.Int64)
    private Long uid;
    
    /**
     * 博文文本
     */
    @VectorCollectionColumn(name = "blog_text", dataType = DataType.VarChar, maxLength = 1024)
    private String blogText;

    /**
     * 博文文本向量， 此处的博文文本向量使用chathlm embedding, 所以是4096
     */
    @VectorCollectionColumn(name = "v_blog_text", dataType = DataType.FloatVector, vectorDimension = 4096)
    private List<Float> blogTextVector;
}
```

### 四、定义申明Dao数据层

我们在 `VectorModelRepositoryImpl` 封装了很多对 `milvus`进行基本操作的api

```java
import com.weibo.biz.omniscience.dolly.milvus.entity.UserBlogVector;
import plus.jdk.milvus.annotation.VectorRepository;
import plus.jdk.milvus.record.VectorModelRepositoryImpl;

@VectorRepository
public class UserBlogVectorDao extends VectorModelRepositoryImpl<UserBlogVector> {
}
```

**一些常用的api示例如下：**

```java
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import plus.jdk.milvus.common.MilvusException;
import plus.jdk.milvus.global.MilvusClientService;
import plus.jdk.milvus.model.HNSWIIndexExtra;
import plus.jdk.milvus.wrapper.LambdaQueryWrapper;
import plus.jdk.milvus.wrapper.LambdaSearchWrapper;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@SpringBootTest
class UserBlogVectorServiceTest {

    @Resource
    private UserBlogVectorDao userBlogVectorDao;


    /**
     * 创建集合和索引
     * @throws MilvusException
     */
    @Test
    public void createCollection() throws MilvusException {
        boolean ret = userBlogVectorDao.createCollection(UserBlogVector.class);
        HNSWIIndexExtra extra = new HNSWIIndexExtra();
        extra.setM(16);
        extra.setEfConstruction(8);
        userBlogVectorDao.createIndex(UserBlogVector.class, "idx_blog_vector",
                UserBlogVector::getBlogTextVector, extra);
        userBlogVectorDao.loadCollection(UserBlogVector.class);
    }

    /**
     * 使用主键删除记录
=     */
    @Test
    public void deleteRecord() throws MilvusException {
        boolean ret = userBlogVectorDao.remove(12345556, UserBlogVector.class);
        log.info("{}", ret);
    }

    /**
     * 使用其他字段查找相关内容
     */
    @Test
    public void query() throws MilvusException {
        LambdaQueryWrapper<UserBlogVector> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlogVector::getUid, 123456L);
        List<UserBlogVector> queryResults = userBlogVectorDao.query(wrapper, UserBlogVector.class);
        log.info("{}", queryResults);
    }

    /**
     * 使用向量查找相似度最高的内容。可以结合其他字段做条件查询过滤
     */
    @Test
    public void search() throws IOException, MilvusException {
        String text = "宝贝们！！没睡吧啊啊啊 刚出炉的九图 投票！喜欢图几";
        Long uid = 1234567L;
        LambdaSearchWrapper<UserBlogVector> wrapper = new LambdaSearchWrapper<>();
        ChatGlmEmbeddingResult result = aigcApiService.chatGlmEmbedding(text, uid);
        wrapper.vector(UserBlogVector::getBlogTextVector, result.getEmbeddings());
        wrapper.setTopK(10);
        List<UserBlogVector> searchResults = userBlogVectorDao.search(wrapper, UserBlogVector.class);
        log.info("{}", searchResults);
    }


    /**
     * 向集合插入记录
     */
    @Test
    public void insertVector() throws MilvusException {
        Long uid = 2656274875L;
        long timestamp = System.currentTimeMillis();
        Date startTime = new Date(timestamp - 3600 * 24 * 10 * 1000L); //最近3天的发博数据
        Date endTime = new Date(timestamp);
        UserBlogVector userBlogVector = new UserBlogVector();
        userBlogVector.setBlogText(text);
        userBlogVector.setUid(uid);
        ChatGlmEmbeddingResult result = chatglmApiService.chatGlmEmbedding(text, uid);
        userBlogVector.setBlogTextVector(result.getEmbeddings());
        boolean ret = userBlogVectorDao.insertVector(userBlogVector);
        log.info("{}", ret);
    }
}
```
