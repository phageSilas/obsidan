`Filter` 和 `Interceptor` 都能在请求进入 Controller 前后做处理，但它们所处的位置不一样。

简单理解：

```
浏览器请求
  ↓
Filter
  ↓
DispatcherServlet
  ↓
Interceptor
  ↓
Controller
  ↓
Interceptor
  ↓
DispatcherServlet
  ↓
Filter
  ↓
响应返回
```

**Filter 更靠外，Interceptor 更靠近 Controller。**

---

## `Filter` 是 Servlet 规范里的东西，不是 Spring MVC 独有的。

它能拦截几乎所有进入 Web 容器的请求，比如：

```
/api/user
/static/a.png
/favicon.ico
```

常见用途：

- 跨域处理
- 编码处理
- 请求日志
- 链路追踪 traceId
- 读取/包装原始 Request、Response
- 网关层、限流、安全过滤
- 在请求还没进入 Spring MVC 前做处理

写法大概是：

``` java
@Component
public class TraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            // Controller 执行前
            chain.doFilter(request, response);
            // Controller 执行后
        } finally {
            // 清理资源
        }
    }
}
```

---

## `Interceptor` 是 Spring MVC 提供的机制。

它只拦截 Spring MVC 管理的请求，也就是最终会进入 Controller 的请求。

常见用途：

- 登录校验
- 权限校验
- 设置当前用户上下文
- 接口访问日志
- Controller 前后的业务级处理

写法大概是：

``` java
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // Controller 执行前
        return true; // 放行
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        // Controller 执行后，视图渲染前
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 整个请求完成后，适合清理 ThreadLocal
    }
}
```

还需要注册：

``` java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/user/register");
    }
}
```

---

核心区别：

|对比|Filter|Interceptor|
|---|---|---|
|来源|Servlet 规范|Spring MVC|
|执行位置|DispatcherServlet 之前|DispatcherServlet 之后、Controller 之前|
|拦截范围|更广，几乎所有 Web 请求|主要是 Controller 请求|
|是否知道 Controller 方法|不知道|知道 handler，能拿到目标 Controller/方法|
|注册方式|`FilterRegistrationBean` / `@Component`|`WebMvcConfigurer#addInterceptors`|
|适合场景|跨域、编码、日志、限流、包装请求响应|登录校验、权限校验、用户上下文|
|依赖 Spring MVC|不依赖|依赖|

---

你做普通单体后端项目，可以这样选：

**登录鉴权：优先用 Interceptor。**

因为登录鉴权通常是面向接口的，而且你可以很方便地排除登录、注册接口：

```
.excludePathPatterns("/api/user/login", "/api/user/register")
```

**通用请求处理：用 Filter。**

比如：

```
每个请求生成 traceId
统一设置编码
包装 Request 使 body 可重复读
限流
跨域
```

---

在你看的短链项目里：

`admin` 用 Filter：

```
UserTransmitFilter
UserFlowRiskControlFilter
```

`project` 用 Interceptor：

```
UserTransmitInterceptor
```

这两个都能做用户上下文传递，只是层级不同。

如果是你自己的项目，我建议这样设计：

```
Filter:
- TraceIdFilter
- RequestLogFilter
- CorsFilter

Interceptor:
- LoginInterceptor
- PermissionInterceptor
- UserContextInterceptor
```

更简单点，初版只需要：

```
LoginInterceptor
```

在里面完成：

```
读取 token -> 校验 token -> 查询用户 -> 放入 UserContext -> 请求结束清理
```