## @Mapper
@Repository 是 Spring 的原生注解，而 `@Mapper` 是 MyBatis 的注解。@Repository通用，适用于 JPA, Hibernate, JDBC 等所有 DAO 层实现。
@Mapper仅用于 MyBatis 的 Mapper 接口。

不同点：
1、@Mapper不需要配置扫描地址，可以单独使用，如果有多个mapper文件的话，可以在项目启动类中加入@MapperScan(“mapper文件所在包”)
2、@Repository不可以单独使用，否则会报错误，要想用，必须配置扫描地址（@MapperScannerConfigurer）

### @Select
### @Insert
### @Update
### @Delete

### @Param
当 Mapper 接口方法有多个参数时，MyBatis 无法识别哪个是哪个，必须用它标记。
``` java
// SQL 中使用 #{username} 和 #{password}
User login(@Param("username") String name, @Param("password") String pwd);
```

### @Options
