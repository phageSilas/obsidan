`permits` 关键字是 **Java 17** 中伴随“密封类”（Sealed Classes）特性而生的，它的作用是**在类的声明中，精确地指定哪些类或接口有权继承或实现它**。

简单来说，如果一个类或接口想要控制自己的继承体系（比如只允许A、B、C三个类继承自己，而其他任何类都无法扩展），它就可以通过 `sealed` 关键字声明自己是“密封”的，并用 `permits` 列出一个“授权名单”，实现精细化的权限控制。

### 🔑 `permits` 的语法与角色

`permits` 关键字的使用非常简单直观，它紧跟在 `sealed` 声明的类名后面：

```java
// 声明一个密封类 Shape，并授权 Circle 和 Rectangle 可以继承它
public sealed class Shape permits Circle, Rectangle {
    // 类的通用逻辑
}
```


在这个语法中，`permits` 扮演了“门禁系统”的角色，只有被明确列入名单的 `Circle` 和 `Rectangle` 才能继承 `Shape`，任何其他类（例如 `Triangle`）尝试继承都会导致编译错误。

### 📜 `permits` 的约束与规则

使用 `permits` 并不是随意列出子类那么简单，它遵循一套严格的规则：

1.  **必须与 `sealed` 配对**：`permits` 关键字不能被单独使用，它必须跟在由 `sealed` 修饰的类或接口声明后面。
2.  **子类声明“继承策略”**：所有被 `permits` 授权的子类，都必须用以下三种修饰符之一明确声明自己的“继承策略”：
    *   `final`：此子类是最终的，**不能再被任何类继承**，到此为止。
    *   `sealed`：此子类也是一个**密封类**，可以继续使用 `permits` 限制更下一级的子类。
    *   `non-sealed`：此子类解除了密封限制，**重新对任何类开放继承**。
3.  **子类必须显式继承**：所有被 `permits` 列出的类，必须在其声明中明确使用 `extends` 或 `implements` 来继承或实现该密封父类。
4.  **处于同一模块或包中**：授权的子类必须与密封父类位于**同一个模块（module）或同一个包（package）下**。
5.  **`permits` 可以省略**：如果所有被允许的子类都和密封父类写在**同一个源文件（.java）** 中，`permits` 子句可以省略，编译器会自动识别。

### 🎯 `permits` 的实战应用场景

`permits` 关键字在需要构建**稳定、可预测且类型安全**的类层次结构时尤其重要。

*   **1. 领域驱动设计 (DDD)：定义有限状态**：当你需要为一个状态机建模时，`permits` 可以确保状态的种类是固定且已知的，例如订单的几种状态。
    ```java
    // 订单状态只能是这三种之一
    public sealed interface OrderState permits Pending, Shipped, Delivered {}
    ```

*   **2. 安全的库或API设计**：作为库或API的作者，你可以通过密封类防止使用者通过非法继承来篡改核心逻辑，保证库的完整性。
    ```java
    // 支付方式只允许这三种，防止出现意外的支付渠道
    public sealed class Payment permits CreditCardPayment, AlipayPayment, WeChatPayment {}
    ```

*   **3. 表达式树或AST节点**：在编译原理或DSL设计中，可以明确定义所有可能的节点类型，如 `Expr` 节点可以是常量、二元运算等。
    ```java
    // 表达式节点可以是常量、二元运算或一元运算
    public sealed interface Expr permits ConstantExpr, BinaryExpr, UnaryExpr {}
    ```

### 🤝 与模式匹配的完美协同

`permits` 最强大的地方，在于它与 Java 17 引入的 `switch` 模式匹配特性结合。当 `switch` 表达式处理一个密封类时，编译器能利用 `permits` 列表，**自动验证所有可能的子类是否都已被处理**，极大地提升了代码的安全性。

```java
// 假设 Shape 是 sealed 类，permits 了 Circle 和 Rectangle
double area = switch (shape) {
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.width() * r.height();
    // 如果遗漏任何一个允许的子类，编译器就会报错，无需 default 分支！
};
```

### ❗ 常见问题与注意事项

*   **`permits` 与 `final` 的区别**：
    *   `final` 是“一刀切”，完全禁止任何形式的继承。
    *   `sealed` + `permits` 是“按名单入场”，允许指定的一小部分类继承，提供了更细粒度的控制。
*   **`permits` 与访问修饰符 (`public`, `protected`) 的区别**：
    *   访问修饰符控制的是“在什么地方可以访问一个类”。
    *   `permits` 控制的是“哪些类可以继承一个类”，两者解决的是不同层面的问题。
*   **`permits` 与 `record`**：`record` 类本质上是 `final` 的，因此可以被一个密封接口通过 `permits` 授权实现，但它自身不能再使用 `permits` 去授权其他类。
*   **兼容性**：`permits` 关键字是 Java 17 的正式特性。在 Java 15 和 16 中，它作为预览特性存在，需要添加 `--enable-preview` 编译参数。

### 💎 总结

`permits` 关键字是 Java 密封类特性中用于实现 **“白名单”式继承控制**的核心。它让开发者能够精确地定义类层次结构，将继承关系从“无政府状态”变为“授权准入”，从而极大地增强了代码的**安全性、可维护性和可预测性**，尤其是在与 `switch` 模式匹配结合时，能构建出无比健壮的系统。