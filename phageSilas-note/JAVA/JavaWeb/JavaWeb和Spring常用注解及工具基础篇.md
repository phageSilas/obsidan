 # 核心
## @SpringBootApplication
通常用于标注主启动类
# 注册
Spring 容器需要知道哪些类需要被管理为 Bean。除了使用 `@Bean` 方法显式声明（通常在 `@Configuration` 类中），更常见的方式是使用 Stereotype（构造型） 注解标记类，并配合组件扫描（Component Scanning）机制，**让 Spring 自动发现并注册这些类作为 Bean。这些 Bean 后续可以通过 `@Autowired` 等方式注入到其他组件中。**

## @Component

通用注解，若一个类不属于 Service、Dao 或 Controller 层，可以使用它。
## @Repository
标注当前类为DAO（数据访问层），主要用于数据库相关操作。
## @Service
标注当前类为服务层，处理核心业务代码，主要涉及一些复杂的逻辑，需要用到 Dao 层
## @Controller
标注当前类为控制层，对应 Spring MVC 控制层，主要用于接受用户请求并调用 Service 层返回数据给前端页面。
## @RestController
等效于 @Controller + @ResponseBody， 用于前后端分离项目，直接返回数据（如 JSON/XML），而不是跳转页面。（其所有处理器方法（handler methods）的返回值都会被自动序列化（通常为 JSON）并写入 HTTP 响应体，而不是被解析为视图名称。）

# 依赖注入
用于将 Spring 容器中的 Bean 类注入到其他类中
## @Autowired
Spring自带，默认按类型(Type) 装配。使用的前提是本类有@Component,被IOC容器管理的才能使用@Autowired注入
### @Qualifier

	当存在多个相同类型的 Bean 时，@Autowired默认按类型注入可能产生歧义。此时，可以与 @Qualifier 结合使用，通过指定 Bean 的名称来精确选择需要注入的实例。

``` java
@Repository("userRepositoryA") public class UserRepositoryA implements UserRepository { /* ... */ } 

@Repository("userRepositoryB") public class UserRepositoryB implements UserRepository { /* ... */ } 

@Service public class UserService { @Autowired @Qualifier("userRepositoryA") // 指定注入名为 "userRepositoryA" 的 Bean private UserRepository userRepository; // ... }

```
### @primary
	@Primary同样是为了解决同一类型存在多个 Bean 实例的注入问题。在 Bean 定义时（例如使用 @Bean或类注解）添加 @Primary 注解，表示该 Bean 是首选的注入对象。当进行 @Autowired 注入时，如果没有使用 @Qualifier 指定名称，Spring 将优先选择带有 @Primary 的 Bean。
``` java
@Primary // 将 UserRepositoryA 设为首选注入对象 @Repository("userRepositoryA") public class UserRepositoryA implements UserRepository { /* ... */ } 

@Repository("userRepositoryB") public class UserRepositoryB implements UserRepository { /* ... */ }

 @Service public class UserService { @Autowired // 会自动注入 UserRepositoryA，因为它是 @Primary private UserRepository userRepository; // ... }
```

## 注意
每一个类/接口对应一个@Autowired,一个@Autowired只能管理一个类/对象,不会同时注入多个对象
**正确✅:**
``` java
@Autowired  
private EmpMapper empMapper;  
@Autowired  
private EmpExprMapper empExprMapper;
```
**错误❌**
``` java
@Autowired  
private EmpMapper empMapper;   
private EmpExprMapper empExprMapper;//empExprMapper并未被注入
```
![[JAVA/JavaWeb/SpringBoot/Attachment/image-7.png]]


# HTTP请求
主要用于 Controller 层，定义 API 接口。
## @RequestMapping
配置 URL 映射，可以作用于类（定义基础路径）或方法上。

