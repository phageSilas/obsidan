这是一个非常经典的业务场景。使用 Redis 的 Sorted Set（有序集合）配合 `ZREVRANGEBYSCORE` 命令来实现“最新发布的博客一直在最前面”的滚动分页（Feed流），是目前业界非常主流且高效的做法。

# 1. `ZREVRANGEBYSCORE` 命令简介

`ZREVRANGEBYSCORE` 的作用是返回有序集合中，分数（Score）在 `max` 和 `min` 之间的所有元素。由于带有 `REV` (Reverse)，它的返回顺序是**按分数从大到小排列**的。

**基本语法：**

代码段

```
ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
```

**参数解析：**

- **`key`**: Redis 中的键名（例如：`blog:feed:user_1`）。
    
- **`max`**: 分数的最大值。
    
- **`min`**: 分数的最小值。
    
- **`WITHSCORES`**: 可选参数，加上它连同分数一起返回。
    
- **`LIMIT offset count`**: 可选参数，用于限制返回结果的数量，类似于 SQL 中的 `LIMIT`。`offset` 是偏移量，`count` 是返回的数量。
    

> **💡 提示：** 在 Redis 6.2 及以上版本中，官方推荐使用更加统一的 `ZRANGE key max min BYSCORE REV LIMIT offset count` 来代替该命令，但两者的底层逻辑和效果是完全一致的。

---

# 2. 为什么用它做博客Feed流？

在博客Feed流场景中，我们通常把**博客的发布时间戳**作为 Sorted Set 的 **Score**，把**博客的 ID** 作为 **Member**。

因为最新发布的博客时间戳最大，使用 `ZREVRANGEBYSCORE` 按分数从大到小降序排列，就能完美实现“最新发布的博客排在最前面”。

---

# 3. 核心难点：解析截图中的“滚动分页查询参数”

如果使用传统的 `LIMIT offset, count` (如 `LIMIT 0, 10`, `LIMIT 10, 10`) 进行分页，会存在一个致命问题：**如果在你翻页的过程中，有新的博客发布了，整个列表的数据会整体下移，导致你在看下一页时，会出现重复的博客。**

截图中的参数逻辑，正是为了解决这个问题而设计的**“滚动分页（Scroll Pagination）”**方案：

- **`max` (最大分数/时间戳)**:
    
    - **第一次查询（第一页）**：传**当前时间戳**（或直接传 `+inf` 表示正无穷大）。
        
    - **后续查询（下一页）**：传**上一次查询结果中最小的时间戳**（也就是上一页最后一条博客的时间戳）。
        
- **`min` (最小分数/时间戳)**:
    
    - 始终传 `0`（或者你系统中允许的最小时间戳）。
        
- **`offset` (偏移量)**:
    
    - **第一次查询**：传 `0`。
        
    - **后续查询**：传**在上一页结果中，时间戳与最小时间戳相同的元素的个数**。
        
    - _为什么要这样设置？_ 因为你下一页查询的 `max` 是上一页的最后一条记录的时间戳。如果不加偏移，下一页的第一条数据就会是上一页的最后一条数据（重复了）。如果有两篇博客是同一毫秒发布的，仅仅忽略第一条还不够，必须统计上一页到底包含了几个这个最小时间戳的博客，把它们全部偏移跳过。
        
- **`count` (查询数量)**:
    
    - 每次希望加载的博客数量（例如 10 篇）。
        

---

# 4. 简单示例演示

假设我们有一个博客流 `blog:feed`，包含以下博客ID和对应的时间戳：

代码段

```
ZADD blog:feed 10001 "blog_1"  # 最早发布
ZADD blog:feed 10005 "blog_2"
ZADD blog:feed 10008 "blog_3"
ZADD blog:feed 10008 "blog_4"  # 和 blog_3 同一时间发布
ZADD blog:feed 10010 "blog_5"
ZADD blog:feed 10015 "blog_6"  # 最新发布
```

假设我们**每页查询 3 条数据**。

## 第一步：请求第一页

- `max` = 10020 (假设当前时间戳为10020)
    
- `min` = 0
    
- `offset` = 0
    
- `count` = 3
    

代码段

```
# 命令
ZREVRANGEBYSCORE blog:feed 10020 0 WITHSCORES LIMIT 0 3

# 返回结果
1) "blog_6"
2) "10015"
3) "blog_5"
4) "10010"
5) "blog_4"
6) "10008"  <-- 这是本页最小的时间戳
```

**前端/客户端需要记录的关键状态，用于下次请求：**

- 上一次的最小时间戳：`10008`
    
- 上一批结果中时间戳为 `10008` 的个数：`1` 个（只有 blog_4）
    

## 第二步：请求第二页（触发滚动）

此时，你向下滚动页面，加载第二页：

- `max` = `10008` (上一次的最小时间戳)
    
