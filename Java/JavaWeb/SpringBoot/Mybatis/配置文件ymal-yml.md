# 小项目用properties,大项目用yml

# 格式
1. **缩进**：使用空格（通常是2个或4个），不要使用Tab
2. **大小写敏感**
3. **注释**：使用 `#` 开头
4. **字符串引号**：特殊字符需要引号
![[JavaWeb/SpringBoot/Attachment/image-2.png|491x280]]
# 常用配置
#### 1. 开启驼峰命名映射 (`map-underscore-to-camel-case`)

这是最重要的配置，它解决了“数据库下划线”与“Java 驼峰”自动对应的问题。
前提:两边命名均符合规范
- **作用：** 自动将数据库的 `user_name` 映射为 Java 的 `userName`。如果不开启，查出来的字段全是 `null`。
    
#### 2. 打印 SQL 日志 (`log-impl`)

开发阶段的神器。

- **作用：** 在控制台打印出 MyBatis 实际执行的 SQL 语句和参数。
    
- **常用值：** `org.apache.ibatis.logging.stdout.StdOutImpl` (直接打印到标准控制台)。
    
#### 3. 实体类包别名 (`type-aliases-package`)

- **作用：** 在写 Mapper XML 文件时，`resultType` 不用写全限定名（如 `com.example.project.pojo.User`），只需要写类名（`User`）。
    
- **设置：** 指定你的实体类（POJO）所在的包路径。
    
#### 4. Mapper XML 文件位置 (`mapper-locations`)

- **作用：** 告诉 MyBatis 去哪里找写 SQL 的 `.xml` 文件。
    
- **常见路径：** 通常放在 `resources/mapper` 目录下。