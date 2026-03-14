## 概念
DAO持久层框架,可省略大量代码
**SpringBoot是基础框架和容器，MyBatis专注于和数据库打交道**

## 准备工作
1) SpringBoot工程,引入MyBatis依赖
2) 配置文件application.properties中配置数据库连接信息: ![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-12.png]]
配置文件的作用等同于JDBC中的[[JDBC#1.注册驱动]]和[[JDBC#2.获取数据库链接]]
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-14.png]]

## 具体操作
创建一个Mapper包(同之前的DAO),下面创建各对象的Mapper接口,如UserMapper,
接口上引入注解@Mapper,该注解会在调用接口时自动为该接口创建实现类对象,并自动存入SpringBoot的IOC仓库(即不需要写注解@Component)


### 查询 @Select
格式: `@Select("查询语句")`
	  `public 对应方法名()`
	   ^03deb2


## @Param:为接口中方法的形参起名字
首先介绍默认顺序:当 Mapper 方法有多个参数时，MyBatis 无法自动识别参数名。
```java
//下面方法位于接口中
@Select("Select * from user where id=#[id],age=#[age],password=#[password]")
public void sel(int id,int age,int password)
//默认顺序是从左至右一一对应的,无论是否对照
```
使用注解@param可以自定义名字,防止顺序出错时参数错误
下面的删改插操作只有一个形参,所以无需担心顺序出错
![[image-23.png|647x197]]



### 删除 @Delete
```java
格式:@Delete("delete from 表 where aa=#{aa}")
		public  Integer 方法名(数据类型 aa) 
```
其中,#{}为占位符,作用同[[预编译SQL]]中的占位符?相同,用以方便改变要控制的属性
使用Integer是因为DML方法的返回值是影响的行数


### 插入@Insert
```java
格式:
 @Insert("insert into user(username,password,name,age) 
		 values(#{username},#{password},#{name},#{age})")//id为自增主键,不需要手动设置  
public Integer insert(User user);// 返回受影响的行数
```

## ==修改@Update==
### 情景一:根据特定参数范围修改特定数据
 格式:
```java
@Update("update user set age=#{age} where id=#{id}")  
public Integer update(User user);// 返回受影响的行数
```
这里sql中只要求根据id修改年龄
那么测试的时候也可以通过无参单独设置
```java
@Test  
public void UUUUpdate(){  
    User user=new User();  
    user.setId(8);//要修改属性对应的id值  
    user.setAge(125);//属性修改后的值  
    Integer i=userMapper.update(user);  
    System.out.println("影响的行数为"+i);  
}
```
**若想要在括号中直接写参数,则需要将对应的5项参数全部写上**![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-17.png|363x164]]
但是,由于我们只要求根据id修改age,所以username,password,name等无关参数可以随便写, MyBatis只会从User对象中提取 id和 age 属性的值,结果和上面一样
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-18.png]]


### 情景二:涉及多个参数
格式:
```java
@Update("update user set username=#{username},password=#{password},name=#{name},age=#{age} where id=#{id}")  
public Integer update(User user);// 返回受影响的行数
```
这里要求根据id修改同行的所有参数,若仍像之前一样只写两个,那么修改后的其他参数将更新为null
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-19.png]]
**若没有提供具体数值的列不允许为null,修改时仍不给它赋值,则会报错**
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-20.png]]


当然,正常全部修改的话就是正常修改(![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-21.png]]

如果使用有参修改也可以
![[image-22.png]]





## 测试
### 查询测试
在main/test/java/下的测试文件中调用[[SpringBoot测试类#]]
将查询的对象存入集合
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-13.png]]
**注意**:若输出地址,这是lombok出问题了,需要重写toSring方法

### 删除测试
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-15.png]]
**注意**:DML方法的返回值是影响的行数

## 插入测试
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-16.png|583x124]]
注意:在括号内填写需要插入的各项数据,属性位置一一对应

# 注意:注解和[[XML]]二选一