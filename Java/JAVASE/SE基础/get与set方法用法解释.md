简单理解为get用来获取值,set用来改变赋予值
## 1. **先理解对象的内存结构**

```java
public class User {
    private Integer id;      // 内存地址：0x1000
    private String username; // 内存地址：0x1004  
    private Integer age;     // 内存地址：0x1008
    // ... 其他属性和方法
}
```

每个User对象创建时，在堆内存中会有独立的空间存储这些属性值。

## 2. **完整的User类示例**

```java
public class User {
    // 1. 属性（成员变量）- 存储在堆内存的对象中
    private Integer id;
    private String username;
    private Integer age;
    
    // 2. 无参构造方法
    public User() {
        System.out.println("无参构造被调用，创建空对象");
        // 此时：id=null, username=null, age=null
    }
    
    // 3. 有参构造方法
    public User(Integer id, String username, Integer age) {
        System.out.println("有参构造被调用，初始化对象");
        this.id = id;          // this.id 表示当前对象的id属性
        this.username = username;  // 参数username赋值给this.username
        this.age = age;        // 参数age赋值给this.age
    }
    
    // 4. Getter方法 - 获取属性值
    public Integer getId() {
        System.out.println("调用getId()，返回id: " + id);
        return this.id;  // 返回当前对象的id值
    }
    
    // 5. Setter方法 - 修改属性值
    public void setId(Integer id) {
        System.out.println("调用setId(" + id + ")，修改id");
        this.id = id;  // 参数id赋值给当前对象的id属性
    }
    
    public Integer getAge() {
        System.out.println("调用getAge()，返回age: " + age);
        return this.age;
    }
    
    public void setAge(Integer age) {
        System.out.println("调用setAge(" + age + ")，修改age");
        this.age = age;
    }
}
```

## 3. 值的流动过程

### 场景1：通过有参构造创建并赋值

```java
public class Test {
    public static void main(String[] args) {
        System.out.println("场景1：有参构造");
        // 步骤1：创建对象时直接赋值
        User user1 = new User(1, "张三", 20);
        // 这行代码的执行流程：
        // 1. new User() 在堆内存分配空间（假设地址：0xA001）
        // 2. 参数值传递：1 → 构造方法的id参数，张三 → username参数，20 → age参数
        // 3. 执行构造方法：
        // this.id = id → 0xA001对象的id属性 = 1
        // this.username = username → 0xA001对象的username = "张三"
        // this.age = age → 0xA001对象的age = 20
        
        // 验证
        System.out.println("user1的age: " + user1.getAge()); // 输出: 20
    }
}
```

**内存状态变化：**

```text
创建前：堆内存 [空]
创建后：堆内存 [0xA001对象] → id=1, username="张三", age=20
```

### 场景2：通过无参构造 + Setter赋值

```java
public class Test {
    public static void main(String[] args) {
        System.out.println("场景2：无参构造 + Setter);
        // 步骤1：创建空对象
        User user2 = new User();
        // 内存状态：堆内存 [0xA002对象] → id=null, username=null, age=null
        
        // 步骤2：通过Setter为age赋值
        user2.setAge(25);
        // 这行代码的执行流程：
        // 1. user2.setAge(25) 调用setAge方法
        // 2. 参数传递：25 → setAge方法的age参数
        // 3. 执行方法体：this.age = age → 0xA002对象的age属性 = 25
        // 注意：这里的this指向user2这个对象（内存地址0xA002）
        
        // 验证
        System.out.println("user2的age: " + user2.getAge()); // 输出: 25
        
        // 步骤3：修改age值
        user2.setAge(30);
        // 流程同上：30 → setAge的age参数 → 0xA002对象的age属性 = 30
        System.out.println("修改后user2的age: " + user2.getAge()); // 输出: 30
    }
}
```
### 值传递机制举例
```java
public static void main(String[] args) {
    int newAge = 88;           // main方法局部变量
    
    User user = new User();    // 创建对象，age=null
    user.setAge(newAge);       // 值传递：newAge的值88传递给setAge的参数age
    
    // 相当于：
    // 1. int temp = newAge;   // temp = 88
    // 2. user.setAge(temp);   // temp传递给setAge方法
}
```
## 4. **常见误区澄清**

### 误区1：构造方法中可以直接访问属性

```java
// 正确：有参构造方法
public User(Integer id, Integer age) {
    this.id = id;   // 正确，通过this访问属性
    this.age = age; // 正确
}

// 错误：参数和属性同名时，不加this会混淆
public User(Integer id, Integer age) {
    id = id;       // 错误！左边的id是参数，右边的id也是参数
    age = age;     // 错误！没有修改对象的属性
}

```
### 误区2：Get/Set方法可以没有

```java
public class User {
    public Integer id;    // 公有属性，直接访问
    public Integer age;
    
    // 没有getter/setter，直接访问
}

public class Test {
    public static void main(String[] args) {
        User user = new User();
        user.age = 20;     // 直接赋值
        int age = user.age; // 直接获取
        // 问题：破坏了封装性，无法控制赋值逻辑
    }
}
```

## 5. **完整演示程序**

```java
// 可视化演示程序
public class ValueFlowDemo {
    public static void main(String[] args) {
        System.out.println("========== Java值流动演示 ==========");
        
        System.out.println("\n1. 使用有参构造方法：");
        System.out.println("执行: User user1 = new User(1001, \"小明\", 18);");
        User user1 = new User(1001, "小明", 18);
        System.out.println("结果: user1.age = " + user1.getAge());
        
        System.out.println("\n2. 使用无参构造 + Setter：");
        System.out.println("执行: User user2 = new User();");
        User user2 = new User();
        System.out.println("初始: user2.age = " + user2.getAge()); // null
        
        System.out.println("执行: user2.setAge(25);");
        user2.setAge(25);
        System.out.println("设置后: user2.age = " + user2.getAge());
        
        System.out.println("\n3. 修改已有对象：");
        System.out.println("执行: user1.setAge(35);");
        user1.setAge(35);
        System.out.println("修改后: user1.age = " + user1.getAge());
        
        System.out.println("\n========== 演示结束 ==========");
    }
}

```
**运行结果：**

```text
========== Java值流动演示 ==========

1. 使用有参构造方法：
执行: User user1 = new User(1001, "小明", 18);
有参构造被调用，初始化对象
结果: user1.age = 18

2. 使用无参构造 + Setter：
执行: User user2 = new User();
无参构造被调用，创建空对象
初始: user2.age = null
执行: user2.setAge(25);
调用setAge(25)，修改age
设置后: user2.age = 25

3. 修改已有对象：
执行: user1.setAge(35);
调用setAge(35)，修改age
修改后: user1.age = 35

========== 演示结束 ==========
```

## **6.核心总结：**

1. **有参构造**：创建对象时**一次性**传入所有初始值
    
2. **无参构造**：创建空对象，属性为默认值
    
3. **Setter方法**：对象创建后**修改**此类中的各变量属性值
    
4. **Getter方法**：**读取**属性值