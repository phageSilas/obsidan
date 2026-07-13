Hutool 是 Java 开发里很常用的国产工具库，常见工具类可以按场景理解：

**字符串工具**

`StrUtil`  
最常用之一，用于字符串判空、格式化、截取、替换、驼峰转换等。

```
StrUtil.isBlank(name);
StrUtil.format("Hello, {}", username);
StrUtil.toUnderlineCase("userName"); // user_name
```

**集合工具**

`CollUtil`  
用于集合判空、创建集合、交集、并集、差集等。

```
CollUtil.isEmpty(list);
CollUtil.newArrayList("Java", "Spring", "MySQL");
CollUtil.intersection(list1, list2);
```

**对象工具**

`ObjectUtil`  
用于对象判空、默认值、比较等。

```
ObjectUtil.isNull(user);
ObjectUtil.defaultIfNull(value, "默认值");
```

**Bean 工具**

`BeanUtil`  
常用于对象属性拷贝、Map 和 Bean 互转。

```
UserDTO dto = BeanUtil.copyProperties(userDO, UserDTO.class);
Map<String, Object> map = BeanUtil.beanToMap(user);
```

在 DTO、VO、DO 转换中很常见，但复杂映射建议用 MapStruct。

**日期时间工具**

`DateUtil`  
用于日期格式化、解析、偏移、开始/结束时间计算。

```
DateUtil.now();
DateUtil.parse("2026-07-13");
DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
DateUtil.beginOfDay(new Date());
DateUtil.offsetDay(new Date(), 7);
```

**JSON 工具**

`JSONUtil`  
用于对象转 JSON、JSON 转对象、读取字段等。

```
String json = JSONUtil.toJsonStr(user);
User user = JSONUtil.toBean(json, User.class);
```

如果项目已经使用 Jackson 或 Fastjson，建议统一一种 JSON 库，避免序列化行为不一致。

**加密摘要工具**

`DigestUtil`  
常用于 MD5、SHA256 等摘要计算。

```
DigestUtil.md5Hex("123456");
DigestUtil.sha256Hex("hello");
```

注意：密码存储不要直接用 MD5，应该使用 BCrypt、Argon2 等专门的密码哈希算法。

**UUID / ID 工具**

`IdUtil`  
用于生成 UUID、雪花 ID 等。

```
IdUtil.fastSimpleUUID();
IdUtil.getSnowflakeNextId();
```

分布式系统里雪花 ID 很常用，但要注意机器 ID、时钟回拨等问题。

**文件工具**

`FileUtil`  
用于读写文件、创建目录、复制、删除等。

```
FileUtil.readUtf8String(file);
FileUtil.writeUtf8String("内容", file);
FileUtil.mkdir("logs");
```

**IO 工具**

`IoUtil`  
用于流复制、关闭流、读取流内容。

```
IoUtil.copy(inputStream, outputStream);
IoUtil.close(inputStream);
```

**HTTP 工具**

`HttpUtil` / `HttpRequest`  
用于发送 GET、POST 请求。

```
String result = HttpUtil.get("https://example.com");

String body = HttpRequest.post(url)
        .body(JSONUtil.toJsonStr(param))
        .execute()
        .body();
```

简单调用很方便；复杂项目中更推荐统一使用 Spring `RestClient`、OpenFeign、OkHttp 等。

**校验工具**

`Validator`  
用于邮箱、手机号、身份证、URL 等格式校验。

```
Validator.isEmail("test@example.com");
Validator.isMobile("13800138000");
```

**类型转换工具**

`Convert`  
用于字符串、数字、日期、集合等类型转换。

```
Integer age = Convert.toInt("18");
Date date = Convert.toDate("2026-07-13");
List<String> list = Convert.toList(String.class, array);
```

实际项目里，Hutool 最常用的通常是：`StrUtil`、`CollUtil`、`ObjectUtil`、`BeanUtil`、`DateUtil`、`JSONUtil`、`IdUtil`、`DigestUtil`、`FileUtil`。  
它很适合提升开发效率，但在大型项目中建议注意“工具库边界”：JSON、HTTP、对象映射、加密这些能力最好和团队技术栈保持统一。