## @GetMapping
请求从服务器获取特定资源。
``` java
@GetMapping("users")
等价于
@RequestMapping(value="/users",method=RequestMethod.GET)


@GetMapping("/users")
public ResponseEntity<List<User>> getAllUsers() {
 return userRepository.findAll();
}
```
## @PostMapping
用于接收 JSON 数据并映射为 Java 对象。常用于更新
``` java
@PostMapping("users") 
等价于
@RequestMapping(value="/users",method=RequestMethod.POST)

@PostMapping("/users") public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) { 
return userRepository.save(userCreateRequest); 
}

```
## @PutMapping
更新服务器上的资源（客户端提供更新后的整个资源）。举个例子：`PUT /users/12`（更新编号为 12 的学生）
``` java
@PutMapping("/users/{userId}") 
等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.PUT)。

@PutMapping("/users/{userId}")
public ResponseEntity<User> updateUser(@PathVariable(value = "userId") Long userId,
  @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
  ......
}

```
## @DeleteMapping
从服务器删除特定的资源。举个例子：`DELETE /users/12`（删除编号为 12 的学生）
``` java
@DeleteMapping("/users/{userId}")
等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.DELETE)

@DeleteMapping("/users/{userId}") public ResponseEntity deleteUser(@PathVariable(value = "userId") Long userId){ 
...... 
}

```
## @PatchMapping
一般PUT 不够用了之后才用 PATCH 请求去更新数据。

## 注意
弱@RestMapping后加了参数,下面的具体的Maping后就不要加参数了,例如
前有@RequestMapping("/emp"),下有@DeleteMapping("/emp")
呢么运行时实际分会给前端的是/emp/emp,重复了


# 参数接收/参数绑定/参数配置
用于绑定请求参数到方法参数中

## @PathVariable
用于从 URL 路径中提取参数。
**用{ }标识路径参数**
``` java
@GetMapping("/klasses/{klassId}/teachers") 
public List<Teacher> getTeachersByClass(@PathVariable("klassId") Long klassId) { 
return teacherService.findTeachersByClass(klassId); 
}

若请求 URL 为 /klasses/123/teachers，则 klassId = 123。
```
## @RequestParam
用于绑定查询参数
``` java
@GetMapping("/klasses/{klassId}/teachers") 
public List<Teacher> getTeachersByClass(@PathVariable Long klassId, @RequestParam(value = "type", required = false) String type) { return teacherService.findTeachersByClassAndType(klassId, type); 
}
若请求 URL 为 /klasses/123/teachers?type=web，则 klassId = 123，
type = web。
```

## @Value
对于一些经常变化的参数,如网页链接,定时更新的参数,可以在配置文件中,通过@Value
注入外部配置的属性
![[JAVA/JavaWeb/SpringBoot/Attachment/image-6.png]]
缺点:繁琐,不方便复用
可以使用@ConfigurationProperties

## @ConfigurationProperties
![[idea64_8yYz94scHP.png|604x227]]

# 全局异常处理器
``` java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler{

@ExceptionHandler
public Result handleException(Exception e){
log.error("错误1");
return Result.error("出错,请联系管理员")//Result为自定义的结果类

	}
	
	public Result handleDuplicateKeyException(DuplicateKeyException e){
	log.error("错误2");
	return Result.error("出错,请联系管理员")
	//即所有DuplicateKeyException异常都会执行该方法
	
}
 
```

## @RestControllerAdvice
**开启全局异常处理器的核心**,先定义一个类,然后在**该类前写上该注解**,即可开启全局异常处理器,自动抓取项目运行时的异常

### @ExceptionHandler
1) 在本处理器的类中定义一个方法,在该方法前写上该注解,定义具体的异常信息,以及抓取到定义的异常后,进行的具体操作
2) 异常抓取的顺序按照异常类的继承关系,从小到大,**优先匹配最小子类**
![[JAVA/JavaWeb/SpringBoot/Attachment/image-9.png|241|258]]
3) 可以通过Exception及其子类自带的message方法获取控制台输出的异常信息,并通过截取字段,将异常返回给前端
![[JAVA/JavaWeb/SpringBoot/Attachment/image-10.png|644|459x252]]
# 过滤器Filter
Filter是javaweb框架自带的
使用Filter时,先定义一个子类继承自javaweb框架自带的Filter,然后在该类中定义重写Filter自带三个方法即可,一般只使用其中的doFilter方法
**每一次网络请求(Mapping),都会被拦截器拦截**进行判断是否放行
![[JAVA/JavaWeb/SpringBoot/Attachment/image-8.png]]
## @WebFilter
写在定义的Filter子类的最前面,定义拦截路径
``` java
@WebFilter(urlPatterns = "/*") //拦截所有请求
@WebFilter(urlPatterns = "/login")//拦截具体的一个路径
@WebFilter(urlPatterns = "/emps/*")//拦截emps目录下的所有路径
```