- `min` = 0
    
- `offset` = `1` (上次结果中分数为10008的元素有1个，所以偏移量为1，正好跳过 blog_4)
    
- `count` = 3
    

代码段

```
# 命令
ZREVRANGEBYSCORE blog:feed 10008 0 WITHSCORES LIMIT 1 3

# 返回结果
1) "blog_3"
2) "10008"
3) "blog_2"
4) "10005"
5) "blog_1"
6) "10001"
```

通过动态调整 `max` 和 `offset`，即使在第一步和第二步之间有新的博客（比如 score 为 10018 的博客）发布，第二步的查询依然是以 `10008` 为起点往下查，完全不会受到新数据插入的干扰，实现了完美的无缝下拉刷新！

# 5. 实际实例演示
``` java
/**  
 * 查看用户关注列表的博客.feed流收件箱滚动刷新  
 * @param max  
 * @param offset  
 * @return  
 */  
@Override  
public Result queryBlogOfFollow(Long max, Integer offset) {  
    Long userId = UserHolder.getUser().getId();  
  
    //1.查询收件箱  
    String key = "feed:" + userId;  
    Set<ZSetOperations.TypedTuple<String>> typedTuples  
            = stringRedisTemplate.opsForZSet()  
              .reverseRangeByScoreWithScores(key, 0, max, offset, 2);//  
  
    if (typedTuples == null || typedTuples.isEmpty()) {  
        return Result.ok();  
    }  
  
    //2.解析数据,blogId,minTime(时间戳),offset  
    List<Long> ids = new ArrayList<>(typedTuples.size());  
    long minTime = 0;  
    int offsetFor = 1;  
    for(ZSetOperations.TypedTuple<String> tuple : typedTuples) {  
        //获取id  
       String idStr = tuple.getValue();  
       ids.add(Long.valueOf(idStr));  
       //获取时间戳(分数)  
        long time = tuple.getScore().longValue();  
        if(time==minTime){//时间戳相同，同一时间的博客，偏移量+1,防止重  
            offsetFor++;  
        }else{  
           minTime = time;//重置时间戳  
           offsetFor = 1;//重置为1  
        }  
    }  
  
    //3.根据blogId查询blog  
    String idStr = StrUtil.join(",", ids);  
    List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();  
  
  
    //4.返回数据  
    ScrollResult scrollResult = new ScrollResult();  
    scrollResult.setList(blogs);  
    scrollResult.setOffset(offsetFor);  
    scrollResult.setMinTime(minTime);  
  
    return Result.ok(scrollResult);  
}
```
## 解释
这段代码写得非常棒！它完美地将我们上一回合讨论的 `ZREVRANGEBYSCORE` 滚动分页逻辑，翻译成了 Java 和 Spring Boot 环境下的企业级代码。

为了让你彻底看透这段代码的精妙之处，我完全按照你的要求，采用**“上面一句代码，下面一段解释”**的方式为你进行深度拆解：

---

### 1. 返回值封装类

``` Java
public class ScrollResult {
    private List<?> list;     // 本次查询到的博客列表
    private Long minTime;     // 本次查询结果中的最小时间戳（留给下一次查询的 max）
    private Integer offset;   // 下一次查询的偏移量
}
```

**📝 解释：**

这是一个专门为滚动分页设计的 DTO（数据传输对象）。前端在进行下一次下拉刷新时，必须要带上这一页的最小时间戳和计算好的偏移量，所以后端必须把这三个数据打包一起返回给前端。

---

### 2. 方法入口与获取用户信息

``` Java
@Override
public Result queryBlogOfFollow(Long max, Integer offset) {
    Long userId = UserHolder.getUser().getId();
```

**📝 解释：**

- **方法参数：** `max` 是前端传来的时间戳（第一次是当前系统时间，后续是上一次返回的 `minTime`）；`offset` 是前端传来的偏移量（第一次是 0，后续是上一次返回的 `offset`）。
    
- **第一句代码：** 从当前线程（如 ThreadLocal）或者 Token 中提取当前正在操作的用户 ID。这是为了去 Redis 里找属于这个用户的专属“收件箱（Feed流）”。
    

---

### 3. 执行 Redis 核心查询

``` Java
    //1.查询收件箱
    String key = "feed:" + userId;
    Set<ZSetOperations.TypedTuple<String>> typedTuples
            = stringRedisTemplate.opsForZSet()
              .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
```

**📝 解释：**

这就是最核心的查询代码！它底层调用的正是 `ZREVRANGEBYSCORE key max 0 LIMIT offset 2 WITHSCORES` 命令。

- `key`: 拼接出该用户的收件箱键名。
    
- `.reverseRangeByScoreWithScores`: Spring Data Redis 提供的 API，对应逆序按分数范围查询，并且**把 Member（博客ID）和 Score（时间戳）一起查出来**。
    
