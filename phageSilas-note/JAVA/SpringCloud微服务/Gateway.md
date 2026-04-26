![[image-2.png]]

# 配置
![[application-route.yml]]


在微服务架构中，如果说 OpenFeign 是微服务之间互相串门的“内部快递员”，Nacos 是“114查号台”，那么 **Spring Cloud Gateway 就是整个微服务系统的“大门保安和前台接待”**（API 网关）。

所有的外部请求（比如来自浏览器、App 的请求）不能直接去访问内部零散的微服务，而是必须先经过 Gateway 这个唯一的入口。

它是 Spring 官方基于 Spring 5.0、Spring Boot 2.0 和 Project Reactor 等技术开发的网关，旨在替代老旧的 Netflix Zuul。它最大的特点是采用了**非阻塞的响应式编程模型（基于 WebFlux 和 Netty）**，因此并发性能极其强悍。

# 1. 为什么需要 Gateway？（核心作用）

如果没有网关，前端需要记住几百个微服务的 IP 和端口，一旦服务重启或扩容，前端就要疯掉。有了 Gateway 之后，它承担了以下核心职责：

- **统一入口（反向代理与路由）：** 屏蔽内部微服务的复杂性。前端只需要调用网关暴露的统一地址（比如 `api.example.com`），网关会根据规则自动将请求转发到对应的后端微服务。
    
- **统一鉴权（Security）：** 就像保安查工牌一样。不需要在每个微服务里都写一遍 Token 校验代码，直接在网关层面统一验证，没登录的请求直接在网关层被打回。
    
- **统一限流（Rate Limiting）：** 结合 Sentinel 或 Redis，在最外层控制并发访问量，防止大流量把后端脆弱的业务服务打挂。
    
- **全局跨域处理（CORS）：** 前端分离项目中常见的跨域问题，可以在网关统一配置解决。
    

---

# 2. Gateway 的三大核心概念

要理解 Gateway 是怎么工作的，只需掌握它的三个“基石”：

1. **Route（路由）：** 它是网关最基础的模块。一个路由包含一个 ID、一个目标 URI（要去哪里）、一组 Predicate（断言）和一组 Filter（过滤器）。
    
2. **Predicate（断言）：** 这是 Java 8 中的条件测试概念。你可以把它理解为**“匹配规则”**。比如：“如果请求的路径是 `/api/user/**`”，或者“如果请求头里包含某个参数”，这就是断言。断言为 `true`，才会执行路由转发。
    
3. **Filter（过滤器）：** 顾名思义，拦截器。它可以在请求被转发之前（Pre）或者之后（Post）对请求进行**修改**。比如：往请求头里塞入当前用户的 ID、记录日志、修改响应内容等。
    

---

# 3. 它是怎么和 Nacos 配合的？（代码示例）

Gateway 最强大的地方在于它可以与 Nacos 无缝结合，实现**动态路由和负载均衡**。

在传统的 Nginx 中，你可能需要写死目标 IP，但在 Gateway 中，你只需要配置文件（`application.yml`）写上**服务名**即可：

YAML

```
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-route         # 路由的唯一标识（随便起）
          uri: lb://user-service         # 目标地址：lb 代表开启负载均衡，user-service 是 Nacos 里的服务名
          predicates:
            - Path=/user/** # 断言：只要请求路径以 /user/ 开头，就匹配这个路由
          filters:
            - StripPrefix=1              # 过滤器：转发前，把路径的第一层（/user）去掉，防止后端接口不匹配
            - AddRequestHeader=X-Role, VIP # 过滤器：给请求头悄悄塞个参数
```

**工作流程演示：**

1. 前端发送请求：`http://网关IP:网关端口/user/getInfo`
    
2. 网关接收到请求，匹配到 `Path=/user/**` 断言。
    
3. 执行 `StripPrefix=1` 过滤器，路径变成了 `/getInfo`。
    
4. 网关看到 `uri: lb://user-service`，立刻去 Nacos 查号台询问：“谁是 `user-service`？”
    
5. Nacos 返回两个健康的实例 IP。网关的负载均衡器（LoadBalancer）挑了一个。
    
6. 网关最终将请求转发到：`http://目标IP:目标端口/getInfo`，并在请求头中携带了 `X-Role=VIP`。
    

# 4. 总结

Spring Cloud Gateway 是现代 Spring 微服务架构中不可或缺的组件。它把那些非业务的代码（路由、鉴权、限流）全部从具体的微服务中抽离出来，集中在一个高并发的入口处进行统一管理，大大减轻了微服务本身的开发负担。



