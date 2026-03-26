# AOP(SpringAOP)
## 定义及特征
AOP即Aspect Oriented Programming(面相切片编程,面向方向编程),底层为**动态代理**
场景: 案例中部分方法运行较慢,定位执耗时长的接口,此时需要统计每一个业务的执行耗时,以及记录操作日志
	**(省流:单独开一个类及方法用来执行大量重复或者需要反复修改原始业务的代码 )**
优势: 减少重复代码; 嗲吗无侵入; 提高开发效率; 维护方便 

``` java
@Slf4j  
@Aspect  
@Component  
public class RecordTimeAspect {  
  
    @Around("execution(* com.itheima.controller.*.*(..))")//匹配所有controller包下的所有方法  
    public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {  
        //1.获取方法执行前的时间  
        long begin = System.currentTimeMillis();//获取当前时间  
  
        //2.执行原始方法  
        Object result = pjp.proceed();//Object是因为原始方法可能有返回值,但返回值不确定类型,所以返回Object  
  
  
        //3.获取方法执行后的时间  
        long end = System.currentTimeMillis();//获取当前时间  
        log.info("方法执行耗时: {}ms", end - begin);  
  
        return result;  
  
    }  
}
```
## @Aspect
写在自己定义的AOP方法类(切面类)上
## 通知类型(用来匹配切入点)
### @Around
环绕通知,标注的通知方法在目标方法前,后都被执行,是常用的类型
@Around需要**自己调用ProceedingJoinPoint.proceed()** 来让原始方法运行,其他通知不需要
@Around环绕通知方法的返回值必须指定用Object接收
 
### @Before
### @After
### @AfterReturning
### @AfterThrowing
![[Attachment/image-4.png|625x206]]
## 连接点,切入点 
省略

## 切入点表达式
### @PointCut
![[Attachment/image-5.png]]
## 通知顺序
相同通知类型的方法先后和Filter顺序类似
目标方法前的通知方法:A-->Z
目标方法后的通知方法:Z-->A

## 切入点表达式
 写在通知类型的注解的括号里
### execution
``` java
excution("访问修饰符 返回值 包名.类名.方法名(方法参数) throws 异常")
其中,访问修饰符可省略,如public
	包名.类名 可省略,不过一般不这么做
	throws 异常 可省略 

```
**通配符**
1) *  :单个独立的任意符号,可以通配返回值,包名,类名,方法名,方法参数,也可以通配包,类,方法名的一部分
``` java
	 excution("* com.*.service.*.update*(*)")
	 其中,第一个*表示返回值类型,而非访问修饰符,因为返回值不可省略,访问修饰符可省略
	 
	 excution("public void com.heima.service.DeptService.update(Integer  id)")
								 |
								 |
 excution(" * com.heima.service.*.up*(*)")//up*意味着以up开头的方法(类似于模糊匹配)
 //用*意味着会通配所有当前层级的
```
2) ..  :多个连续的任意符号,可以通配任意层级的包,或任意类型,任意个数的参数
``` java
excution(* com.itheima..DeptService.*(..))

```
也可以通过逻辑表达符来限定切入点
![[Attachment/image-6.png]]

### @annotation 
@Before("@annotation (com.itheima.anno.自定义名)")
然后在目标方法上加上刚自定义的注解即可 @自定义名
``` java
@Pointcut("execution(* com.sky.mapper.*.*(..)) &&  @annotation(com.sky.annotation.AtuoFill)")

表示: 拦截mapper包下的,同时方法头上带有注解annotation的方法
```

## 连接点JoinPoint
主要用于除@Around外的那几种方法,
用于获取目标对象/目标类/目标方法/目标方法参数![[Attachment/image-7.png]]

# SpringBoot引入第三方Bean
## @Bean
通常写在配置类中@Configuration
当想使用或修改第三方只能读不能操作的类/方法时
在方法名上**写@Bean即可在本方法中实例化其他第三方类**,并把返回值交给IOC容器

- **第一步（跟 Spring 无关）：** 是**你自己**在方法里面写了 `new HuaweiCloudClient()`。因为这个第三方类**本来就在你的 Maven 依赖里**，普通的 Java 虚拟机（JVM）**完全可以读取它并把它实例化**。此时，这个对象已经被你造出来了，只不过它还在你手里。
    
- **第二步（Spring 介入）：** Spring 启动时，它不关心这个第三方 `.class` 文件长什么样。它只关心你的这个 `@Bean` 方法。Spring 会**调用你的这个方法**，然后把你 `return` 出来的那个“成品对象”一把接过去，放进自己的 IoC 容器（大仓库）里



# SpringBoot依赖自动配置
## @ComponentScan
本注解适用于第三方本地包较少的情况下
若一个项目想使用另一个项目的类/对象,
1) 先在本项目的工程文件中引入另一个项目的依赖,该依赖是maven项目文件生成时自带的,一般在工程文件开头
``` java
<groupId>com.itheima</groupId>  
<artifactId>springboot-aop-quickstart</artifactId>  
<version>0.0.1-SNAPSHOT</version>
```
 2) 在本项目的application启动类前,加上注释@ComponentScan,其格式如下
``` java
 @ComponentScan(basePackages={"另一个项目包名","本项目包名"})
 
 记上本项目包名的原因是自带的@SpringBootApplication默认检索的包就是本项目的包,
 @ComponentScan会改变默认的检索包,所以要把本项目的包加上,总共扫描两个包
 
```
**注意:** 导入第三方包后若想使用其中的类,那个类本身需要被IOC容器管理,即有个@Component注解

## @import
类似于java自身的import导入包
可以直接导入需要的那个类
![[Attachment/image-8.png]]


