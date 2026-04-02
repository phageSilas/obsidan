# 基本语法
注入RedisTemplate后即可调用相关redis方法
在 Java Spring（尤其是 Spring Boot）体系中，操作 Redis 最核心的工具是 Spring Data Redis 提供的 **`RedisTemplate`** 和 **`StringRedisTemplate`**。

Spring 会将 Redis 的各种数据结构操作封装成了不同的 `Operations` 接口。以下是日常开发中最常用的方法分类与示例：

---

### 一、 通用 Key 操作（针对所有数据类型）

无论底层是哪种数据结构，这些方法都用于直接操作 Redis 的键（Key）。

- **检查 Key 是否存在：**
    
    Java
    
    ```
    redisTemplate.hasKey("myKey"); // 返回 true 或 false
    ```
    
- **删除 Key：**
    
    Java
    
    ```
    redisTemplate.delete("myKey"); // 删除单个
    redisTemplate.delete(Arrays.asList("key1", "key2")); // 批量删除
    ```
    
- **设置过期时间：**
    
    Java
    
    ```
    // 设置 60 秒后过期
    redisTemplate.expire("myKey", 60, TimeUnit.SECONDS);
    ```
    
- **获取剩余过期时间：**
    
    Java
    
    ```
    redisTemplate.getExpire("myKey"); // 返回剩余秒数
    redisTemplate.getExpire("myKey", TimeUnit.MINUTES); // 返回剩余分钟数
    ```
    

---

### 二、 String 字符串操作 (`opsForValue`)

最基础的键值对存储，通常用于缓存 JSON 字符串、计数器等。

- **基础设值与获取：**
    
    Java
    
    ```
    // 设值
    redisTemplate.opsForValue().set("name", "Gemini");
    // 设值并直接附带过期时间（推荐，保证原子性）
    redisTemplate.opsForValue().set("token", "xyz123", 30, TimeUnit.MINUTES);
    // 获取值
    String name = (String) redisTemplate.opsForValue().get("name");
    ```
    
- **如果不存在则设置 (SETNX - 常用于分布式锁简单实现)：**
    
    Java
    
    ```
    Boolean isSuccess = redisTemplate.opsForValue().setIfAbsent("lock", "value", 10, TimeUnit.SECONDS);
    ```
    
- **递增/递减操作（计数器）：**
    
    Java
    
    ```
    redisTemplate.opsForValue().increment("viewCount", 1); // 增加 1
    redisTemplate.opsForValue().decrement("viewCount", 1); // 减少 1
    ```
    

---

### 三、 Hash 哈希操作 (`opsForHash`)

适合存储对象（例如用户信息），将对象的各个字段独立存储，方便局部修改。

- **添加/修改 Hash 字段：**
    
    Java
    
    ```
    redisTemplate.opsForHash().put("user:1001", "name", "John");
    redisTemplate.opsForHash().put("user:1001", "age", "25");
    ```
    
- **批量添加（存入一个 Map）：**
    
    Java
    
    ```
    Map<String, Object> map = new HashMap<>();
    map.put("name", "Jane");
    map.put("city", "Tokyo");
    redisTemplate.opsForHash().putAll("user:1002", map);
    ```
    
- **获取特定字段 / 获取整个对象：**
    
    Java
    
    ```
    // 获取单个字段
    String name = (String) redisTemplate.opsForHash().get("user:1001", "name");
    // 获取该 Hash 下的所有键值对
    Map<Object, Object> userMap = redisTemplate.opsForHash().entries("user:1001");
    ```
    
- **删除特定字段：**
    
    Java
    
    ```
    redisTemplate.opsForHash().delete("user:1001", "age");
    ```
    

---

### 四、 List 列表操作 (`opsForList`)

一个简单的字符串列表，按插入顺序排序。常用于消息队列、最新动态列表等。

- **头尾压入元素：**
    
    Java
    
    ```
    redisTemplate.opsForList().leftPush("recent:logs", "log1"); // 从左侧（头部）插入
    redisTemplate.opsForList().rightPush("recent:logs", "log2"); // 从右侧（尾部）插入
    ```
    
- **获取/弹出元素：**
    
    Java
    
    ```
    // 弹出并移除最左侧的元素
    String log = (String) redisTemplate.opsForList().leftPop("recent:logs");
    ```
    
- **获取列表片段（分页）：**
    
    Java
    
    ```
    // 获取列表前 10 个元素（0 是开始索引，9 是结束索引）
    List<Object> logs = redisTemplate.opsForList().range("recent:logs", 0, 9);
    ```
    
- **获取列表长度：**
    
    Java
    
    ```
    Long size = redisTemplate.opsForList().size("recent:logs");
    ```
    

---

### 五、 Set 集合操作 (`opsForSet`)

无序集合，元素不能重复。常用于标签系统、共同好友（交集/并集操作）。

- **添加与查询成员：**
    
    Java
    
    ```
    redisTemplate.opsForSet().add("tags:article:1", "java", "spring", "redis"); // 添加
    Set<Object> tags = redisTemplate.opsForSet().members("tags:article:1"); // 获取所有元素
    Boolean hasJava = redisTemplate.opsForSet().isMember("tags:article:1", "java"); // 判断是否存在
    ```
    