# 过滤器
## 默认过滤器和全局过滤器的区别
虽然 `default-filters`（默认过滤器）和 `GlobalFilter`（全局过滤器）的最终表现都是**“对所有的请求都生效”**，但它们的**底层基因、配置方式**以及**擅长的场景**有着本质的区别。

打个比方：

- **`default-filters`** 就像是公司大楼的**“标准化行政规定”**（比如：所有进大楼的人，都在胸前贴个访客标）。它大多是拿现成的规则直接套用。
    
- **`GlobalFilter`** 就像是隐形在各个闸机的**“高级安保系统”**（比如：人脸识别、无犯罪记录比对、黑名单拦截）。它需要复杂的定制化代码逻辑。
    

下面我们从几个维度详细对比它们的区别：

### 1. 核心定义与实现原理

- **`default-filters`（默认过滤器）：**
    
    - 它的本质依然是 **`GatewayFilter`（局部过滤器）**。
        
    - 网关内置了几十种局部过滤器工厂（比如加请求头 `AddRequestHeader`、去前缀 `StripPrefix` 等）。通常这些过滤器是配置在某个特定路由（Route）下的。
        
    - `default-filters` 只是 Spring 提供的一个**语法糖**。它相当于把你写在这里的局部过滤器，**批量、强行地**复制粘贴到了配置文件中的每一个路由里面。
        
- **`GlobalFilter`（全局过滤器）：**
    
    - 它是另外一套独立的接口体系（实现 `org.springframework.cloud.gateway.filter.GlobalFilter` 接口）。
        
    - 它**天生就是全局的**。只要你把它写成一个 Spring Bean 注入到容器中，它就会自动作用于所有的路由，不需要在配置文件里做任何绑定。
        

---

### 2. 配置与使用方式的区别

**A. `default-filters`：主打“配置化、零代码”**

通常在 `application.yml` 中配置，直接使用 Spring Cloud Gateway 提供好的内置过滤器规则。非常简单粗暴。

YAML

```
spring:
  cloud:
    gateway:
      # 这里配置的规则，会对下面所有的 routes 都生效
      default-filters:
        - AddResponseHeader=X-Response-Default-Red, Default-Blue # 给所有响应加统一的头
        - StripPrefix=1 # 所有路由默认去掉第一层路径
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
```

**B. `GlobalFilter`：主打“硬编码、高定制”**

必须通过写 Java 代码来实现，拥有极高的自由度。你可以拿到完整的 `ServerWebExchange`（包含请求和响应），去查数据库、调 Redis、做复杂的加密解密等。

Java

```
@Component // 必须交给 Spring 管理
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取请求头中的 token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        // 2. 复杂的定制逻辑：比如校验 token 是否为空，或者去 Redis 查是否过期
        if (StringUtils.isBlank(token)) {
            // 拦截请求，返回 401 状态码
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // 结束请求
        }
        
        // 3. 放行，交给下一个过滤器
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 返回过滤器的执行顺序，数字越小优先级越高
        return 0; 
    }
}
```

---

### 3. 应用场景对比

|**维度**|**default-filters (默认过滤器)**|**GlobalFilter (全局过滤器)**|
|---|---|---|
|**功能来源**|使用官方内置的 30 多种 `GatewayFilterFactory`|开发者自己用 Java 编写|
|**灵活性**|**低**。只能做简单的参数修改（增删改 Header、修改 URL 参数、限流等）。|**极高**。可以植入任意复杂的业务代码（查库、微服务调用、自定义算法）。|
|**典型场景**|1. 给所有请求加上统一个企业内部标识。<br><br>  <br><br>2. 统一去除某个路径前缀。<br><br>  <br><br>3. 统一记录简单的基础日志。|1. **统一网关鉴权**（校验 JWT Token）。<br><br>  <br><br>2. **全局 IP 黑白名单**校验。<br><br>  <br><br>3. 全局跨域预处理。<br><br>  <br><br>4. 复杂的全局性能监控与接口耗时统计。|
|**生效范围**|配置文件中定义的所有路由|整个网关的所有 HTTP 请求|

---

### 4. 当它们相遇时的执行顺序

一个请求进入网关后，如果同时存在这两种过滤器，Gateway 会将它们合并到一个过滤器链（Filter Chain）中，并根据 `@Order` 注解或 `Ordered` 接口的值进行排序执行（**数字越小，优先级越高**）。

如果执行顺序的值（Order）一模一样，默认的优先级是：

**`default-filters` > 路由专属的局部过滤器 > `GlobalFilter`**

### 总结

