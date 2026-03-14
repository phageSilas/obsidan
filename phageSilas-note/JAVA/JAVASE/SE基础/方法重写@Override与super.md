## 1. **方法重写的基本概念**

方法重写（Override）是面向对象编程的一个重要特性，指**子类重新定义父类中已经存在的方法**，以提供更具体的实现。

## 2.使用条件
1. 重写的方法必须与父类方法有相同的方法名、参数列表和返回类型（或是其子类）。
2. 重写的方法的访问权限不能低于父类方法的访问权限（例如，父类方法是public，子类方法也必须是public）。
3. 重写的方法不能抛出比父类方法更多的异常（检查异常）
4. **重写的方法只在本类中生效,除非其他类继承该类**

## 3.作用
1. **多态性的实现**：通过方法重写，子类可以根据自己的需要重新定义父类的方法，从而在运行时根据对象的实际类型来调用相应的方法。
2. **增强或修改父类的行为**：子类可以在不改变父类方法名的情况下，提供不同的实现，从而扩展或修改父类的功能。
3. **适应子类的特殊需求**：子类可能有一些特殊的行为，这些行为与父类不同，因此需要重写父类的方法。


## 4.基本格式
@Override
public 希望返回的数据类型  需要重写的方法(){
需要该方法实现的具体操作;
return 需要的数据
}

## 5.简单示例
```java
class Animal {
    public void makeSound() {
        System.out.println("动物叫");
    }
}

class Dog extends Animal {
    @Override  // 重写：重新定义父类方法
    public void makeSound() {
        System.out.println("汪汪汪");
    }
}
```

## 6.常用重写
### ①toString():常用于User用户类
#### 未重写toString的问题：原始的toString只会返回哈希值

```java
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    
    // 构造方法、getter/setter...
    
    // 没有重写toString，继承Object的toString
}

public class Test {
    public static void main(String[] args) {
        User user = new User(1, "zhangsan", "123456", "张三", 25);
        
        // 打印对象（会自动调用toString）
        System.out.println("用户信息：" + user);
        // 输出：用户信息：User@1b6d3586
        
        // 调试时看不到具体内容
        // 在集合中更糟糕
        List<User> users = new ArrayList<>();
        users.add(new User(1, "zhangsan", "123", "张三", 20));
        users.add(new User(2, "lisi", "456", "李四", 22));
        
        System.out.println("用户列表：" + users);
        // 输出：用户列表：[User@1b6d3586, User@4554617c]
        // 完全不知道里面是谁！
    }
}

```
#### 重写toString的好处：

```java
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    
    // 构造方法、getter/setter...
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
        // 注意：password不应该在toString中显示，安全考虑！
    }
}

public class Test {
    public static void main(String[] args) {
        User user = new User(1, "zhangsan", "123456", "张三", 25);
        
        // 1. 直接打印对象
        System.out.println("用户信息：" + user);
        // 输出：用户信息：User{id=1, username='zhangsan', name='张三', age=25}
        
        // 2. 调试时直接看到内容
        System.out.println(user);  // 直接打印，自动调用toString
        
        // 3. 在集合中也能看到内容
        List<User> users = new ArrayList<>();
        users.add(new User(1, "zhangsan", "123", "张三", 20));
        users.add(new User(2, "lisi", "456", "李四", 22));
        
        System.out.println("用户列表：" + users);
        // 输出：
        //用户列表：[User{id=1, username='zhangsan', name='张三', age=20}, 
        //  User{id=2, username='lisi', name='李四', age=22}]
        
        // 4. 日志记录
        log.info("用户登录：{}", user);  // 日志框架会自动调用toString
        // 输出：用户登录：User{id=1, username='zhangsan', name='张三', age=25}
    }
}
```

### 重写②equals(Object o)和hashCode()
```java
public class User {
    private Integer id;
    private String username;
    private String name;
    private Integer age;
    
    // 重写toString...
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // 通常根据业务主键判断相等性
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        // 必须和equals保持一致
        return Objects.hash(id);
    }
}

// 使用场景：在集合中查找、去重
public class UserTest {
    public static void main(String[] args) {
        User user1 = new User(1, "zhangsan", "张三", 20);
        User user2 = new User(1, "zhangsan2", "张三", 21);
        
        System.out.println(user1.equals(user2));  // true，因为id相同
        
        Set<User> users = new HashSet<>();
        users.add(user1);
        users.add(user2);  // 不会添加，因为equals返回true
        
        System.out.println(users.size());  // 1
    }
}
```

## 7.重写后仍想使用之前的方法
### 概要
1. 重写方法后，子类对象调用该方法时，将执行重写后的方法。
2. 如果需要在子类中调用父类被重写的方法，可以使用**super.方法名()**。
3. 但是，对于Object类的方法，通常我们不会在重写时调用父类方法，因为Object类的方法实现通常不满足我们的业务需求。

### 1. **重写后的调用情况**

#### 默认情况：调用重写后的方法

