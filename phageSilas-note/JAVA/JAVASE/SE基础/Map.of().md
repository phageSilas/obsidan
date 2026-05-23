`Map.of()` 是 Java 9 在 `Map` 接口中引入的一组**静态工厂方法**，专门用来快速创建**不可变的 Map 实例**。它让你告别 `new HashMap<>()` 再逐个 `put` 的繁琐操作，尤其适合创建固定的小型映射。

---

## 1. 基本语法

`Map.of()` 提供了多个重载版本，接受 **0～10 对键值**，参数按 `key1, value1, key2, value2, ...` 的顺序排列。

```java
// 空 Map
Map<String, Integer> empty = Map.of();

// 单键值对
Map<String, Integer> one = Map.of("A", 1);

// 多键值对（最多 10 对）
Map<String, Integer> map = Map.of("A", 1, "B", 2, "C", 3);
```

---

## 2. 核心特性（务必注意）

- **不可变**：返回的 Map **完全不可修改**，调用 `put`、`remove`、`clear` 等方法会抛出 `UnsupportedOperationException`。
- **禁止 `null`**：**键或值都不能为 `null`**，否则抛出 `NullPointerException`。
- **拒绝重复键**：如果参数中出现重复的键，会抛出 `IllegalArgumentException`。
- **顺序不保证**：对于 3 对及以下的 Map，会保持插入顺序；超过后不保证顺序（实际实现可能不同，但规范不承诺迭代顺序）。

```java
Map<String, Integer> map = Map.of("A", 1, "B", 2);
map.put("C", 3);   // ❌ UnsupportedOperationException
Map.of("A", 1, null, 2);   // ❌ NullPointerException
Map.of("A", 1, "A", 2);    // ❌ IllegalArgumentException: duplicate key
```

---

## 3. 超过 10 对键值怎么办？

`Map.of()` 只支持到 10 对，如需更多，用 **`Map.ofEntries()`** + `Map.entry(K, V)`：

```java
Map<String, Integer> bigMap = Map.ofEntries(
    Map.entry("A", 1),
    Map.entry("B", 2),
    Map.entry("C", 3),
    Map.entry("D", 4),
    Map.entry("E", 5),
    // ... 任意多个
    Map.entry("Z", 26)
);
```
同样返回不可变 Map，键值都不能为 `null`，不允许重复键。

---

## 4. 典型使用场景

- **常量配置/映射**：比如状态码与描述的对应关系。
- **枚举值转换**：快速建立枚举到字符串的映射。
- **方法返回值**：**返回一个只读的映射，保证不会被调用方篡改**。
- **测试代码**：快速构造少量测试数据。

```java
// 一周日程的只读映射
Map<String, String> weekPlan = Map.of(
    "MON", "会议",
    "TUE", "开发",
    "WED", "代码评审"
);
```

---

## 5. 与 `new HashMap()` 的区别

```java
// 可变，可以随时修改
Map<String, Integer> mutable = new HashMap<>();
mutable.put("A", 1);

// 不可变，适合希望数据被保护、不被意外修改的场景
Map<String, Integer> immutable = Map.of("A", 1);
```

---

## 6. 注意事项

- `Map.of()` 返回的不可变 Map **不允许 `null`**，如果你需要“可为 null 值”的 Map，还是要用 `HashMap` 等传统实现。
- 重载的方法参数是成对的，确保参数数量是偶数，否则编译报错。
- 它是 **值对象** 风格的实现，适合**小量数据**，不要滥用在大数据量上（性能无碍，但语义上它表达的是固定集）。

掌握 `Map.of()` 可以让你的代码更简洁、意图更清晰，同时借助不可变性减少防御性拷贝和并发问题。