## @ServeltComponentScan必要的
**写在启动类最前面,使拦截器可以接管整个项目,自动拦截所有请求**,不写就无法成功开启拦截器
## doFilter方法
本方法用来表示拦截后进行的操作
若本方法没有具体的方法体,则只会拦截,而不进行任何操作
其中常用令牌验证然后使用filterChain.dofilter(servletRequest,servletResponse)进行放行
### 注意
1) 如果doFilter中有多个放行,那么会反复执行本次拦截及放行后的响应
``` java
@Override  
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)  
        throws IOException, ServletException {  
    log.info("拦截该请求");  
    chain.doFilter(request, response);  
   log.info("DemoFilter...完成");  
  
    log.info("放行2");  
    chain.doFilter(request, response);  
  
    log.info("放行3");  
    chain.doFilter(request, response);  
    return;   
}

```

![[idea64_N6ZMTBveF7.png]]
会造成性能浪费,所以通常只建议一个doFilter中只有一个放行

2) 如果有多个拦截器类,那么会按照拦截器的类名从A-Z依次执行,并按照先进后出的原则返回资源
![[JAVA/JavaWeb/SpringBoot/Attachment/image-11.png|612]]![[JAVA/JavaWeb/SpringBoot/Attachment/image-12.png]]

# 拦截器Interceptor
Interceptor是spring框架提供的
1) 先定义一个类继承自HandlerInterceptor,在类上加上@Component交给IOC容器管理
2) 其中有三个方法,常用的有2个
	1.  在目标资源执行前运行,返回值为true则放行,返回值为false不放行
``` java
@Override
	public boolean prehandle(HttpServiceRequest request,HttpServlerResdponse response,Object handler) throws Expections
	return true;
``` 
---------------
	2. 在目标资源执行完成后运行
``` java
	@Override
	public void postHandle(HttpServiceRequest request,HttpServlerResdponse response,Object handler)
```



2) 然后注册一个**配置类继承自WebMvcConfogurer**,来注册新定义的拦截器类以及拦截路径,在该类上加上@Configuration(@Configuration自带@Component,无需再加)
``` java
@Configuration
public class WebConfig implents WebMvcConfogurer{
@Autowired
private DemoInterceptor demoInterceptor;

@Override
public void addInterceptors(InterceptorRegistry registry){
	registry.addInterceptor(demoInterceptor)//添加拦截类,若不使用@Autowired注入,需要new DemoInterceptor()
	
	.addpathPatterns("/**")//添加拦截路径
	}
}
``` 
-------
	**常用的拦截路径**:
```java
.addPathPatterns("/*")  
//一级路径,只能匹配单一路径如/depts,/emps,不能匹配多几路径,如/depts/1,emps/list

.addPathPatterns("/**")  
//能匹配任意路径,包括单机路径/depts 和多级路径/depts/1 ,depts/1/2	
	
.addPathPatterns("/depts/*")  
//能拦截/depts下的一级路径,比如能匹配/depts/1,但是不能匹配/depts/1/2,以及/depts本身
	
.addPathPatterns("/depts/**")  
//能拦截/depts下的任意路径,比如/depts/1,depts/1/2,以及可以匹配/depts本身

```

# 对于Filter和Interceptor 的管理范围问题
Filter属于javaweb本身提供的,Interceptor是spring提供的,**Filter可以拦截项目里的所有资源,Interceptor只能拦截Spring框架里的内容**,所以若两个同时存在,会先让Filter拦截,再让Interceptor拦截,返回的时候先返回给Interceptor,再返回给Filter
![[JAVA/JavaWeb/SpringBoot/Attachment/image-13.png|618x215]]
