
## 使用环境
假设有一个User类，然后我们**在另一个类（比如Test类）中想要使用User类的对象**。  在Java中，如果你想要使用一个类的非静态成员（包括属性和方法），你需要先创建该类的对象（实例化）。  因为非静态成员是属于对象的，而不是属于类的。所以你需要先有对象，然后通过对象来访问这些成员。

## User user =new User()
### User: 类名，表示我们需要调用的类。
这指定了我们要创建哪个类的对象。这里我们要创建User类的对象。
### user: 对象的引用变量名，也就是我们给这个对象起的名字，通过这个名字我们可以操作这个对象。
1. 这是一个引用变量，它存储了对象在内存中的地址。我们可以通过这个引用变量来访问对象。名字可以自定义。
2. 注意：这个引用变量本身是存储在栈内存中的，而它指向的对象是存储在堆内存中的。
### new:关键字
new关键字用于在堆内存中创建一个新的对象，并返回这个对象的地址。

### User():构造方法
1. 构造方法用于初始化新创建的对象。它可以设置对象的初始状态,即有参构造（例如，给属性赋初值User(18,"john")）。
2. 如果类中没有显式定义构造方法，Java会提供一个默认的无参构造方法。

### 整个表达式含义
在堆内存中创建一个新的User对象，然后调用User类的无参/有参构造方法来初始化这个对象，最后将这个对象的地址赋值给引用变量user。
注意:如果不先创建对象，而直接使用类名去访问非静态成员，编译器会报错。因为非静态成员是属于对象的，而不是类。静态成员详见:[[对象#补充 静态成员]]

### 详细代码解释
1. User user:告诉Java编译器："我要创建一个名为user的变量，它只能存储User类型的对象"
```java
//User user;类比如定义属性
String name;      // 声明一个字符串变量
int age;          // 声明一个整数变量
User user;        // 声明一个User类型的变量,user变量名可以自定义
```
2. 变量名user:给这个对象引用起个名字，以后就用这个名字操作对象
```java
User user1;  // 创建一个叫user1的遥控器（还没配对任何对象）
User user2;  // 创建另一个叫user2的遥控器
// 此时这两个遥控器都没指向任何对象，是null


```
	一个类可以创建多个对象
```java
public class Test {
    public static void main(String[] args) {
        // 创建三个独立的对象
        User user1 = new User();  // 对象1
        User user2 = new User();  // 对象2  
        User user3 = new User();  // 对象3
        
        user1.setAge(10);
        user2.setAge(20);
        user3.setAge(30);
        
        // 三个对象互不影响
        System.out.println(user1.getAge());  // 10
        System.out.println(user2.getAge());  // 20
        System.out.println(user3.getAge());  // 30
    }
}
```
3. 关键字new:在堆内存中分配空间，创建真正的对象
```java
// 不写new会怎样？
User user;  // 只有遥控器，没有电视
// user.setAge(20); // ❌ 会报空指针异常，因为user=null

User user = new User();  // 有遥控器也有电视
// user.setAge(20);      // ✅ 可以正常操作
```

4. 构造方法User():初始化新创建的对象
```java
// 有参构造和无参构造的区别：
new User();           // 调用无参构造，属性是默认值
new User(1, "张三", 20);  // 调用有参构造，属性有初始值
```
### 内存解释
```java
public class Test {
    public static void main(String[] args) {
        // 情况1：只有声明，没有new
        User user1;  // 栈内存：user1变量，值=null
        // user1.setAge(20);  // 运行时错误：NullPointerException
        
        // 情况2：完整的对象创建
        User user2 = new User();  // 发生了以下事情：
        // 1. 在栈内存创建user2变量
        // 2. 在堆内存分配一块空间存储User对象
        // 3. 调用User()构造方法初始化对象
        // 4. 把堆内存地址赋值给user2变量
        
        user2.setAge(20);  // ✅ 正常执行
    }
}
```
```text
user2内存分布

栈内存（Stack）      堆内存（Heap）
=============       ============
user2: 0x1001       [地址0x1001: User对象]
                     id: null
                     username: null  
                     age: 20
```

### 详细执行过程
```java
public class Demo {
    public static void main(String[] args) {
        System.out.println("步骤1: 声明变量");
        User user;  // 在栈帧中分配空间给user变量，值=null
        
        System.out.println("步骤2: 创建对象");
        user = new User();  // 分四步：
        // 1. new: JVM在堆内存中计算User对象需要多少字节
        // 2. 分配: 在堆内存中找到合适空间，标记为"已使用"
        // 3. User(): 调用构造方法，初始化对象属性
        // 4. = : 把堆内存地址(如0x1001)赋值给user变量
        
        System.out.println("步骤3: 使用对象");
        user.setAge(25);  // 根据user中的地址0x1001找到堆内存中的对象
                          // 然后调用该对象的setAge方法
        
        System.out.println("步骤4: 获取值");
        int age = user.getAge();  // 通过地址找到对象，调用getAge方法
        System.out.println("年龄: " + age);
    }
}
```

## 创建子类对象
**Animal animal = new Dog();**
### 可以这样写的基本前提
子类对象也算作父类对象
Animal包括Dog,使用Animal不仅可以调用Dog中的方法,也可以使用父类Animal中的方法
```java
// 方式A：直接创建Dog对象
Dog myDog = new Dog();
// 内存：myDog -> [Dog对象]

// 方式B：用Animal引用指向Dog对象
Animal myAnimal = new Dog();
// 内存：myAnimal -> [Dog对象]
// 注意：虽然是Animal引用，但指向的实际是Dog对象

// 验证：
System.out.println(myAnimal.getClass());  // 输出：class Dog
System.out.println(myDog.getClass());     // 输出：class Dog
// 两者指向的都是Dog类型的对象！

```
### 区别
Dog dog = new Dog();     
Animal animal = new Dog(); 

1. **编译时类型不同**：决定了编译器允许调用哪些方法
2. **访问权限不同**：Animal引用看不到Dog特有方法
3. **设计意图不同**：部分细节见[[多态#向上转型]]
    - `Dog dog`：强调"我需要Dog的完整功能"
    - `Animal animal`：强调"我只需要Animal的通用接口"
#### **引用类型不同**

```java
class Animal {
    public void eat() {
        System.out.println("Animal eating");
    }
    
    public void sleep() {
        System.out.println("Animal sleeping");
    }
}

class Dog extends Animal {
    @Override
    public void eat() {
        System.out.println("Dog eating bone");
    }
    
    // Dog新增的方法
    public void bark() {
        System.out.println("Wang Wang!");
    }
    
    // Dog特有的方法
    public void guard() {
        System.out.println("Dog is guarding");
    }
}

public class Test {
    public static void main(String[] args) {
        // 写法A：Dog引用
        Dog dog = new Dog();
        
        // 写法B：Animal引用
        Animal animal = new Dog();
        
        // 两者的访问权限不同：
        // ========== 都能访问的方法 ==========
        dog.eat();        // ✅ 输出：Dog eating bone
        animal.eat();     // ✅ 输出：Dog eating bone
        // 都能调用eat，执行的都是Dog重写后的版本
        
        dog.sleep();      // ✅ 输出：Animal sleeping
        animal.sleep();   // ✅ 输出：Animal sleeping
        // 都能调用从Animal继承的sleep方法
        
        // ========== 只有Dog引用能访问的方法 ==========
        dog.bark();       // ✅ 输出：Wang Wang!
        // animal.bark();   // ❌ 编译错误！Animal类没有bark方法
        
        dog.guard();      // ✅ 输出：Dog is guarding
        // animal.guard();  // ❌ 编译错误！
        
        System.out.println("==============");
        
        // 实际上底层是同一个Dog对象，但通过不同"视角"访问
        System.out.println(dog.getClass());    // class Dog
        System.out.println(animal.getClass()); // class Dog
    }
}


```