# Maven的依赖继承
依赖集成类似于java继承,可以单继承,多重继承,但不可多继承
## 继承操作
### 创建父工程
 一般自己项目的父工程也要继承自spring-boot-start-parent
所以先让自己的父工程继承自该工程,然后再让自己的其他模块的工程继承自父工程
``` java
主工程:

<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>3.5.8</version>  
        <relativePath/> 
</parent>

  <relativePath> <relativePath/> 是继承的父工程的相对路径,不过spring-boot-starter-parent是spring中央仓库自带的,所以不用写,让他自闭合即可
```

### 子工程继承父工程
```
在子工程中先把父工程的坐标写进<parent></parent>
```
``` java
<parent>
<groupId>com.ggg456</groupId>  
<artifactId>tlias-management-demo</artifactId>  
<version>0.0.1-SNAPSHOT</version>

</parent>

```
然后把父工程的相对位置加进去
```
<relativePath>../tilas-parent/工程.xml<relativePath/> 
../ 表示本子工程的上一级文件夹
```

即
``` java
<parent>
<groupId>com.ggg456</groupId>  
<artifactId>tlias-management-demo</artifactId>  
<version>0.0.1-SNAPSHOT</version>
<relativePath>../tilas-parent/工程.xml<relativePath/> 
</parent>
```

## 在父工程中配置各个工具的公共依赖
在父工程中的配置后的依赖,子工程中的相关依赖就可以直接删了
```
并且父工程中可以通过 <dependencyMangement></dependencyMangement>
锁定自身及子类的依赖版本
<dependencyMangement>
	<dependency>  
	    <groupId>com.aliyun.oss</groupId>  
	    <artifactId>aliyun-sdk-oss</artifactId>  
	    <version>3.17.4</version>  
	</dependency>
</dependencyMangement>

<dependency> 负责直接引入依赖
<dependencyMangement>负责管理依赖的版本


```
锁定后,就可以把子工程中依赖的</version>删掉了

## 在子工程中配置各自单独的依赖
对于不同于主工程的依赖,一子工程中的版本为准

## 最后
确定继承关系后,子工程中的依赖belike:
``` java
<dependency>  
		<groupId>com.aliyun.oss</groupId>
	    <artifactId>aliyun-sdk-oss</artifactId>  
</dependency>

其中
 <groupId>com.aliyun.oss</groupId> 父类中写了,可以省略,但不推荐
<version>3.17.4</version> 父类中版本锁定了,可以省略

```

# Maven的模块聚合
分模块拆分后的项目,若想打包package,需要按照依赖调用的顺序依次对各模块install,显然麻烦,因此需要**聚合**,将多个模块组成一个整体

## 聚合工程
聚合工程是不具备具体业务的空工程,其中只有pom文件,所以一般来说,
**聚合工程和最大的父工程是同一个工程**
![[Attachment/image-9.png|519x335]]

## 操作
在聚合工程(父工程)中,使用`<moudle>`入子工程

```  java
<modules>
    <module>../tilas-pojo</module>
    <module>../tilas-pojo</module>
    <module>../tilas-pojo</module>
</modules>
```



# 事务@Transactional
## 使用类型
- **方法**：推荐将注解使用于方法上，不过需要注意的是：**该注解只能应用到 public 方法上，否则不生效。**
- **类**：如果这个注解使用在类上的话，表明该注解对该类中所有的 public 方法都生效。
- **接口**：不推荐在接口上使用。
- 

---
## 



# Spring Cache

## @EnableCaching
加在启动类上.用以启动缓存注解

## @Cacheable (动态代理)
写在方法前,在方法执行前查询缓存中是否有数据,如果有,则直接返回缓存数据;若没有,调用方法并将方法返回值放到缓存中

``` java
@Cacheable(cacheNames="userCache",key="#id")
public User getById(long id){
User user = usermapper.getById(id)
}

其中,
1.cacheNames写要调用的redis的key,
若redis中有,则跳过方法中的查询数据库方法(代理方法),直接调用redis中的数据
若没有则正常执行方法,并在执行方法时,会通过反射创建出这条数据
 
2.key后只写形参,不能写result
```


==注意:==  不要将本注解应用于新增/修改/删除操作,因为在运行时,本注解会使程序直接跳过数据库步骤(只读取缓存数据),不会有任何改变
## @Cacheput
写在方法前,将返回值放到缓存中

``` java
@Cacheput(cacheNames="userCache",key="#user.id")
public User save(@RequestBody User user)
{
userMapper.insert(user);
return user;
}

其中,cacheName随意写

#xx是spring一种语法的固定格式,
1. 后边写形参,表示从传进来的对象里获取;
2. 后边写result,表示从返回值中获取
3. p0,表示第一个形参,p1表示第二个形参
4. a0, root.args[0], 效果同3,都是第1个形参
然后就可以动态计算.id的值
```
以上输出后,redis中会显示 userCache::id(在redis中:表示分级,最终表示在userCache文件夹下的`[Empty]` 文件夹下的"userCache::id("),因为两个冒号分了两级

## @CacheEvict (动态代理)
写在方法前,将一条或者多条数据从缓存中删除
``` java
@CacheEvict(cacheNames="userCache",key="#id")
public User save(@RequestBody User user)
{
userMapper.insert(user);
return user;
}


```
方法结束后,删除对应的Redis数据,
@CacheEvict(cacheNames="userCache",allEntries = true)表示删除userCache文件夹下的全部数据

**注意:** 一般修改,删除数据都需要清除全部缓存,因为其关联的数据太多,折腾修改不如直接全删