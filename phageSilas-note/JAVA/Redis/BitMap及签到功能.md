BitMap（位图）是 Redis 中一个极其惊艳且实用的功能，在处理海量用户的状态标记（比如签到、日活统计、用户状态变更）时，它堪称**“空间优化的大杀器”**。

结合你之前处理 Feed 流分页时对高效设计的追求，我相信你一定会喜欢 BitMap。我们先来重新认识一下它，然后再详细拆解它在“签到”功能中的实战用法。

---

## 1. 什么是 BitMap？

首先要澄清一个误区：**BitMap 在 Redis 中并不是一种全新的数据类型，它的底层其实就是 String（字符串）。**

我们知道，计算机底层的数据都是由 `0` 和 `1` 组成的（二进制位，即 bit）。一个字节（Byte）包含 8 个 bit。

Redis 的 String 类型最大可以存储 512MB 的数据，转换成 bit 就是 $512 \times 1024 \times 1024 \times 8 \approx 42.9$ 亿个 bit。

**BitMap 的核心思想就是：不再用一个 String 去存具体的值（比如 "hello"），而是把 String 拆开，直接去操作这 42.9 亿个 bit 位上的 `0` 或 `1`。**

- **极高的空间利用率：** 记录一个用户的状态（只有是/否两种状态），只需要 1 个 bit。100 万个用户的状态，只需要大概 122 KB 的内存！
    

---

## 2. 为什么不用 MySQL 做签到？

传统的做法是建一张 `sign_record` 表：包含 `id`, `user_id`, `sign_date`。

- **痛点：** 假设你有 100 万日活用户，每天签到一次，一年就是 3.65 亿条数据！这不仅极其消耗存储空间，而且在查询“用户本月连续签到天数”等复杂逻辑时，SQL 语句会非常难写且效率极低。
    

如果换成 **Redis BitMap**：

- 我们用**一个月**为一个单位。每个月最多 31 天，也就是只需要 31 个 bit。
    
- 一个用户一年的签到记录，仅仅只需要 $365 \text{ bits} \approx 46 \text{ bytes}$（字节）！100 万用户一年的签到数据也就大概 46 MB。这是 MySQL 望尘莫及的优化。
    

---

## 3. BitMap 在“签到”中的核心设计

为了实现签到，我们需要巧妙地设计 Redis 的 Key 和 Offset（偏移量）。

- **Key 设计：** `sign:{用户ID}:{年月}`，例如 `sign:1001:202604`（表示用户 1001 在 2026 年 4 月的签到记录）。
    
- **Offset（偏移量）设计：** 使用当月的**“日期号减 1”**作为偏移量。例如，4 月 1 号的偏移量是 `0`，今天是 4 月 14 号，偏移量就是 `13`。
    
- **Value 设计：** `1` 代表已签到，`0` 代表未签到。
    

### 常用的 Redis 命令对应签到业务：

1. **签到（今天打卡）：**
    
    `SETBIT sign:1001:202604 13 1` (把第 14 天的 bit 位置为 1)
    
2. **检查今天是否签到：**
    
    `GETBIT sign:1001:202604 13` (返回 1 说明签了，0 说明没签)
    
3. **统计本月总签到天数：**
    
    `BITCOUNT sign:1001:202604` (直接返回这个 key 中一共有多少个 1)
    

---

## 4. Java 代码实战：实现签到与连续签到统计

### 签到的实现
``` java
/**  
 * 签到功能  
 * @return  
 */  
@Override  
public Result sign() {  
    // 1.获取当前登录用户  
    Long userId = UserHolder.getUser().getId();  
  
    // 2.创建一个签到时间  
    LocalDateTime now = LocalDateTime.now();  
  
    // 3.拼接key  
    String keySuffixx = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));//20220101  
    String key = USER_SIGN_KEY + userId + keySuffixx;  
    int day = now.getDayOfMonth();//本月的第几天  
  
    stringRedisTemplate.opsForValue().setBit(key, day - 1, true); // 0表示本月1号，1表示本月2号，依此类推，day - 1表示当前日期，true表示已签到  
    return Result.ok();  
  
  
}

```

### 代码分析：

1. **Key 的设计非常规范：**
    ``` Java
    String keySuffixx = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String key = USER_SIGN_KEY + userId + keySuffixx;
    ```
    
    把时间和用户 ID 结合，并且按照“月”来分 Key（例如生成类似 `sign:1001:202604` 的 Key）。这保证了 Redis 中单个 Key 的体积不会过大，管理起来非常清晰，到期了如果不需要甚至可以按月清理。
    
    _(小提示：代码里后面的注释写着 `//20220101`，但实际上 `yyyyMM` 格式化出来的结果是 `202604` 这样的年月格式，没有日，这对于按月签到的逻辑来说是**正确**的，只是注释稍微有点小笔误而已~)_
    
1. **偏移量（Offset）计算极其准确：**
    ``` Java
    int day = now.getDayOfMonth();
    stringRedisTemplate.opsForValue().setBit(key, day - 1, true); 
    ```
    
    这是这段代码最核心、最容易出错的地方，但你处理得完全正确！
    
    因为 `getDayOfMonth()` 返回的今天是 1 到 31 的数字，而 Redis 的 BitMap 偏移量是从 `0` 开始算的。所以 `day - 1` 完美地把 1 号映射到了第 0 位，把今天（假设 14 号）映射到了第 13 位。
    
2. **API 使用正确：**
    
    传入 `true`，`StringRedisTemplate` 底层会自动帮你把对应的 bit 位设置为 `1`。
    
### 总结

这段代码就是一个标准的、可以直接上生产环境的 BitMap 签到接口核心代码。只要 `USER_SIGN_KEY` 常量（比如定义为 `"sign:"`）没问题，前端一调用这个接口，该用户今天的打卡记录就以大概 0.1 字节的极小代价，稳稳地存在 Redis 里了！非常棒！

### 连续签到统计的实现
