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

# SentinelWenInterceptor下的BlockExceptionHandler和@SentinelResource的blockHandler有什么区别
简单来说：

- **`BlockExceptionHandler`** 是**全局 Web 级别**的“大门保安”。
    
- **`@SentinelResource` 的 `blockHandler`** 是**局部方法级别**的“私人保镖”。
    

以下是它们的详细对比和核心区别：

## 1. 作用范围与拦截维度的区别

- **`BlockExceptionHandler`（全局 Web 拦截）：**
    
    - **作用域：** 全局生效。它是由 `SentinelWebInterceptor`（Sentinel 提供的 Spring MVC 拦截器）触发的。
        
    - **拦截维度：** 针对 **HTTP URL 请求**。在 Sentinel 控制台中，你通常是给某一个接口路径（比如 `/api/getUser`）配置了流控规则。当请求这个 URL 被限流时，就会触发它。
        
    - **定位：** 它是统一处理 Web 接口限流降级的统管枢纽。
        
- **`@SentinelResource` 的 `blockHandler`（局部方法拦截）：**
    
    - **作用域：** 局部生效。只对加了 `@SentinelResource` 注解的那个**具体 Java 方法**生效。
        
    - **拦截维度：** 针对 **Java 方法调用**（资源名为注解中 `value` 属性定义的值）。无论这个方法是被 HTTP 请求调用的，还是被内部定时任务、RPC 远程调用的，只要触发了该资源的限流规则，就会被拦截。
        
    - **定位：** 它是针对某一个核心方法提供精细化、个性化的兜底逻辑。
![[MyBlockExceptionHandler.java]]

## 2. 代码实现与处理方式的区别

这是两者在日常开发中体感差异最大的一点，主要是**返回值类型**的限制不同。

**A. `BlockExceptionHandler` (处理 HTTP 响应)**

因为它拦截的是 Web 请求，所以它的工作是**直接操作原生的 HTTP 响应流**，返回友好的 JSON 给前端。
``` Java
@Component
public class MyGlobalBlockExceptionHandler implements BlockExceptionHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        // 1. 设置响应头
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=utf-8");
        
        // 2. 根据不同的 BlockException 子类（限流、降级等）返回不同的 JSON 信息
        String jsonMsg = "{\"code\":-1, \"msg\":\"系统繁忙，请稍后再试 (全局Web限流)\"}";
        
        // 3. 直接写回给前端
        response.getWriter().write(jsonMsg);
    }
}
```

**B. `@SentinelResource` 的 `blockHandler` (处理业务对象)**

因为它拦截的是具体的 Java 方法，所以它的**返回值必须和原方法完全保持一致**，以保证调用方拿到符合预期的业务对象类型。
``` Java
@Service
public class OrderService {

    // 资源名为 "createOrder"
    @SentinelResource(value = "createOrder", blockHandler = "handleCreateOrderBlock")
    public OrderDTO createOrder(Long userId, String productId) {
        // 核心下单逻辑...
        return new OrderDTO(123L, "下单成功");
    }

    // blockHandler 方法：返回值必须是 OrderDTO，参数必须和原方法一致，加上 BlockException
    public OrderDTO handleCreateOrderBlock(Long userId, String productId, BlockException ex) {
        System.out.println("触发方法级别限流");
        // 返回一个默认的/兜底的业务对象
        return new OrderDTO(null, "系统繁忙，下单失败 (方法级限流)");
    }
}
```

## 3. 当两者冲突时，谁会生效？（执行顺序）

在 Spring Web 环境中，一个 HTTP 请求进来，会**先经过 Spring MVC 的 Interceptor（拦截器），然后再执行到 Controller，最后调用 Service 里的方法**。

因此：

1. 如果限流规则是配置在 **URL 路径**上（比如 `/create`），请求在 Web 层就被 `SentinelWebInterceptor` 拦住了，此时会执行 **`BlockExceptionHandler`**。请求根本走不到后面的 Controller 和 Service 方法里。
    
2. 如果 URL 路径没有被限流，请求顺利进入了方法内部，而在执行到带有 `@SentinelResource("createOrder")` 的方法时，触发了配置在 **资源名 `createOrder`** 上的规则，此时会执行 **`blockHandler`**。
    

### 总结与最佳实践

|**维度**|**BlockExceptionHandler**|**@SentinelResource 的 blockHandler**|
|---|---|---|
|**层级**|Web 接入层 (Controller 之前)|业务逻辑层 (Controller / Service 内部)|
|**拦截对象**|HTTP URL (如 `/api/login`)|Java 方法 (如 `doLogin()`)|
|**通用性**|**高**。只需写一次，全局所有 API 限流时都会返回统一的 JSON 格式。|**低**。需要为每个重要方法单独写兜底逻辑，定制化程度高。|
|**返回值**|`void` (通过 `HttpServletResponse` 写出 JSON)|与原拦截方法完全一致的返回值类型|

**日常开发怎么选？**

- **绝大多数情况：** 推荐实现 `BlockExceptionHandler`。因为对于大部分 Web 接口限流，前端只需要一个统一的 JSON 错误提示（例如：“当前访问人数过多，请稍后再试”）即可，没必要每个接口都去写一遍兜底代码。
    
- **核心链路场景：** 对于极少数核心的方法（比如获取用户资产详情），如果被限流了不能简单地报错，而是需要返回一些默认值或者走备用缓存，这时候再在该方法上使用 `@SentinelResource` 和 `blockHandler` 进行精细化兜底。

也就是说 SentinelWenInterceptor下的BlockExceptionHandle 是给全局用的(各种Mapping请求)
@SentinelResource的blockHandler是具体到某一个方法