```java
class Animal {
    public void makeSound() {
        System.out.println("动物叫");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("汪汪汪");
    }
}

public class Test {
    public static void main(String[] args) {
        Dog dog = new Dog();
        dog.makeSound();  // 输出：汪汪汪（调用重写后的方法）
        
        Animal animal = new Dog();
        animal.makeSound();  // 输出：汪汪汪（多态，调用子类重写的方法）
    }
}
```
==Animal animal = new Dog();==可以这样写的原因详见[[创建对象new与get,set方法与创建子类对象#创建子类对象]]
### 2. **如何调用父类被重写的方法：super关键字**

#### super关键字的作用：

- 访问父类的属性：`super.属性名`
    
- 调用父类的方法：`super.方法名()`
    
- 调用父类的构造方法：`super(参数)`
    
#### 示例1：在重写的方法中调用父类方法

```java
class Animal {
    public void makeSound() {
        System.out.println("动物叫");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        // 1. 先调用父类的方法
        super.makeSound();  // 输出：动物叫
        
        // 2. 再添加子类的特定行为
        System.out.println("汪汪汪");
        System.out.println("摇尾巴");
    }
}

public class Test {
    public static void main(String[] args) {
        Dog dog = new Dog();
        dog.makeSound();
        // 输出：
        // 动物叫
        // 汪汪汪
        // 摇尾巴
    }
}
```

#### 示例2：在User中重写toString并包含父类信息

```java
// 假设有一个父类BaseEntity
public class BaseEntity {
    private Long id;
    private Date createTime;
    private Date updateTime;
    
    public BaseEntity() {}
    
    // getter/setter
    
    @Override
    public String toString() {
        return "BaseEntity{" +
                "id=" + id +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}

// User继承BaseEntity
public class User extends BaseEntity {
    private String username;
    private String password;
    private String name;
    private Integer age;
    
    // 构造方法、getter/setter...
    
    @Override
    public String toString() {
        // 方案1：调用父类的toString，然后添加子类信息
        return super.toString() + 
               " -> User{" +
               "username='" + username + '\'' +
               ", name='" + name + '\'' +
               ", age=" + age +
               '}';
        
        // 方案2：手动组合信息
        /*
        return "User{" +
                super.toString() +  // 父类信息
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
        */
    }
}

public class Test {
    public static void main(String[] args) {
        User user = new User();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setName("张三");
        user.setAge(25);
        user.setCreateTime(new Date());
        
        System.out.println(user.toString());
        // 输出：
        // BaseEntity{id=1, createTime=Mon Oct 10 10:00:00 CST 2023, updateTime=null} -> User{username='zhangsan', name='张三', age=25}
    }
}
```

### **不能直接调用父类方法的情况**
#### 情况1:外部类/子类直接调用super.方法()
#####  **语法限制：super关键字的官方定位**

- `super`是一个**关键字**，不是对象的属性
    
- 它只在**类的内部**有效，表示"当前对象的父类部分"
    
- 它的作用域是**当前类的方法和构造器内部**
    
```java
class Parent {
    public void method() {
        System.out.println("Parent.method()");
    }
}

class Child extends Parent {
    @Override
    public void method() {
        System.out.println("Child.method()");
    }
    
    public void test() {
        // ✅ 正确：在Child类内部使用super
        super.method();  // 调用Parent的method
    }
}
Parent内部类
------------------------------------------
Main外部类
public class Main {
    public static void main(String[] args) {
        Child child = new Child();
        
        // ❌ 错误：不能在类外部使用super
        // child.super.method();  // 语法错误！
        
        // ❌ 错误：不能通过变量访问super
        // Object obj = child;
        // obj.super.method();  // 不存在这样的语法
        
        // 只能通过Child类内部的方法间接调用
        child.test();  // 输出：Parent.method()
    }
}
```

#### 情况2：通过父类引用调用被重写的方法（多态）

```java
class Parent {
    public void show() {
        System.out.println("Parent.show()");
    }
}

class Child extends Parent {
    @Override
    public void show() {
        System.out.println("Child.show()");
    }
    
    public void callParentShow() {
        super.show();  // ✅ 可以调用：输出 Parent.show()
    }
}

public class Test {
    public static void main(String[] args) {
        Child child = new Child();
        child.show();           // 输出：Child.show()
        child.callParentShow(); // 输出：Parent.show()
        
        Parent parent = new Child();
        parent.show();  // 输出：Child.show()
        // 即使声明为Parent类型，实际调用的是Child的show方法
        
        // 无法直接调用：parent.super.show();  // ❌ 语法错误
    }
}

```
#### 情况3：静态方法不能被重写，没有super概念

```java
class Parent {
    public static void staticMethod() {
        System.out.println("Parent.staticMethod()");
    }
}

class Child extends Parent {
    // 这不是重写，是隐藏（hide）
    public static void staticMethod() {
        System.out.println("Child.staticMethod()");
        // 不能使用super.staticMethod()，因为静态方法没有多态
        Parent.staticMethod();  // ✅ 通过类名调用
    }
}
```
