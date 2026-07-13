当然。你选中的这个：

```
@Accessors(chain = true)
```

就是 Lombok 里很常见但新手容易忽略的注解。

它的作用是让 setter 支持链式调用。

普通 setter：

```
Result result = new Result();
result.setCode("0");
result.setMessage("成功");
result.setData(user);
```

加了：

```
@Accessors(chain = true)
```

之后可以写成：

```
Result result = new Result()
        .setCode("0")
        .setMessage("成功")
        .setData(user);
```

所以这个项目里的 `Results.success()` 才能这么写：

```
return new Result<T>()
        .setCode(Result.SUCCESS_CODE)
        .setData(data);
```

---

除了你提到的：

```
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
```

后端项目里还经常会用这些。

**`@Getter` / `@Setter`**

只生成 getter 或 setter，比 `@Data` 更精细。

```
@Getter
@Setter
public class UserDTO {
    private String username;
}
```

如果是错误码枚举，一般只需要 `@Getter`，不应该给 `@Setter`。

---

**`@Builder`**

生成建造者模式，适合字段比较多的 DTO。

```
@Builder
@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private String realName;
}
```

使用：

```
UserInfoDTO user = UserInfoDTO.builder()
        .userId(1L)
        .username("zhangsan")
        .realName("张三")
        .build();
```

比超长构造方法清楚很多。

---

**`@Slf4j`**

自动生成日志对象。

```
@Slf4j
@Service
public class UserServiceImpl {

    public void create() {
        log.info("创建用户");
        log.error("创建用户失败");
    }
}
```

不用你手动写：

```
private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
```

非常常用。

---

**`@EqualsAndHashCode`**

生成 `equals()` 和 `hashCode()`。

继承父类时经常这样写：

```
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDO extends BaseDO {
    private Long id;
}
```

如果实体继承了 `BaseDO`，但你希望比较时包含父类字段，就用 `callSuper = true`。

---

**`@ToString`**

生成 `toString()`。

```
@ToString
public class UserDTO {
    private String username;
}
```

可以排除敏感字段：

```
@ToString(exclude = "password")
public class UserDTO {
    private String username;
    private String password;
}
```

不过现在更推荐：

```
@ToString.Exclude
private String password;
```

---

**`@Accessors`**

控制 getter/setter 风格。

链式调用：

```
@Accessors(chain = true)
```

setter 返回当前对象。

fluent 风格：

```
@Accessors(fluent = true)
```

普通写法：

```
user.getUsername();
user.setUsername("zhangsan");
```

fluent 后：

```
user.username();
user.username("zhangsan");
```

后端项目里更常用 `chain = true`，不太建议随便用 `fluent = true`，因为会影响 JavaBean 规范，某些框架可能识别不自然。

---

**`@SneakyThrows`**

不用显式写 `try-catch` 或 `throws`，Lombok 会偷偷帮你抛出受检异常。

```
@SneakyThrows
public void readFile() {
    Files.readString(Path.of("a.txt"));
}
```

等价于你绕过了：

```
throws IOException
```

这个注解要慎用。业务代码里不建议乱用，因为异常来源会变得不明显。工具类、测试代码里可以偶尔用。

---

## **`@Value`**

创建不可变对象，类似：

```
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
```

并且字段默认是 `private final`。

```
@Value
public class UserInfo {
    Long id;
    String username;
}
```

适合值对象，但不太适合 MyBatis/JPA 实体，因为实体通常需要无参构造和 setter。

---

**`@With`**

基于不可变对象复制一个新对象，只改某个字段。

```
@Value
@With
public class UserInfo {
    Long id;
    String username;
}
```

使用：

```
UserInfo user1 = new UserInfo(1L, "zhangsan");
UserInfo user2 = user1.withUsername("lisi");
```

`user1` 不变，`user2` 是新对象。

---

## **`@NonNull`**

给参数或字段加非空检查。

```
public void create(@NonNull String username) {
    // username 为 null 会抛 NullPointerException
}
```

不过在 Web 项目里，参数校验更推荐用：

```
@NotNull
@NotBlank
@Valid
```

也就是 Jakarta Validation 那套。

---

**`@Cleanup`**

自动关闭资源，类似 try-with-resources。

```
@Cleanup InputStream inputStream = new FileInputStream("a.txt");
```

实际项目中更推荐直接写 Java 原生：

```
try (InputStream inputStream = new FileInputStream("a.txt")) {
}
```

所以这个注解知道就行。

---

**`@FieldNameConstants`**

生成字段名常量，适合避免魔法字符串。

```
@FieldNameConstants
public class UserDO {
    private String username;
    private String phone;
}
```

会生成类似：

```
UserDO.Fields.username
UserDO.Fields.phone
```

不过 MyBatis-Plus 项目里常用 LambdaQueryWrapper：

```
.eq(UserDO::getUsername, username)
```

所以这个不一定常用。

---

我建议你后端项目里重点掌握这些：

```
@Getter
@Setter
@Builder
@Slf4j
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString.Exclude
@SneakyThrows
```

但有个经验：**实体类、DTO 可以用 Lombok 简化；核心业务逻辑不要为了少写几行代码而让可读性变差。**

尤其是这几个要慎用：

```
@SneakyThrows
@Accessors(fluent = true)
@Data 用在继承类上
```

`@Data` 会同时生成 getter、setter、toString、equals、hashCode，有时候太“全家桶”。实际项目里，很多类用：

```
@Getter
@Setter
```

反而更稳。

## value
对，你记得没错。这里有两个完全不同的 `@Value`，只是名字一样。

### 一个是 **Spring 的 `@Value`**：

```
import org.springframework.beans.factory.annotation.Value;
```

它用来从配置文件、环境变量、表达式里给字段赋值。

比如 `application.yml`：

```
short-link:
  domain:
    default: ysnb.nb:8001
```

Java 里：

```
@Component
public class ShortLinkProperties {

    @Value("${short-link.domain.default}")
    private String defaultDomain;
}
```

也可以写默认值：

```
@Value("${short-link.domain.default:localhost:8080}")
private String defaultDomain;
```

这个常用于读取单个配置项。

---

### 另一个是 **Lombok 的 `@Value`**：

```
import lombok.Value;
```

它用于创建不可变对象。

```
@Value
public class UserInfo {
    Long id;
    String username;
}
```

大概等价于：

```
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserInfo {
    private final Long id;
    private final String username;
}
```

它和配置文件赋值没有关系。

---

所以区别是：

|注解|包名|作用|
|---|---|---|
|Spring `@Value`|`org.springframework.beans.factory.annotation.Value`|从配置文件/环境变量/表达式注入值|
|Lombok `@Value`|`lombok.Value`|创建不可变对象|

实际开发时一定看 import：

```
import org.springframework.beans.factory.annotation.Value; // Spring 配置注入
```

```
import lombok.Value; // Lombok 不可变对象
```

不过在 Spring Boot 项目里，如果配置项比较多，我更推荐用：

```
@ConfigurationProperties
```

比如：

```
short-link:
  flow-limit:
    enable: true
    time-window: 1
    max-access-count: 20
```

对应：

```
@Data
@Component
@ConfigurationProperties(prefix = "short-link.flow-limit")
public class FlowLimitProperties {

    private Boolean enable;

    private Integer timeWindow;

    private Long maxAccessCount;
}
```

这个项目里的：

```
UserFlowRiskControlConfiguration
```

就是这种写法。

简单建议：

```
单个配置：用 Spring @Value
一组配置：用 @ConfigurationProperties
不可变对象：用 Lombok @Value
```