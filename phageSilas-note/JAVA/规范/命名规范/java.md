| **类型**             | **命名风格**                 | **规范描述**                | **示例**                                        |
| ------------------ | ------------------------ | ----------------------- | --------------------------------------------- |
| **类 (Class) / 接口** | **大驼峰 (PascalCase)**     | 首字母大写，后续每个单词首字母大写。      | `User`, `StudentInfo`, `PaymentService`       |
| **方法 (Method)**    | **小驼峰 (lowerCamelCase)** | 首字母小写，后续每个单词首字母大写。动词开头。 | `getUserName()`, `calculateTotal()`, `save()` |
| **变量 (Variable)**  | **小驼峰 (lowerCamelCase)** | 首字母小写，后续每个单词首字母大写。      | `userId`, `firstName`, `customerList`         |
| **常量 (Constant)**  | **全大写 + 下划线**            | 全部字母大写，单词之间用下划线分隔。      | `MAX_COUNT`, `DEFAULT_TIMEOUT`, `PI`          |
| **包 (Package)**    | **全小写**                  | 全部小写，通常使用点分隔符，避免下划线。    | `com.google.common`, `java.util`              |

### Java 与 SQL 的映射 (Mapping)

在实际开发中（如使用 MyBatis 或 JPA/Hibernate），需要将 Java 的“驼峰”与 数据库的“下划线”对应起来

|**概念**|**Java 代码 (驼峰)**|**SQL 数据库 (下划线)**|
|---|---|---|
|**类 / 表**|`UserInfo`|`user_info`|
|**属性 / 字段**|`userId`|`user_id`|
|**属性 / 字段**|`createTime`|`create_time`|
|**属性 / 字段**|`homeAddress`|`home_address`|
