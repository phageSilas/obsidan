## 定义
Java DataBase Connectivity,使用Java语言操作数据库

## 必要前提
创建一maven项目,
引入必要依赖
 <dependency>  
    <groupId>com.mysql</groupId>  
    <artifactId>mysql-connector-j</artifactId>  
    <version>8.0.33</version>  
</dependency>


## 测试类最好使用[[try-catch-finally语句]]
## 以及[[预编译SQL]]


## DML数据操作基本过程
### 1.注册驱动
Class.forName("com.mysql.cj.jdbc.Driver");
### 2.获取数据库链接
```
String url="jdbc:mysql://localhost:3306/web01";  
String user="root";  
String password="20050715.gao";  
Connection connection =DriverManager.getConnection(url,user,password);
//DriverManager.getConnection(数据库链接,用户名,密码)用来获取数据库连接
```
### 3.获取sql语句执行对象  
```
Statement statement=connection.createStatement();
//Statement方法里有各种对象用来执行各种sql语句
```

### 4.执行sql语句
```
int i=statement.executeUpdate
("update user set age=25 where id>=1");  
System.out.println("影响记录数:"+i);
```

### 5.释放资源
```
statement.close();  
connection.close();
```

### 补充
DML方法的返回值是影响的行数

## DQL数据查询操作
### 1.User
1) java.pojo包下创建User类
2) 创建对象,其中int类型使用包装类Integer,避免由于int默认数值0导致错误
   (若不想手动写构造,可使用lombok依赖下的注解:
	@Data  
	@AllArgsConstructor  
	@NoArgsConstructor
	)

## 2.主测试函数
操作和DML类似
### 关键方法对象
结果集对象**ResultSet** :其含有方法
1) 方法next():把光标从当前位置向后移动一步,并判断当前行为是否有效行,返回值为Boolean: **true 有效,false 无**  (类似于迭代器)
2) 方法getXxx():获取数据
示例如下:
```
While(resultSet.next()){
	int i=result.getInt("id) ;
	 }
```


#