- 参数 `0` 和 `max`: 对应分数范围，最小值为 0，最大值为前端传进来的 `max`。
    
- 参数 `offset` 和 `2`: 对应 `LIMIT offset count`。这里的 `2` 表示每页查询 2 条数据（实际项目中通常是 10 甚至更多，这里 2 可能是你为了测试方便写的）。
    

---

### 4. 判空兜底


``` Java
    if (typedTuples == null || typedTuples.isEmpty()) {
        return Result.ok();
    }
```

**📝 解释：**

良好的编码习惯。如果查询出来的结果是空的，说明用户的关注列表里没有新动态了，直接返回成功，避免后续代码出现空指针异常。

---

### 5. 数据解析准备工作

``` Java
    //2.解析数据,blogId,minTime(时间戳),offset
    List<Long> ids = new ArrayList<>(typedTuples.size());
    long minTime = 0;
    int offsetFor = 1;
```

**📝 解释：**

这里初始化了三个变量，用于接收和计算数据：

- `ids`: 用来收集从 Redis 中查出来的博客 ID 集合。
    
- `minTime`: 准备记录当前这一批数据中最小的时间戳。
    
- **`offsetFor = 1`**: 极其关键的细节！为什么初始值是 `1` 而不是 `0`？因为只要有一条数据，和最小时间戳相同的数据**保底就有 1 条**（也就是它自己）。随着后续循环比较，如果发现还有同样时间戳的数据，这个计数器就会往上加。
    

---

### 6. 遍历解析与核心逻辑计算
``` Java
    for(ZSetOperations.TypedTuple<String> tuple : typedTuples) {
        //获取id
       String idStr = tuple.getValue();
       ids.add(Long.valueOf(idStr));
       
       //获取时间戳(分数)
        long time = tuple.getScore().longValue();
```

**📝 解释：**

开始遍历 Redis 返回的这一批数据。把 Member (博客 ID) 转换成 Long 类型放进 `ids` 集合里，准备去数据库查详细信息。同时把 Score (时间戳) 提取出来。

``` Java
        if(time == minTime) { // 时间戳相同，同一时间的博客，偏移量+1, 防止重复
            offsetFor++;
        } else {
           minTime = time;  // 重置时间戳
           offsetFor = 1;   // 重置为1
        }
    }
```

**📝 解释：**

**这是整个方法中最烧脑、最巧妙的一段逻辑！** 它是用来计算返回给前端的下一次 `offset` 的。

因为 `typedTuples` 里的数据是从大到小降序排列的。循环执行到最后一次时，拿到的必定是这批数据里的**最小时间戳**。

- 如果当前取出的 `time` 和之前记录的 `minTime` 一样：说明在同一毫秒内发了多篇博客，我们要把跳过数量 `offsetFor` 加 1。
    
- 如果取出的 `time` 是一个新的（更小的）时间：说明进入了更早的时间段。此时把当前的 `time` 认作新的最小值记录在 `minTime` 中，同时把统计相同时间的计数器 `offsetFor` 重新归 `1`。
    
- **结果：当整个 for 循环结束时，`minTime` 必然是这批数据里的最小时间戳，而 `offsetFor` 必然是这批数据里“时间戳等于 minTime”的元素总个数。**
    

---

### 7. 数据库查询与排序保真
``` Java
    //3.根据blogId查询blog
    String idStr = StrUtil.join(",", ids);
    List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
```

**📝 解释：**

拿到这批博客的 ID 后，要去 MySQL 数据库里查出博客的具体内容（标题、内容、作者等）。

- `.in("id", ids)`: 用 SQL 的 `IN (id1, id2...)` 语法批量查询，减少数据库网络 IO。
    
- **`.last("ORDER BY FIELD(id," + idStr + ")")`**: 这一句非常有含金量！在 MySQL 中，如果用 `IN` 查询，返回的数据默认是按数据库主键索引排序的，**这会破坏我们在 Redis 中辛辛苦苦排好的时间倒序！** 使用 `ORDER BY FIELD` 可以强行要求 MySQL 按照我们传入的 ID 字符串的顺序（也就是 Redis 里的倒序顺序）来组装返回结果。
    

---

### 8. 封装并返回结果

``` Java
    //4.返回数据
    ScrollResult scrollResult = new ScrollResult();
    scrollResult.setList(blogs);
    scrollResult.setOffset(offsetFor);
    scrollResult.setMinTime(minTime);

    return Result.ok(scrollResult);
}
```

**📝 解释：**

将查到的博客实体列表 `blogs`、刚刚在循环里费尽心思算出来的下次使用的 `minTime` 和 `offsetFor` 组装进 `ScrollResult` 对象中，最后统一包装成通用返回格式给前端。前端拿到后，就可以无缝衔接下一次的分页请求了。