- **集合运算（交集、并集）：**
    
    Java
    
    ```
    // 获取两个 Set 的交集（例如共同好友）
    Set<Object> common = redisTemplate.opsForSet().intersect("user:1:friends", "user:2:friends");
    ```
    
- **删除元素：**
    
    Java
    
    ```
    redisTemplate.opsForSet().remove("tags:article:1", "spring");
    ```
    

---

### 六、 ZSet 有序集合操作 (`opsForZSet`)

在 Set 的基础上，为每个元素关联一个 `score`（分数），按分数自动排序。非常适合排行榜等业务。

- **添加元素与分数：**
    
    Java
    
    ```
    // 为玩家添加积分
    redisTemplate.opsForZSet().add("leaderboard", "PlayerA", 1500.5);
    redisTemplate.opsForZSet().add("leaderboard", "PlayerB", 2000.0);
    ```
    
- **增加分数：**
    
    Java
    
    ```
    // PlayerA 增加 100 分
    redisTemplate.opsForZSet().incrementScore("leaderboard", "PlayerA", 100);
    ```
    
- **获取排行榜单：**
    
    Java
    
    ```
    // 获取积分最高的 Top 10 玩家（倒序排，从大到小）
    Set<Object> top10 = redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);
    
    // 获取积分在 1000 到 2000 之间的玩家
    Set<Object> range = redisTemplate.opsForZSet().rangeByScore("leaderboard", 1000, 2000);
    ```
    


如果在项目中你的 Key 和 Value 都是字符串（这在绝大多数场景下是最常见的），建议直接注入并使用 **`StringRedisTemplate`**。它的内部序列化器已经默认配置为 `StringRedisSerializer`，可以避免存入 Redis 时出现类似 `\xac\xed\x00\x05t\x00\x05` 的乱码前缀问题。

# 序列化和反序列化

### 一、 什么是 JSON 序列化？（存数据）

**序列化（Serialization）** 就是你把乐高城堡**拆解**开，并且在纸上**写下详细的拼装说明书**（将其变成 JSON 字符串），然后再把这份说明书传给朋友（存入 Redis）。

- **在 Java/Redis 中的过程：** 你的程序里有一个活生生的 Java 对象：`User(name="张三", age=25)`。 经过 JSON 序列化器处理后，它变成了一段标准的纯文本字符串：`{"name":"张三", "age":25}`。 最后，Redis 把这段纯文本存储起来。
    
- **干什么用的？** Redis 是用 C 语言写的，它根本不认识什么是 Java 的 `User` 对象，它只认“字节”和“字符串”。序列化的目的，就是**把复杂的内存对象，翻译成 Redis 能听懂、能存储的基础格式**。
    

---

### 二、 什么是 JSON 反序列化？（取数据）

**反序列化（Deserialization）** 就是你的朋友把这份**说明书**（JSON 字符串）通过网络还给你，你拿到说明书后，按照上面的步骤，**重新拼装出了一模一样的乐高城堡**。

- **在 Java/Redis 中的过程：** 你需要读取用户信息，Redis 把那段文本 `{"name":"张三", "age":25}` 扔给你。 你的 Java 程序（反序列化器）拿到这段 JSON 后，自动在内存里重新 `new` 了一个 `User` 对象，并把属性填进去。 你拿到的又是一个可以直接调用 `user.getName()` 的 Java 对象了。
    
- **干什么用的？** 你的 Java 代码无法直接对一串字符串进行面向对象的操作（比如你没法对字符串直接调用 `.getAge()` 方法）。反序列化的目的，就是**把数据库里的死文本，重新复活成能在 Java 代码里跑的活对象**。
## 序列化
**定义:** 序列化是指将对象转化成可存储或可传输的格式，将默认的"乱码"对象转化为常见的String等
直接使用Redis存进去的都是`xac\xed\x00\x05t\x00\x05`之类的乱码
需要在@Configuration配置类中配置序列化器,然后才能正常的输出字符

``` java
@Configuration  
public class RedisConfig {  
    @Bean  
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {  
        // 创建 RedisTemplate 对象  
        RedisTemplate<String, Object> template = new RedisTemplate<>();  
        // 设置连接工厂  
        template.setConnectionFactory(connectionFactory);  
  
  // 1. 创建 String 序列化器，通常用于 Key        StringRedisSerializer stringSerializer = new StringRedisSerializer();  
  
 // 2. 创建 JSON 序列化器，通常用于 Value        // GenericJackson2JsonRedisSerializer 优点是通用，会自动在 JSON 中带上类信息  
 RedisSerializer<Object> jsonSerializer = RedisSerializer.json();  
  
// 3. 配置 Key 的序列化方式为 String        template.setKeySerializer(stringSerializer);  
        template.setHashKeySerializer(stringSerializer);  
  
 // 4. 配置 Value 的序列化方式为 JSON        template.setValueSerializer(jsonSerializer);  
        template.setHashValueSerializer(jsonSerializer);  
  
        // 初始化参数和初始化工作  
        template.afterPropertiesSet();  
        return template;  
    }  
  
}
```

### 效果
![[image-2.png]]

