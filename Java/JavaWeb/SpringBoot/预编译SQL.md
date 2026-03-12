主要作用:防止静态sql导致的sql注入,性能更高

## 基本操作
1) 使用占位符?表示需要调用的sql变量值
`String sql = "INSERT INTO products(name, price) VALUES (?, ?)";`

2) 创建PreparedStatement
`PreparedStatement pstmt = connection.prepareStatement(sql);`
(会自动将特殊字符转义)

3) 设置参数（注意：索引从1开始）
`pstmt.setString(1, "john_doe");`
`pstmt.setString(2, "john@example.com");`
`pstmt.setInt(3, 25);`

## 常使用代码
#{}和${},一般使用#{},表示对象的属性名

# 错误
当一号内需要使用预编译时,比如==where e.name like **'%#{name}%'**==
此时其中的#{}识别不到
**解决办法:** 使用字符串拼接%+#{}+%
			使用MySql提供的函数 **CONCAT**
	![[JavaWeb/SpringBoot/Attachment/image-5.png]]
最终修改为
==where e.name like **CONCAT('%',#{name},'%')**==
(注意#{}是参数,不需要用单引号)