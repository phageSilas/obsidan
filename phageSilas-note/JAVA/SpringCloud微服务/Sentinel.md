# Sentinel的作用
在[Redis]中叫做**哨兵**，用于监控主从切换，但是在微服务中叫做**流量防卫兵**。
![[JAVA/SpringCloud微服务/Attachment/image-1.png]]

# 导入
除了Nacos的依赖，还需要添加一个sentinel的依赖：

``` java
 <!--sentinel的依赖-->
<dependency>
 <groupId>com.alibaba.cloud</groupId>  
 <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

配置导入
``` yml
 spring:
   cloud:
     sentinel:
       transport:
        ## 指定控制台的地址，默认端口8080
         dashboard: localhost:8080
```


# 兜底
在 Spring Cloud Alibaba Sentinel 中，`@SentinelResource` 注解是我们最常用的功能之一。它允许我们为特定的方法定义“资源”，并为其配置流控、熔断和兜底逻辑。

在使用该注解时，`fallback` 和 `blockHandler` 是两个极易混淆的属性。虽然它们的作用都是在原方法无法正常执行时提供一个“备用方案”（兜底），但它们的**触发时机**和**针对的异常类型**有着本质的区别。

下面我们将详细解释 `fallback`，并对比它与 `blockHandler` 的核心差异。

---
## 1. 什么是 `fallback`（兜底回调）？

`fallback` 主要是为了处理**业务运行时异常（Runtime Exceptions）**。

- **触发时机：** 当你的业务代码在执行过程中抛出了异常（例如：`NullPointerException`、数据库连接超时、除以零等），导致方法无法正常返回时，Sentinel 会捕获这个异常，并去调用你指定的 `fallback` 方法。
    
- **核心目的：** 实现**服务降级**。当服务内部发生错误时，为了避免直接给用户返回冰冷的 500 错误页面或导致调用链路雪崩，我们可以通过 `fallback` 返回一个默认的缓存值或友好的提示信息。
    

**代码示例：**
``` Java
@SentinelResource(value = "getUserInfo", fallback = "fallbackHandler")
public User getUserInfo(String userId) {
    // 模拟业务报错
    if ("123".equals(userId)) {
        throw new RuntimeException("数据库查询超时！");
    }
    return new User(userId, "正常用户");
}

// Fallback 方法：参数必须和原方法完全一致，最后可以多加一个 Throwable 类型的参数用于接收异常

public User fallbackHandler(String userId, Throwable e) {
    System.out.println("发生业务异常：" + e.getMessage());
    return new User(userId, "默认降级用户"); // 兜底返回
}
```
 fallback 方法参数一般会额外加一个Throwable
 blockhandler方法参数一般添加额外一个BlockException
---

## 2. `fallback` 与 `blockHandler` 的核心区别

`blockHandler` 的全称是 Block Exception Handler（阻塞异常处理器）。它专门用于处理 **Sentinel 规则拦截** 所产生的异常。

以下是它们的多维度对比：

| **比较维度**    | **fallback (业务兜底)**                      | **blockHandler (规则兜底)**                                                                    |
| ----------- | ---------------------------------------- | ------------------------------------------------------------------------------------------ |
| **针对的异常类型** | `Throwable` 及其子类（普通的 Java 运行时业务异常）。      | 仅针对 Sentinel 内部的 `BlockException` 及其子类（如 FlowException, DegradeException 等）。               |
| **触发原因**    | **代码自己报错了**（比如空指针、SQL 异常）。               | **触发了 Sentinel 控制台配置的规则**（比如 QPS 超出了限流阈值、触发了熔断降级规则、触发了热点参数规则）。代码可能本身没问题，但被 Sentinel 强制拦截了。 |
| **方法签名要求**  | 参数列表必须与原方法一致。**最后可附加一个 `Throwable` 参数**。 | 参数列表必须与原方法一致。**最后必须附加一个 `BlockException` 参数**（如果不加，Sentinel 无法找到该方法）。                      |
| **类级别的配置**  | 可通过 `fallbackClass` 指定专门的兜底类。            | 可通过 `blockHandlerClass` 指定专门的阻塞处理类。                                                        |

---

## 3. 当它们同时存在时，优先级是怎样的？
优先blockhandler

在实际开发中，我们通常会同时配置这两个属性，以保证系统“固若金汤”：
``` Java
@SentinelResource(
    value = "doSomething",
    blockHandler = "handleBlock",  // 应对限流、熔断
    fallback = "handleFallback"    // 应对代码 Bug、数据库异常
)
public String doSomething(int id) {
    // 业务逻辑
}
```

**执行与流转逻辑如下：**

1. **请求进来，先过 Sentinel 的规则检查（限流、系统保护等）。**
    - 如果触发了限流等规则，抛出 `BlockException`，此时**直接进入 `blockHandler`**。
        
2. **规则检查通过，开始执行业务代码。**
    - 如果业务代码执行正常，返回正常结果。
    - 如果业务代码抛出了普通异常（如 `IllegalArgumentException`），此时会**进入 `fallback`**。
     
3. **特殊情况（熔断降级）：**
    
    - 如果 Sentinel 配置了**异常比例/异常数熔断降级规则**。当业务异常的次数达到阈值后，Sentinel 会将该资源熔断（断路器打开）。
        
    - 在熔断期间，新的请求进来会直接被 Sentinel 拦截，抛出 `DegradeException` (属于 `BlockException`)，此时**会进入 `blockHandler`**。
        

## 总结建议

- **把 `blockHandler` 当作“交警”**：专门处理因为交通管制（限流）或道路施工（熔断）而被拦下来的请求。
    
- **把 `fallback` 当作“备用轮胎”**：专门处理车子自己抛锚（代码抛出异常）的情况。
    

**注意一个历史版本的细节：** 在 Sentinel 1.6.0 之前的版本，`fallback` 只能处理业务异常。从 Sentinel 1.6.0 开始，如果你的请求被限流了（抛出 `BlockException`），但是你**只配置了 `fallback` 而没有配置 `blockHandler`**，那么 `fallback` 也会作为最终的“接盘侠”去处理这个 `BlockException`。但最佳实践依然是**将两者分开配置，各司其职**。