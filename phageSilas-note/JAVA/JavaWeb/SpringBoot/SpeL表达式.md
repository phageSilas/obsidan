**SpEL（Spring Expression Language，Spring 表达式语言）**，是 Spring 框架提供的一种功能强大的**运行时表达式语言**。它可以在运行时查询和操作对象图，支持动态计算值、调用方法、访问属性及进行逻辑运算。

可以把它理解为一个**专门为 Spring 打造的“迷你脚本引擎”**，允许你用简洁的字符串表达式，在运行时动态获取数据或执行逻辑。

---

### 1. 核心区分：`#{}` 与 `${}`（新手最常见困惑）

- **`#{...}`**：这是 **SpEL 表达式**的语法。里面的内容会被 Spring 动态解析和执行（如：`#{2 + 3}` 结果是 `5`）。
- **`${...}`**：这是**属性占位符**，用于读取 `.properties` 或 `.yml` 配置文件中的静态值（如：`${server.port}` 获取端口号）。
- **混合使用**：它们可以结合，比如 `#{'${db.driver}'}`，但极少这么用。

---

### 2. SpEL 能做什么？（核心用途）

SpEL 非常强大，常用场景包括：

- **动态注入值**（配合 `@Value` 注解）。
- **方法参数的条件判断**（Spring Security 权限注解 `@PreAuthorize`）。
- **缓存键的动态生成**（`@Cacheable` 的 `key` 属性）。
- **在 XML 或 Java 配置中执行复杂运算**。

---

### 3. SpEL 实战代码示例

#### 场景一：在 `@Value` 中注入外部属性并运算
```java
@Component
public class MyService {
    // 获取系统属性（JVM 中的 java.home）并转为小写
    @Value("#{systemProperties['java.home'].toLowerCase()}")
    private String javaHome;

    // 执行算术和逻辑运算
    @Value("#{ 10 * 20 > 100 ? '大于100' : '小于等于100' }")
    private String resultDesc;

    // 访问 Spring 容器中的其他 Bean 及其方法
    @Value("#{userService.getDefaultUser().name}")
    private String defaultUserName;
}
```

#### 场景二：在 `@Cacheable` 中使用（动态缓存键）
这是 SpEL 最经典的使用场景，**直接操作方法参数**，通过 `#参数名` 引用：
```java
@Service
public class OrderService {
    // key 动态生成：根据用户ID和订单ID拼接
    @Cacheable(value = "orders", key = "#userId + '_' + #orderId")
    public Order getOrder(Long userId, String orderId) {
        // 查询数据库...
        return new Order();
    }
}
```
> 除了 `#参数名`，还可以用 `#p0` 代表第一个参数，`#root.methodName` 代表当前方法名。

#### 场景三：Spring Security 权限控制
```java
@RestController
public class UserController {
    // 只有当登录用户的ID等于传入的userId时，才允许访问
    @PreAuthorize("#userId == authentication.principal.id")
    public User getUser(@PathVariable Long userId) {
        return userService.findById(userId);
    }
}
```

---

### 4. SpEL 中的核心变量与上下文（必知）

在解析表达式时，SpEL 有一个 **EvaluationContext（评估上下文）**，它默认提供了几个内置变量：

| 关键字 | 含义 | 使用示例 |
| :--- | :--- | :--- |
| **`#this`** | 当前被操作的对象 | 集合过滤时取当前元素 |
| **`#root`** | 根对象（通常是方法的参数或目标对象） | `#root.methodName` |
| **`#参数名`** | 方法入参（如 `#id`，`#user`） | `#user.name` |
| **`#p0` ~ `#pn`** | 方法入参的索引（0开始） | `#p0` 代表第一个参数 |

---

### 5. SpEL 强大的运算符支持

SpEL 支持几乎所有的 Java 运算符，甚至支持正则匹配和集合筛选：

- **算术**：`+`, `-`, `*`, `/`, `%`
- **关系/逻辑**：`>`, `<`, `==`, `&&`, `||`, `!`，以及 `?:`（三元运算符）
- **正则匹配**：`matches`（例如 `#{'abc' matches '^[a-z]+$'}` 返回 `true`）
- **集合筛选与投影**（极强）：

假设有一个 `users` 集合，你想获取所有年龄 > 18 的用户的姓名列表：
```java
// .?[条件] 为筛选，.![属性] 为投影（提取属性）
@Value("#{users.?[age > 18].![name]}")
private List<String> adultNames;
```

---

### 6. 如何在你刚刚定义的自定义注解中使用 SpEL？

既然你刚问了自定义注解，结合 SpEL 是 Spring 项目中的高阶实操。假设你自定义了一个 `@MyCache` 注解，想支持动态 key：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCache {
    String key(); // 用户传入 SpEL 表达式字符串，比如 "user_#id"
}
```

**解析时**（用 AOP 切面），使用 Spring 内置的 `ExpressionParser` 去解析：

```java
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

// 在 AOP 环绕通知中：
public String parseKey(String keySpel, MethodInvocation invocation) {
    ExpressionParser parser = new SpelExpressionParser();
    // 构建上下文，将方法参数放进去
    StandardEvaluationContext context = new StandardEvaluationContext();
    // 把参数名（如 id）和参数值绑定，让 SpEL 能识别 #id
    context.setVariable("id", invocation.getArguments()[0]); 
    // 解析表达式
    return parser.parseExpression(keySpel).getValue(context, String.class);
}
```

---

### 7. 性能提示（避坑）

- SpEL 表达式在每次解析时都会编译执行（虽然有一定缓存），**尽量避免在高频循环中动态解析新的 SpEL 字符串**。
- 如果表达式固定不变（例如 `#user.name`），Spring 底层会缓存解析后的表达式结构，性能尚可，但远不如直接调用 Java 方法快。

---

**总结一句话**：SpEL 就是 Spring 赋予你的“动态脚本能力”，让你可以在配置、缓存、权限、注入等场景中，像写 Java 代码一样灵活地操作运行时数据。
