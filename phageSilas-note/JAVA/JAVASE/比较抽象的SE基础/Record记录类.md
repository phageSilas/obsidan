Java 14 以预览特性（Preview Feature）的形式引入了 `record` 类，它是一种透明的数据载体，主要用来简化不可变数据的建模。在 Java 16 中 `record` 成为正式特性，但它的用法从 Java 14 开始就已经基本定型。

下面我会从核心概念、自动生成的方法、自定义与限制、使用场景几个方面，结合代码示例来介绍。

---

## 1. 什么是 record？

`record` 是一种特殊的类，它的所有实例字段都是 `final`，并且一旦创建就不能再被修改（不可变）。它非常适合用来充当 DTO、值对象、坐标点、范围等**纯数据载体**，能极大减少模板代码。

### 基本语法
```java
record Point(int x, int y) { }
```
这一行代码相当于一个完整的类，它自动拥有了：
- 两个 `private final` 字段 `x` 和 `y`
- 一个全参构造器 `Point(int x, int y)`
- 两个访问器方法 `x()` 和 `y()`（注意不是 `getX()`）
- `equals(Object o)` 方法（比较所有字段）
- `hashCode()` 方法（基于所有字段）
- `toString()` 方法（格式如 `Point[x=1, y=2]`）

---

## 2. 快速示例：用 record 替代传统 POJO

**传统 Java 类**（大约 40 行）：
```java
public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point[" + "x=" + x + ", y=" + y + ']';
    }
}
```

**用 record 只需一行**：
```java
record Point(int x, int y) { }
```

使用方式几乎一样：
```java
Point p1 = new Point(1, 2);
Point p2 = new Point(1, 2);

System.out.println(p1.x());      // 1
System.out.println(p1.y());      // 2
System.out.println(p1);          // Point[x=1, y=2]
System.out.println(p1.equals(p2)); // true
System.out.println(p1.hashCode() == p2.hashCode()); // true
```

---

## 3. 紧凑构造器（Compact Constructor）

如果需要在构造时进行参数校验或规范化处理，可以使用**紧凑构造器**（省略参数列表，直接写方法体）。  
这样写的好处是：编译器会将你写的逻辑合并到自动生成的构造器开头，之后才会正式为 `final` 字段赋值。

```java
record Person(String name, int age) {
    // 紧凑构造器，不能有参数列表，也不能对字段直接赋值
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("年龄不能为负数");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("名字不能为空");
        }
        // 可以在这里对参数做规范化（比如修整空格）
        name = name.trim();
        // 编译器会自动将 name、age 赋值给对应的 final 字段
    }
}
```

使用效果：
```java
Person p = new Person(" Alice ", 25);
System.out.println(p.name()); // "Alice"（已修剪）

new Person("", 30);   // 抛出 IllegalArgumentException
new Person("Bob", -1); // 抛出 IllegalArgumentException
```

注意：紧凑构造器中不能对 `this.name` 等字段直接赋值，赋值操作由编译器自动在最后完成。

---

## 4. 添加自定义方法与静态字段/方法

record 虽然会自动生成很多方法，但你仍可以添加自己的实例方法、静态方法或静态字段（不能添加实例字段）。

```java
record Rectangle(double width, double height) {
    // 静态字段
    static final double GOLDEN_RATIO = 1.618;

    // 实例方法：计算面积
    public double area() {
        return width * height;
    }

    // 静态工厂方法：创建一个正方形
    public static Rectangle square(double side) {
        return new Rectangle(side, side);
    }
}
```

使用：
```java
Rectangle r = new Rectangle(3, 4);
System.out.println(r.area());         // 12.0
Rectangle sq = Rectangle.square(5);
System.out.println(sq);               // Rectangle[width=5.0, height=5.0]
System.out.println(Rectangle.GOLDEN_RATIO); // 1.618
```

---

## 5. 实现接口

record 可以实现接口，这在实际开发中非常有用。

```java
interface Describable {
    String describe();
}

record Book(String title, String author) implements Describable {
    @Override
    public String describe() {
        return author + " 著《" + title + "》";
    }
}
```

使用：
```java
Book book = new Book("Java 编程思想", "Bruce Eckel");
System.out.println(book.describe()); // Bruce Eckel 著《Java 编程思想》
```

---

## 6. 局部 record（Local Record）

record 可以定义在方法内部，作为局部数据载体，这在处理临时数据结构时很方便。

```java
public void process() {
    record Pair(String key, int value) { }

    List<Pair> list = List.of(
        new Pair("A", 1),
        new Pair("B", 2)
    );

    for (Pair p : list) {
        System.out.println(p.key() + " -> " + p.value());
    }
}
```

---

## 7. record 的重要限制

- **隐式 final**：record 不能被继承（也不能继承其他类，因为它的父类固定为 `java.lang.Record`）。
- **不能声明实例字段**：record 的状态完全由声明时的“组件”决定，任何额外的实例字段都不允许（但可以声明静态字段）。
- **每个组件都是 `private final`**，只能通过同名的访问器方法读取。
- **不能有显式的父类**：`record Point(int x, int y) extends Object` 会编译错误。
- **不能被序列化机制特殊处理**（使用 `Serializable` 时要小心），但其序列化与反序列化基于组件列表，相对安全。

---

## 8. 常见使用场景

- **DTO / VO**：如 API 返回的 JSON 对象映射
- **键值对**、**元组**：`record Pair<A, B>(A first, B second) {}`
- **配置项**：`record DatabaseConfig(String url, String user, String password) {}`
- **不可变集合元素**：如 `record Color(int r, int g, int b) {}`
- **模式匹配（Java 16+）** 配合 `instanceof` 解构

```java
Object obj = new Point(3, 4);
if (obj instanceof Point(int x, int y)) {
    System.out.println("x=" + x + ", y=" + y); // 直接解构
}
```

---

## 9. 编译与运行（Java 14 预览特性）

由于 Java 14 中 record 是预览特性，编译和运行时需要显式开启：
```bash
javac --enable-preview --release 14 Point.java
java --enable-preview Point
```
如果使用 Java 16 或更高版本（如 17、21），则直接使用，无需额外参数。

---

record 的核心思想是 **“用最少的代码描述清楚数据的结构”**，并自动生成所有配套的样板方法，让开发者专注于业务逻辑而非机械的 getter、equals 等代码。掌握它可以大幅提升代码简洁性和可读性。