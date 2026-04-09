# 1. 对象属性拷贝BeanUtils.copyProperties
## 使用场景
承接自上一条建造者模式,若两个类有较多的相同属性,如用户属性类和它对应的DTO类,可以通过Spring提供的该工具类实现
## 使用方法
``` java
// 1. 先 new 一个空的目标对象
Employee employee = new Employee();

// 2. 调用工具类进行拷贝 (源对象, 目标对象)
BeanUtils.copyProperties(employeeDTO, employee);
//会自动把 employeeDTO里的值全部塞进 employee 里。左➡️右
```
若是把属性更多的类赋值给属性更少的类,代码会正常运行，多出来的属性会被直接“无视”掉（静默忽略）


# 2. Bean 对象转换为 Map<String, Object> 结构BeanUtil.beanToMap

BeanUtil.beanToMap 是 Hutool 工具包中的一个方法
**主要用途**
1) 将对象属性转为键值对：把 Bean 的所有字段名作为 key，字段值作为 value
2) 方便数据存储：特别适合存储到 Redis Hash、MongoDB 等键值型数据库
3) 数据传输和序列化：在不同系统间传递数据时使用

``` java
// 示例 1: 基本转换
UserDTO user = new UserDTO();
user.setId(1L);
user.setPhone("13800138000");
user.setNickName("张三");

Map<String, Object> map = BeanUtil.beanToMap(user);
// 结果: {id=1, phone="13800138000", nickName="张三"}

// 示例 2: 驼峰转下划线 + 忽略 null 值
Map<String, Object> map2 = BeanUtil.beanToMap(user, true, true);
// 结果: {id=1, phone="13800138000", nick_name="张三"}


```

**注意事项**
1) 只会转换有 getter 方法的属性
2) 默认保留 null 值（除非指定 ignoreNullValue=true）
3) 默认保持原字段命名（除非指定 isToUnderlineCase=true 转为下划线）
4) 静态方法可以直接调用，无需创建实例

#  将Map 中的数据填充到 Java Bean 对象中BeanUtil.fillBeanWithMap
也是 Hutool 工具包中的方法，它的作用与 beanToMap 正好相反

**主要用途**
1) Map 转对象：将键值对数据还原为 Java Bean
2) Redis 数据还原：从 Redis Hash 读取的 Map 转为业务对象
3) 表单数据封装：将前端传来的参数 Map 封装为对象

``` java
// 示例 1: 基本转换
Map<String, Object> map = new HashMap<>();
map.put("id", 1L);
map.put("phone", "13800138000");
map.put("nickName", "张三");

UserDTO user = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
// 结果: user.getId()=1, user.getPhone()="13800138000", user.getNickName()="张三"

// 示例 2: 支持下划线转驼峰
Map<String, Object> map2 = new HashMap<>();
map2.put("user_id", 1L);
map2.put("user_name", "李四");

UserDTO user2 = BeanUtil.fillBeanWithMap(map2, new UserDTO(), true);
// 结果: user.getUserId()=1, user.getUserName()="李四"

```

## 与toBean的区别

| fillBeanWithMap | 填充已有对象 | 需要复用对象实例时 |
| --------------- | ------ | --------- |
| **toBean**      | 创建新对象  | 直接创建新对象时  |
``` java
// fillBeanWithMap - 填充已有对象
UserDTO existingUser = new UserDTO();
BeanUtil.fillBeanWithMap(map, existingUser, false);

// toBean - 创建新对象（更常用）
UserDTO newUser = BeanUtil.toBean(map, UserDTO.class);

```

# BooleanUtil.isTrue(isLock) 
等效于**Boolean.TRUE.equals(isLock)**
**作用:** 
``` java
检查 Boolean 值是否为 true
   BooleanUtil.isTrue(Boolean.TRUE)  = true
   BooleanUtil.isTrue(Boolean.FALSE) = false
   BooleanUtil.isTrue(null)          = false
 
形参:
bool – 被检查的Boolean值
返回值:
当值为true且非null时返回true
```
防止拆箱时拆到null抛出 `NullPointerException` (空指针异常)(原因见[[常见错误-异常处理#2. 自动拆箱的致命陷阱：空指针异常 (NPE)]])

