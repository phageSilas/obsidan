# Result和Result< T>
`Result` 里用 `Object data` 当然能跑，而且很多入门项目就是这么写的。区别不在“能不能用”，而在**类型安全、代码提示、可维护性、接口文档清晰度**。

核心区别是：

```
Result<T>
```

表示：这个返回体里的 `data` 是一个明确类型的对象。

比如：

```
Result<UserRespDTO>
Result<List<UserRespDTO>>
Result<PageResp<UserRespDTO>>
```

而你这个：

```
private Object data;
```

表示：`data` 可以是任何东西，编译器不知道它到底是什么。

举个例子。

如果 Controller 这样写：

```
public Result<UserRespDTO> getUser() {
    UserRespDTO user = userService.getUser();
    return Results.success(user);
}
```

调用方、Swagger、前端类型生成工具、IDE 都能知道：

```
data 是 UserRespDTO
```

如果写成：

```
public Result getUser() {
    UserRespDTO user = userService.getUser();
    return Result.ok(user);
}
```

那 `data` 只是 `Object`。后续如果 Java 代码里要用，就要强转：

```
UserRespDTO user = (UserRespDTO) result.getData();
```

这就有风险。万一实际返回的是别的类型，编译期不报错，运行时才炸：

```
ClassCastException
```

**泛型的最大价值：把错误提前到编译期。**

比如你写：

```
Result<UserRespDTO> result = Results.success(new OrderRespDTO());
```

如果 `Results.success()` 泛型设计得好，IDE 和编译器会提醒你类型不匹配。

但 `Object data` 不会提醒，因为任何对象都能塞进去：

```
Result result = Result.ok(new OrderRespDTO());
```

它觉得没问题。

你的设计：

```
private Boolean success;
private String errorMsg;
private Object data;
private Long total;
```

适合小项目、练习项目、内部接口，简单直接。

但它有几个问题：

1. `data` 类型不明确。
2. `total` 不是所有接口都需要，普通详情接口也会带一个空 `total`。
3. `errorMsg` 只有错误信息，没有错误码，前端不方便按错误类型处理。
4. `success` 只能表示成功失败，不能表达更细的业务错误。
5. 接口文档里 `data` 经常显示成 `object`，可读性差。

更推荐的写法是：

```
@Data
public class Result<T> {
    private String code;
    private String message;
    private T data;
}
```

分页不要把 `total` 放在最外层，而是单独设计分页返回对象：

```
@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
}
```

然后接口返回：

```
Result<PageResult<UserRespDTO>>
```

这样结构更清楚：

```
{
  "code": "0",
  "message": "success",
  "data": {
    "records": [],
    "total": 100
  }
}
```

而不是所有接口都长这样：

```
{
  "success": true,
  "errorMsg": null,
  "data": {},
  "total": null
}
```

简单说：

```
Result
```

能用，适合快速写。

```
Result<T>
```

更规范，适合长期维护。

你如果是自己从 0 做项目，我建议这样设计：

```
Result<T> {
    code;
    message;
    data;
}
```

分页另起：

```
PageResult<T> {
    records;
    total;
}
```

这套结构简单、清晰、扩展性好，也不会一开始就搞得太复杂。

# Result< T>的处理方式
Results的方法可以写进 `Result` 类里，完全能用。单独拆一个 `Results`，更多是**代码风格和职责拆分**，不是必须。

比如你这样写也没问题：

``` java
@Data
public class Result<T> {
    private String code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode("0")
                .setData(data);
    }

    public static Result<Void> success() {
        return new Result<Void>().setCode("0");
    }

    public static Result<Void> failure(String code, String message) {
        return new Result<Void>()
                .setCode(code)
                .setMessage(message);
    }
}
```

然后 Controller 里：

```
return Result.success(user);
```

这是很常见、很推荐新项目使用的方式。

它单独设计一个 `Results`：

```
Result<T>    // 只表示返回体的数据结构
Results      // 专门负责构造 Result 对象
```

主要有几个好处。

第一，职责更单一。

`Result` 是一个 DTO，负责描述响应格式：

```
code
message
data
requestId
```

`Results` 是一个工具类，负责创建成功、失败响应：

``` java
Results.success()
Results.success(data)
Results.failure()
Results.failure(exception)
```

这就是把“数据结构”和“构造逻辑”分开。

第二，避免 `Result` 类越来越臃肿。

项目大了以后，可能会出现很多构造方法：

``` java
success()
success(data)
success(data, message)
failure()
failure(errorCode)
failure(errorCode, message)
failure(AbstractException)
failure(IErrorCode)
failure(Throwable)
```

如果全塞进 `Result`，这个类既像 DTO，又像工具类。拆成 `Results` 后，`Result` 保持干净。

第三，语义上更像“工厂”。

`Results.success(user)` 的意思是：用 `Results` 这个响应构造器创建一个成功结果。

类似 Java 里的：

```
Collections.emptyList()
Objects.requireNonNull()
Optional.ofNullable()
```

对象本身是 `List`、`Object`、`Optional`，但构造或工具方法放在另一个类里。

不过，对你自己的项目来说，我更建议一开始直接写在 `Result` 里，因为更简单：

```
return Result.success(user);
return Result.fail("用户名或密码错误");
```

等项目变大了，再拆 `Results` 也不迟。

我会这样取舍：

```
小项目 / 学习项目 / 单体业务系统：
Result 里直接放静态方法，简单清楚。

中大型项目 / 团队规范强 / 响应构造逻辑多：
Result 只做数据结构，Results 做构造工具类。
```

所以答案是：**不是不行，而是他选择了“DTO 和构造器分离”的写法。你自己做项目时，把静态方法写进 `Result<T>` 里完全可以，而且更适合起步。**
