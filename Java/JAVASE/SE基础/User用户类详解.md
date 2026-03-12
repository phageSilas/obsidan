
## 1. **User类的完整结构**
一般包括私有属性,无参构造,有参构造,get方法,set方法这五项
其他情况可以根据需要添加入toString()之类的重写方法
```java
// 典型的完整User类
public class User {
    // 1. 私有属性定义
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    
    // 2. 无参构造方法
    public User() {
        // 初始化默认值
    }
    
    // 3. 有参构造方法
    public User(Integer id, String username, String password, String name, Integer age) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
    }
    
    // 4. Getter方法
    public Integer getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getName() {
        return name;
    }
    
    public Integer getAge() {
        return age;
    }
    
    // 5. Setter方法
    public void setId(Integer id) {
        this.id = id;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
}

```
## 2. **各部分的作用和必要性**

### 第一部分：私有属性定义

```java
private Integer id;
private String username;
private String password;
private String name;
private Integer age;

```
**为什么需要：**

1. **数据存储**：对象的核心，存储用户的具体信息
    
2. **封装性**：私有(private)修饰符保护数据不被直接访问
    
3. **数据完整性**：定义User对象应该包含哪些数据
    

**在其他类中使用：**

```java
// 错误：不能直接访问私有属性
User user = new User();
// user.id = 1;  // ❌ 编译错误，id是私有的

// 必须通过公开的方法访问
user.setId(1);  // ✅ 通过setter方法
int id = user.getId();  // ✅ 通过getter方法
```

### 第二部分：无参构造方法

```java

public User() {
    // 空构造方法
}

```
**为什么需要：**

1. **框架兼容性**：MyBatis、Spring、Jackson等框架都依赖无参构造创建对象
    
2. **反射创建**：Java反射机制需要无参构造来实例化对象
    
3. **灵活性**：先创建对象，再逐步设置属性
    

**在其他类中使用：**

```java
// 场景1：从数据库查询（MyBatis使用）
User user = userMapper.selectById(1);
// MyBatis内部：
// 1. 调用无参构造：User user = new User();
// 2. 反射调用setter设置属性值

// 场景2：从JSON反序列化（Jackson使用）
String json = "{\"id\":1,\"name\":\"张三\"}";
ObjectMapper mapper = new ObjectMapper();
User user = mapper.readValue(json, User.class);
// Jackson内部也需要无参构造创建对象

```
### 第三部分：有参构造方法

```java
public User(Integer id, String username, String password, String name, Integer age) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.name = name;
    this.age = age;
}
```

**为什么需要：**

1. **便捷初始化**：创建对象时一次性设置所有属性
    
2. **保证完整性**：确保创建的对象就是完整的，不会有null值
    
3. **代码简洁**：减少setter调用的代码量
    

**在其他类中使用：**

```java
// 创建完整对象
User user1 = new User(1, "zhangsan", "123456", "张三", 25);

// 对比：无参构造+setter需要5行代码
User user2 = new User();
user2.setId(2);
user2.setUsername("lisi");
user2.setPassword("654321");
user2.setName("李四");
user2.setAge(30);

// 批量创建对象
List<User> users = Arrays.asList(
    new User(1, "user1", "pass1", "用户1", 20),
    new User(2, "user2", "pass2", "用户2", 25),
    new User(3, "user3", "pass3", "用户3", 30)
);

```
### 第四部分：Getter方法

```java
public Integer getId() {
    return id;
}

public String getUsername() {
    return username;
}

// ... 其他getter

```
**为什么需要：**

1. **读取数据**：外部类需要获取对象的属性值
    
2. **控制访问**：可以在getter中添加逻辑（如格式化、权限检查）
    
3. **兼容性**：JavaBean规范要求属性必须有getter方法
    

**在其他类中使用：**

```java
// 业务逻辑中读取数据
public class UserService {
    public void processUser(User user) {
        // 获取用户信息
        String name = user.getName();  // 通过getter获取
        Integer age = user.getAge();
        
        // 使用数据
        if (age >= 18) {
            System.out.println(name + "是成年人");
        }
        
        // 设置其他业务逻辑
        updateUserInfo(user);
    }
    
    private void updateUserInfo(User user) {
        // 访问user的属性来做更新逻辑
        if (user.getId() != null) {
            // 根据id更新
        }
    }
}
```

### 第五部分：Setter方法

```java
public void setId(Integer id) {
    this.id = id;
}

public void setUsername(String username) {
    this.username = username;
}

// ... 其他setter
```

**为什么需要：**

1. **修改数据**：外部类需要修改对象的属性
    
2. **数据验证**：可以在setter中验证数据的有效性
    
3. **灵活性**：可以动态修改对象的任何属性
    

**在其他类中使用：**

```java
public class UserController {
    public void updateUserAge(int userId, int newAge) {
        // 1. 查询用户
        User user = userDao.findById(userId);
        
        // 2. 修改年龄
        user.setAge(newAge);  // 调用setter修改属性
        
        // 3. 保存更新
        userDao.update(user);
    }
    
    // 表单数据绑定
    public void createUser(RegistrationForm form) {
        User user = new User();
        user.setUsername(form.getUsername());  // 逐个设置属性
        user.setPassword(form.getPassword());
        user.setName(form.getRealName());
        user.setAge(form.getAge());
        
        userService.save(user);
    }
}
```

## 4. **为什么这5项缺一不可**

| 组成部分         | 缺失的后果          | 具体问题                       |
| ------------ | -------------- | -------------------------- |
| **私有属性**     | 无法存储数据，不是完整的对象 | 对象没有状态，无法表示真实用户            |
| **无参构造**     | 框架无法正常工作       | MyBatis、Spring、Jackson等会报错 |
| **有参构造**     | 创建对象不方便，代码冗余   | 每次创建都要多行setter调用           |
| **Getter方法** | 无法读取对象数据       | 外部代码无法获取用户信息               |
| **Setter方法** | 无法修改对象数据       | 创建后无法修改，对象状态固定             |

## 5.通过lombok依赖简化
```java
@Data  // 自动生成getter、setter、toString、equals、hashCode
@NoArgsConstructor  // 自动生成无参构造
@AllArgsConstructor // 自动生成全参构造
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
}

// 编译后实际上包含了我们之前手写的所有5个部分
```
注意:如过遇到如参数长度错误,无构造方法等报错时,尝试取消lombok依赖,手动补齐无参有参构造和get,set方法