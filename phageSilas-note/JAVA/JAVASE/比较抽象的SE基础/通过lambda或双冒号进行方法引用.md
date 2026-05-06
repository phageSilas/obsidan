# 方法引用和一般的方法调用区别
简而言之：`方法名(参数)` 是在**立刻执行代码**，而 `::` 是在**传递代码的执行说明书**。

以下是它们之间最核心的几个区别：

### 1. 核心区别：立即执行 vs 延迟执行（传递行为）

- **直接调用 `方法名(参数)`**：
    
    这是**主动的、立即的**。当你写下这行代码并运行到这里时，JVM 会立刻跳到该方法中执行逻辑，计算出结果，然后带着结果返回继续往下走。
    
- **方法引用 `类名::方法名`**：
    
    这是**被动的、延迟的（Lazy）**。你并没有执行这个方法，你只是**把这个方法本身当成了一个变量**传递给了别人（比如传递给了 Stream API）。至于这个方法什么时候执行、执行多少次、传什么参数进去，完全由接收它的那方（框架或底层代码）来决定。
    
**代码对比体验：**
``` Java
// 1. 直接调用（立即执行）
// 此时 getRandomNumber() 会立刻执行，把生成的那个确定的数字传给 print 方法
print(getRandomNumber()); 

// 2. 方法引用（延迟执行）
// 此时 getRandomNumber() 并没有立刻执行！
// 你只是把“如何获取随机数”这个动作交给了 Stream。
// 至于它生成几个？什么时候生成？由 generate() 和 limit(5) 决定。
Stream.generate(Math::random).limit(5).forEach(System.out::println);
```

### 2. 传递的内容：结果/数据 vs 代码逻辑

- **直接调用**传递的是**结果（数据）**。
    
    例如 `doSomething( calculate() )`。程序必须先算出 `calculate()` 的结果（比如是 100），然后把 100 当作参数传给 `doSomething(100)`。
    
- **方法引用**传递的是**代码逻辑（行为）**。
    
    例如 `doSomething( this::calculate )`。程序不会立刻计算，而是把 `calculate` 这段**代码的指针/引用**扔给 `doSomething` 方法内部。这就叫做“将函数作为一等公民传递”。
    

### 3. 上下文要求的区别

- **直接调用**：可以在代码的任何地方使用，只要你有访问权限。
    
- **方法引用 `::`**：**只能**用在需要函数式接口（Functional Interface）的地方。它必须有一个“目标类型”来接收它（比如 `Runnable`, `Supplier`, `Function`, `Consumer` 等）。它本质上就是 Lambda 表达式的简写。
    

### 4. 一个通俗的生活比喻

假设你是一家公司的老板，你需要员工去打印一份文件：

- **直接调用 `打印(文件)`**：
    
    你亲自走到打印机面前，塞入文件，按下打印键。马上就听到了机器转动的声音。（立刻发生，你亲自动手执行）。
    
- **方法引用 `打印机::打印`**：
    
    你找来秘书，告诉她：“**这是使用打印机打印的方法**（`打印机::打印`），这叠文件交给你，你看着办”。
    
    秘书拿到这个“方法”后，可能会马上打印，也可能会攒到下班前一次性打印，甚至如果发现文件是空的，她可能一次都不打印（延迟发生，控制权交给了框架/底层的循环逻辑）。
    

### 总结

- 如果你想告诉程序“现在就做这件事，把结果给我”，用直接调用 `方法(参数)`。
    
- 如果你想告诉程序“这是一段现成的逻辑，你拿着，等会儿遍历数据/触发事件时，按这个逻辑处理”，用方法引用 `::`。


# 使用
在 Java 中，双冒号 `::` 并不是某个特定方法的缩写，而是 **方法引用（Method Reference）** 的操作符。它是在 Java 8 中与 Lambda 表达式一起引入的重要特性。

它的核心作用是作为 **Lambda 表达式的语法糖**。当你的 Lambda 表达式仅仅是为了**调用一个已经存在的现成方法**时，就可以直接使用 `::` 来提取该方法，让代码更加紧凑、直观和具有声明性。

你可以把 `::` 理解为：“不要自己写逻辑了，直接把这个现成的方法拿过来用”。

方法引用主要分为以下四种使用场景：

### 1. 静态方法引用 (`类名::静态方法名`)

当 Lambda 表达式只是在调用一个类的静态方法时。

- **Lambda 写法:** `str -> Integer.parseInt(str)`
    
- **双冒号写法:** `Integer::parseInt`
``` Java
List<String> stringNumbers = Arrays.asList("1", "2", "3");
// 使用 Lambda
stringNumbers.stream().map(s -> Integer.parseInt(s));
// 使用方法引用
stringNumbers.stream().map(Integer::parseInt); 
```

### 2. 特定类的任意对象的实例方法引用 (`类名::实例方法名`)

这是最常用但也最容易产生疑惑的一种。在处理集合数据（比如 Service 层处理从 DAO 层拿到的实体列表）时非常常见。当 Lambda 的**第一个参数**作为方法的调用者，且后面的参数（如果有的话）作为该方法的参数时使用。

- **Lambda 写法:** `user -> user.getName()`
    
- **双冒号写法:** `User::getName`

``` Java
// 假设有一个 User 实体类列表，现在需要提取所有用户的名字
List<User> users = userService.listAll();

// 使用 Lambda
List<String> names = users.stream().map(u -> u.getName()).collect(Collectors.toList());

// 使用方法引用 (极简写法)
List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
```

### 3. 特定对象的实例方法引用 (`对象实例::实例方法名`)

当 Lambda 表达式只是在调用一个外部已经存在的具体对象的方法时。最经典的例子就是打印输出。

- **Lambda 写法:** `str -> System.out.println(str)`
    
- **双冒号写法:** `System.out::println` （`System.out` 是一个具体的 `PrintStream` 对象实例）
``` Java
List<String> list = Arrays.asList("A", "B", "C");
// 遍历打印
list.forEach(System.out::println);
```

### 4. 构造器引用 (`类名::new`)

用于快速实例化对象。只要目标接口的抽象方法与该类的构造方法的参数列表一致即可。这在将一种数据模型转换为另一种（例如 DO 转 DTO）时很方便。

- **无参构造 Lambda:** `() -> new ArrayList<>()`
    
- **无参构造引用:** `ArrayList::new`

``` Java
// 将 Stream 收集到指定的 List 实现中
List<String> names = stream.collect(Collectors.toCollection(ArrayList::new));
```

### 总结

如果你的代码写成 Lambda 表达式后，箭头 `->` 右侧**没有做任何额外的业务逻辑处理**，仅仅是原封不动地透传参数给另一个方法，那么这就是使用 `::` 的绝佳时机。