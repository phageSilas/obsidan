# 作用
随着用户的输入或外部条件的变化而变化的sql语句
防止因某一个参数出错或为null而无法正常触发查询
# 基本使用(默认在xml文件中)
## if 和 where

使用
``` java
<where>
	<if 判断条件>
	 拼接的sql语句,一般以and/or开头
	 </if>
</where>
```
其中
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-11.png|538]]

```

<if>:判断条件是否成立,如果结果为true,则拼接sql
<where>: 根据查询条件,生成where关键字,并自动去除条件前面多余的and或or
```

## foreach
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-21.png|544|538]]
``` java
void batchInsertUsers( List<User> users);
```

``` xml
<insert id="batchInsertUsers">
    INSERT INTO user (name, age, status) 
    VALUES 
    <foreach collection="users" item="user" separator=",">
        (#{user.name}, #{user.age}, #{user.status})
    </foreach>
</insert>

```


``` SQL
INSERT INTO user (name, age, status) VALUES ('张三', 20, 1), ('李四', 25, 0), ('王五', 22, 1)
```

其中,**collection在现代jdk中可以使用对象名**,一般情况的默认如下
- 如果参数是 `List` 类型，MyBatis 会自动给它起个别名叫 **`"list"`**。
- 如果参数是数组类型（比如 `User[]`），MyBatis 会自动给它起个别名叫 **`"array"`**。
- 如果参数是普通的 `Collection`（比如 `Set`），别名就是 **`"collection"`**。

另外,也可以通过在接口中以@Param来定义c ollection名字
``` java
List<User> selectByIds(@Param("idList") List<Long> ids);
```
这样collection就是idList了