- 如果你只是想对所有的请求做一些**简单的、标准化的修改**（比如塞个固定的请求头），优先在 YAML 里写 **`default-filters`**，省时省力。
    
- 如果你需要做**统一鉴权、权限校验、复杂的黑白名单拦截**这种需要写大量逻辑代码的功能，毫不犹豫地去写一个 **`GlobalFilter`**。这也是我们在企业级开发中最常写的一种过滤器。

# 断言Predicate和过滤器Filter的区别

但实际上，你的理解有一个小小的偏差：**Predicate（断言）本身就是“条件”，而 Filter（过滤器）才是满足条件后执行的“步骤”。**

为了让你秒懂，我们继续用前面的比喻。如果 Spring Cloud Gateway 是一栋大楼，那么：

- **Predicate（断言）是“前台的指路向导”。** 它的核心任务是**“判断和分发”**（做选择题）。
    
- **Filter（过滤器）是“走廊里的安检员/服务员”。** 它的核心任务是**“加工和处理”**（做操作题）。
    

下面我们从三个核心维度来拆解它们的本质区别：

## 1. 核心职责不同：一个是“寻路”，一个是“加工”

**Predicate（断言）：解决“请求该去哪”的问题。**

- 当一个 HTTP 请求来到网关时，网关里可能配置了成百上千个路由（Route）。网关怎么知道这个请求属于哪个路由？
    
- 这就是 Predicate 的工作。它会对请求的特征（比如 URL 路径、请求头、Cookie、请求方法 GET/POST 等）进行判断。
    
- **它的结果只有 `true` 或 `false`。** 只要有一个条件不满足返回了 `false`，这个路由就被抛弃，网关会接着去匹配下一个路由。
    

**Filter（过滤器）：解决“请求在路上要做什么”的问题。**

- 一旦请求被某个路由的 Predicate 匹配成功（条件为 `true`），这个请求就正式踏上了该路由的“专属走廊”。
    
- 在走廊里，Filter 开始工作。它不对请求的去向做决定，而是直接对请求（Request）或响应（Response）**动手动脚**。
    
- 比如：给请求头塞个 Token、把路径里的前缀砍掉一段、记录一下当前时间戳等。
    

## 2. 针对你刚才的误区纠正

你提到：“不都是满足某个条件后才执行某个步骤么？”

**真相是：Filter 在设计上，默认是**没有条件**的（或者说它的条件就是“只要你进了这条路由，就必须执行”）。**

- **Predicate 的逻辑：** `if (路径 == "/user/**") { 让你进入 user 路由 } else { 滚蛋 }`
    
- **Filter 的逻辑：** `既然你已经进来了，把外套脱掉 (StripPrefix)，戴上 VIP 牌子 (AddRequestHeader)`。
    

当然，如果你自己在写代码实现 `GlobalFilter` 时，可以在代码里写 `if...else...` 来决定要不要拦截，但从网关的宏观架构设计来看，Filter 的本职工作是“执行拦截与修改”，而不是“路由匹配”。

## 3. 用一个快递分拣的例子总结

假设你要寄一个快递到北京：

1. **Predicate（断言 = 分拣扫描仪）：** 包裹顺着传送带过来，扫描仪读取邮编。
    
    “邮编是 100000 吗？” -> **是（true）** -> 拨杆启动，把包裹推入“北京专线”的流水线。
    
    （_在这里，扫描仪只负责判断，不改变包裹本身_）。
    
2. **Filter（过滤器 = 流水线上的包装工）：**
    
    包裹进入北京专线后，经过包装工。
    
    包装工 A：给包裹贴上“航空件”标签（AddRequestHeader）。
    
    包装工 B：把包裹外层破损的纸箱撕掉（StripPrefix）。
    
    （_在这里，包装工不关心你要寄到哪，因为能到这条流水线的一定是去北京的，他们只负责修改包裹_）。
    

### 对比清单

|**维度**|**Predicate (断言)**|**Filter (过滤器)**|
|---|---|---|
|**底层返回值**|`boolean` (True 或 False)|`ServerWebExchange` (修改后的请求/响应上下文)|
|**核心动作**|**Match** (匹配)|**Intercept & Modify** (拦截与修改)|
|**发生时机**|在确定路由**之前**执行|在确定路由**之后**，转发给微服务之前/之后执行|
|**如果失败了**|返回 404 (没找到对应路由)|返回 401/403 (没权限) 或其他自定义错误|

一句话概括：**Predicate 决定了请求“走哪条路”，Filter 决定了请求在路上“要经历什么”。**