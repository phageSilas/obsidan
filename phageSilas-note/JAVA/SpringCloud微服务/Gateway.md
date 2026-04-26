![[image-2.png]]


在微服务架构中，如果说 OpenFeign 是微服务之间互相串门的“内部快递员”，Nacos 是“114查号台”，那么 **Spring Cloud Gateway 就是整个微服务系统的“大门保安和前台接待”**（API 网关）。

所有的外部请求（比如来自浏览器、App 的请求）不能直接去访问内部零散的微服务，而是必须先经过 Gateway 这个唯一的入口。

它是 Spring 官方基于 Spring 5.0、Spring Boot 2.0 和 Project Reactor 等技术开发的网关，旨在替代老旧的 Netflix Zuul。它最大的特点是采用了**非阻塞的响应式编程模型（基于 WebFlux 和 Netty）**，因此并发性能极其强悍。

### 1. 为什么需要 Gateway？（核心作用）

如果没有网关，前端需要记住几百个微服务的 IP 和端口，一旦服务重启或扩容，前端就要疯掉。有了 Gateway 之后，它承担了以下核心职责：

- **统一入口（反向代理与路由）：** 屏蔽内部微服务的复杂性。前端只需要调用网关暴露的统一地址（比如 `api.example.com`），网关会根据规则自动将请求转发到对应的后端微服务。
    
- **统一鉴权（Security）：** 就像保安查工牌一样。不需要在每个微服务里都写一遍 Token 校验代码，直接在网关层面统一验证，没登录的请求直接在网关层被打回。
    
- **统一限流（Rate Limiting）：** 结合 Sentinel 或 Redis，在最外层控制并发访问量，防止大流量把后端脆弱的业务服务打挂。
    
- **全局跨域处理（CORS）：** 前端分离项目中常见的跨域问题，可以在网关统一配置解决。
    

---

### 2. Gateway 的三大核心概念

要理解 Gateway 是怎么工作的，只需掌握它的三个“基石”：

1. **Route（路由）：** 它是网关最基础的模块。一个路由包含一个 ID、一个目标 URI（要去哪里）、一组 Predicate（断言）和一组 Filter（过滤器）。
    
2. **Predicate（断言）：** 这是 Java 8 中的条件测试概念。你可以把它理解为**“匹配规则”**。比如：“如果请求的路径是 `/api/user/**`”，或者“如果请求头里包含某个参数”，这就是断言。断言为 `true`，才会执行路由转发。
    
3. **Filter（过滤器）：** 顾名思义，拦截器。它可以在请求被转发之前（Pre）或者之后（Post）对请求进行**修改**。比如：往请求头里塞入当前用户的 ID、记录日志、修改响应内容等。
    

---

### 3. 它是怎么和 Nacos 配合的？（代码示例）

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
    

### 4. 总结

Spring Cloud Gateway 是现代 Spring 微服务架构中不可或缺的组件。它把那些非业务的代码（路由、鉴权、限流）全部从具体的微服务中抽离出来，集中在一个高并发的入口处进行统一管理，大大减轻了微服务本身的开发负担。