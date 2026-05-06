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