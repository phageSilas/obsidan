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
![[image-4.png|625x206]]
## 连接点,切入点 
省略

## 切入点表达式
### @PointCut
![[image-5.png]]
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
![[image-6.png]]

### @annotation 
@Before("@annotation (com.itheima.anno.自定义名)")
然后在目标方法上加上刚自定义的注解即可 @自定义名

## 连接点JoinPoint
主要用于除@Around外的那几种方法,
用于获取目标对象/目标类/目标方法/目标方法参数![[image-7.png]]

# SpringBoot引入第三方Bean
## @Bean




# SpringBoot依赖自动配置
## @ComponentScan
本注解适用于第三方本地包较少的情况下
若一个项目想使用另一个项目的类/对象,
1) 先在本项目的pom文件中引入另一个项目的依赖,该依赖是maven项目文件生成时自带的,一般在pom文件开头
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
![[image-8.png]]


# Maven的依赖继承
依赖集成类似于java继承,可以单继承,多重继承,但不可多继承
## 继承操作
### 创建父pom
 一般自己项目的父pom也要继承自spring-boot-start-parent
所以先让自己的父pom继承自该pom,然后再让自己的其他模块的pom继承自父pom
``` java
主pom:

<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>3.5.8</version>  
        <relativePath/> 
</parent>

  <relativePath> <relativePath/> 是继承的父pom的相对路径,不过spring-boot-starter-parent是spring中央仓库自带的,所以不用写,让他自闭合即可
```

### 子pom继承父pom
```
在子pom中先把父pom的坐标写进<parent></parent>
```
``` java
<parent>
<groupId>com.ggg456</groupId>  
<artifactId>tlias-management-demo</artifactId>  
<version>0.0.1-SNAPSHOT</version>

</parent>

```
然后把父pom的相对位置加进去
```
<relativePath>../tilas-parent/pom.xml<relativePath/> 
../ 表示本子pom的上一级文件夹
```

即
``` java
<parent>
<groupId>com.ggg456</groupId>  
<artifactId>tlias-management-demo</artifactId>  
<version>0.0.1-SNAPSHOT</version>
<relativePath>../tilas-parent/pom.xml<relativePath/> 
</parent>
```

## 在父pom中配置各个工具的依赖
在父pom中的配置后的依赖,子pom中的相关依赖就可以直接删了
