BeanUtil来源自HuTool,BeanUtils来源自Spring
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


# 2.BeanUtil.toBean(效果类似于copyProperties)
没问题，我们来系统地认识一下 Hutool 工具包中非常高频使用的 `BeanUtil.toBean` 方法。

### 1. 它是做什么的？

在 Java 开发中，我们经常需要在不同的数据模型之间转换数据。例如，将从数据库查出来的实体类（Entity/DO）转换为返回给前端的视图对象（VO/DTO）。

如果手动写 `userDTO.setName(user.getName())`，当属性有几十个时，代码会极其臃肿。`BeanUtil.toBean` 就是为了解决这个问题而诞生的。它的核心作用是：**根据源对象，自动实例化一个新的目标对象，并将源对象中同名的属性值拷贝过去。**

### 2. 基本用法与代码示例

这个方法的标准签名通常是：`public static <T> T toBean(Object source, Class<T> clazz)`。

**示例：**

假设我们有一个 `User` 实体类和一个 `UserDTO` 数据传输类。
``` Java
// 源对象（例如从数据库查询出来的实体）
User user = new User();
user.setId(1L);
user.setUsername("zhangsan");
user.setAge(25);

// 使用 BeanUtil.toBean 直接生成新对象并完成赋值
UserDTO userDTO = BeanUtil.toBean(user, UserDTO.class);

// 此时 userDTO 已经是一个新实例，并且 id, username, age 都与 user 相同
```

### 3. 核心特性与底层逻辑

要完全掌握这个方法，需要了解它的一些关键特性：

- **实例化与赋值一体化：** 你不需要手动 `new UserDTO()`。只要你传入了 `UserDTO.class`，Hutool 会通过反射调用该类的无参构造方法帮你 `new` 出一个新对象，然后再进行属性填充。
    
- **基于反射的名称匹配：** 它的底层依然依赖 Java 的反射机制（通过获取 `getter/setter` 方法或直接操作字段）。它会在源对象和目标对象中寻找**名称相同**的属性进行拷贝。如果名称匹配不上，该属性就会被忽略（目标对象中该属性为 `null`）。
    
- **强大的自动类型转换：** 这是 Hutool 相比于 Spring 的 `BeanUtils` 最亮眼的特性之一。如果源对象的一个属性是 `Integer` 类型，而目标对象同名属性是 `String` 类型，Hutool 会**自动帮你完成类型转换**（比如把 `25` 转换成 `"25"`）。
    
- **浅拷贝（极其重要）：** `BeanUtil.toBean` 默认执行的是**浅拷贝（Shallow Copy）**。
    
    - 如果属性是基本数据类型（如 `int`, `boolean`）或 `String`，它会直接复制值。
        
    - 如果属性是**引用类型**（如另一个对象 `Role`、`List` 集合等），它拷贝的是**内存地址的引用**。这意味着，修改新对象中的嵌套对象，会同时改变源对象中的嵌套对象。
        

### 4. 与 `copyProperties` 的区别

Hutool 中还有一个类似的方法叫 `BeanUtil.copyProperties`，很多开发者容易混淆：

| **方法**                                    | **目标对象的提供方式**           | **适用场景**                         |
| ----------------------------------------- | ----------------------- | -------------------------------- |
| `BeanUtil.toBean(source, Target.class)`   | **自动创建**（传入 Class 类型即可） | 需要根据现有数据**凭空生成一个全新对象**时。         |
| `BeanUtil.copyProperties(source, target)` | **手动提供**（必须传入已实例化的对象）   | 目标对象已经存在，只需要**将源数据覆盖/追加**到目标对象时。 |

### 5. 进阶使用：CopyOptions

为了应对复杂的业务场景，`toBean` 还提供了一个重载方法，允许传入 `CopyOptions` 来定制拷贝规则。这在你之前遇到的“脱敏屏蔽”或特殊转换中非常有用：
``` Java
UserDTO userDTO = BeanUtil.toBean(user, UserDTO.class, 
    CopyOptions.create()
        .ignoreNullValue() // 忽略源对象中的 null 值，不覆盖目标对象
        .setIgnoreProperties("password", "phone") // 强制忽略某些属性（比如避免把密码拷贝过去）
        .setFieldMapping(MapUtil.builder("realName", "name").build()) // 字段名不一致时的映射
);
```

总结来说，`BeanUtil.toBean` 是一个极其方便的“内存对象复制机”。但正因为它只负责老老实实地把内存里的数据从 A 搬运到 B，所以它不会理会任何像 `@JsonSerialize` 这种属于“JSON 序列化表现层”的注解。

### 使用场景一: 获取未脱敏的真实信息
``` java
return  Results.success(BeanUtil.toBean(userService.getUserInfo(username), UserActualRespDTO.class));
```
#### `@JsonSerialize` 仅仅是在“离开后端”时才生效

`@JsonSerialize`（通常来自 Jackson 库）是一个**序列化层**的注解。它**不会改变 Java 对象在内存中的真实值**。

- 当你调用 `userService.getUserInfo(username)` 时，从数据库查出来的是什么，这个对象在 JVM 内存里存的就是什么（也就是**未脱敏的明文**）。
    
- 只有当这个对象被 Controller 层返回给前端，SpringBoot 调用 Jackson 库将 Java 对象**转换成 JSON 字符串**的那一瞬间，`@JsonSerialize` 指定的脱敏逻辑才会执行。
    

#### `BeanUtil.toBean` 是基于内存的物理拷贝

Hutool 的 `BeanUtil.toBean(source, targetClass)` 是一个**内存级别**的属性拷贝工具。

- 它的底层原理是通过 Java 的**反射机制**（Reflection），调用原对象的 `getter` 方法获取值，然后调用目标对象的 `setter` 方法赋值。
    
- 在拷贝的过程中，`BeanUtil` 根本不知道也不关心 `@JsonSerialize` 注解的存在。它拿到的直接是内存里未经任何处理的原始明文数据。
    

**流程对比图：**

- **正常返回：** 原始对象(明文) -> Controller -> Jackson触发 `@JsonSerialize` -> 脱敏后的JSON字符串 -> 前端
    
- **使用 BeanUtil：** 原始对象(明文) -> `BeanUtil.toBean` -> **新对象(明文)** -> Controller -> (如果新类没有注解)转为JSON -> 未脱敏JSON -> 前端

# 3. Bean 对象转换为 Map<String, Object> 结构BeanUtil.beanToMap

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

#  3. 将Map 中的数据填充到 Java Bean 对象中BeanUtil.fillBeanWithMap
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

# 4. BooleanUtil.isTrue(isLock) 
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
**例子**
``` java
@Override  
public boolean tryLock(Long timeoutSec) {  
    // 1.获取线程标识  
    Long threadId = Thread.currentThread().getId();//获取当前线程的ID  
  
    //2.尝试获取锁  
    Boolean isLock = stringRedisTemplate.opsForValue()  
            .setIfAbsent(KEY_PREFIX + name, threadId+"", timeoutSec, TimeUnit.SECONDS);  
  
    return BooleanUtil.isTrue(isLock);//判断是否获取锁成功,如果是1，则返回true,0或者null都是返回false  
}
```

