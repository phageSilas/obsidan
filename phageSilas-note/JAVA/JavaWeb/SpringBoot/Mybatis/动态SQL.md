# 定义
随着用户的输入或外部条件的变化而变化的sql语句
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
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-11.png|641]]

```

<if>:判断条件是否成立,如果结果为true,则拼接sql
<where>: 根据查询条件,生成where关键字,并自动去除条件前面多余的and或or
```

## foreach
![[JAVA/JavaWeb/SpringBoot/Mybatis/Attachment/image-21.png|651x338]]
使用
