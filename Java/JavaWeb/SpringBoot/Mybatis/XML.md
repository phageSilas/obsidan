# 使用XML时,记得将注解去掉
# 概念:
MyBatis中,既可以通过注解生成sql语句,也可以配置XML文件配置
简单的使用注解,复杂的使用XML
 
# 基本操作
## 创建
resource文件夹下创建"目录"--**"com/接口的包名/对应接口名"**,
注意:不要用"."连接,要用"/",这样才能和包名一个效果
![[JavaWeb/SpringBoot/Attachment/image-1.png]]
补充:若真的没有按照要求命名,可以在application.properties中配置
	 xml映射文件的位置
## 基本配置

```java
<?xml version="1.0" encoding="UTF-8" ?>  
<!DOCTYPE mapper  
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"  
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">  
<mapper namespace="com.ggg456.mapper.UserMapper">  
  
<!--  此处插入sql代码-->
  
</mapper>
```
mapper namespace后接接口的"引用",与接口名一致

# 简单示例
```java
<select id="aaa" resultType="com.ggg456.pojo.User">
<!--"aaa"是此后调用该查询语句的方法名,调用时名字一定要相同-->
<!--resultType:返回值类型,返回的单条记录往哪放-->  
<!--select每次返回的都是User属性,因此往User类中放-->
        select * from user  
</select>
```

