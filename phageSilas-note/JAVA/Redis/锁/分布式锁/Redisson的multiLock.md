在真实的复杂业务场景中，我们经常会遇到**“一次操作需要同时锁定多个资源”**的情况。如果处理不好，极易引发经典的**死锁**问题。

Redisson 的 `MultiLock` 就是专门为了解决这种“多资源并发锁定”而诞生的。它的核心哲学非常简单霸气：**“要么全拿到，要么全不要（All or Nothing）”**。

---

# 1. 痛点场景：为什么需要 MultiLock？

**经典场景：电商大促，合并支付 / 购物车结算** 假设用户购物车里有 3 件不同的商品（商品 A、商品 B、商品 C），用户点击“一起结算”。为了保证不超卖，后台必须**同时扣减**这 3 件商品的库存。

**传统写法的致命死锁危机：** 如果你用普通的 RedissonLock，可能会这么写代码：

Java

```
lockA.lock();
lockB.lock();
lockC.lock();
// 执行扣减逻辑...
```

**灾难发生：**

- 线程 1 正在结算 A 和 B。它先锁住了 A，正准备去锁 B。
    
- 线程 2 正在结算 B 和 A。它先锁住了 B，正准备去锁 A。
    
- 结果：线程 1 拿着 A 等 B，线程 2 拿着 B 等 A。**死锁诞生，系统卡死！**
    

---

# 2. MultiLock 的核心机制与破解之法

为了破解这种多资源交叉锁定的死锁，Redisson 提供了 `RedissonMultiLock` 对象。你可以把多个互不相干的锁，打包成一个“超级锁”。

## A. 代码怎么写？

极其简单，只需把单锁塞进联锁对象中：

Java

```
RLock lock1 = redissonClient.getLock("lock:product:A");
RLock lock2 = redissonClient.getLock("lock:product:B");
RLock lock3 = redissonClient.getLock("lock:product:C");

// 把三个锁合并为一个联锁
RedissonMultiLock multiLock = new RedissonMultiLock(lock1, lock2, lock3);

try {
    // 尝试加锁。这里的逻辑是：必须 3 个锁全部拿到，才算成功！
    // 假设设置等待时间 10 秒，持有时间 30 秒
    boolean isLock = multiLock.tryLock(10, 30, TimeUnit.SECONDS);
    
    if (isLock) {
        System.out.println("成功锁定 A、B、C 三个商品的库存！");
        // 执行合并扣减库存逻辑...
    } else {
        System.out.println("获取联锁失败，有其他线程正在操作其中某个商品");
    }
} finally {
    // 一键释放所有锁
    multiLock.unlock();
}
```

## B. 底层机制：它是如何防止死锁的？

MultiLock 的底层并没有什么黑魔法（比如去 Redis 里发一条同时改三个 Key 的命令，Redis 跨 slot 时不支持这么做），它的本质是在 Java 客户端做了一个**极其严谨的 `for` 循环 + 失败回滚机制**。

它的核心流程如下：

1. **遍历获取：** 拿着传入的锁列表 `[lockA, lockB, lockC]`，按顺序挨个去 Redis 发 Lua 脚本尝试加锁。
    
2. **中途失败怎么办？（这是核心）** 假设成功拿到了 A 和 B，但在尝试拿 C 的时候，发现 C 被别人占了。
    
    - **MultiLock 绝对不会拿着 A 和 B 傻等！**
        
    - 它会**立刻主动释放**已经拿到的 A 和 B（回滚）。
        
    - 然后休眠一小段随机时间，重新从 A 开始一轮新的抢锁尝试。
        
3. **全部成功：** 只有当 `[lockA, lockB, lockC]` 全部成功获取时，`multiLock.lock()` 才会向业务层返回成功。
    
4. **一键释放：** 当业务调用 `multiLock.unlock()` 时，底层再次遍历列表，把所有的锁挨个释放。
    

**看门狗（WatchDog）支持：** 如果你在加锁时不传 `leaseTime`（持有时间），MultiLock 底层依然会为每一个拿到的锁挂上 WatchDog 定时任务，保证业务没跑完前，所有的锁都不会自动过期。

---

# 3. 源码骨架一瞥 (加深理解)

从源码的骨架中，你可以清晰地看到它的**“失败清理”**逻辑：

Java

```
// RedissonMultiLock 底层 tryLock 的精简逻辑
public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
    long newLeaseTime = -1;
    if (leaseTime != -1) {
        newLeaseTime = unit.toMillis(leaseTime);
    }
    
    long time = System.currentTimeMillis();
    long remainTime = unit.toMillis(waitTime); // 剩余的全局等待时间
    
    // acquiredLocks 用于记录已经成功拿到的锁
    List<RLock> acquiredLocks = new ArrayList<>(locks.size());
    
    // 1. 遍历所有锁
    for (RLock lock : locks) {
        // 去拿当前的锁
        boolean lockAcquired = lock.tryLock(remainTime, newLeaseTime, TimeUnit.MILLISECONDS);
        
        if (lockAcquired) {
            // 拿到了，加进成功列表
            acquiredLocks.add(lock);
        } else {
            // 🚨 核心逻辑：拿到一半失败了！
            // 2. 清理战场：把之前辛苦拿到的锁全部释放掉！
            unlockInner(acquiredLocks);
            
            // 宣告本次尝试失败，外层死循环会休眠后重新触发这段逻辑
            return false; 
        }
        
        // 扣减剩余的总等待时间，如果超时了，也要走失败清理逻辑 (源码中有，此处精简)
        remainTime -= (System.currentTimeMillis() - time);
    }
    
    // 3. 顺利走完 for 循环，说明全部拿到！返回成功！
    return true;
}
```

---

# 4. 延伸：MultiLock 和 RedLock 的关系？

如果你听说过 Redis 分布式锁，一定听过 Redis 作者 antirez 提出的 **RedLock（红锁）** 算法。

实际上，在 Redisson 的源码中，`RedissonRedLock` 就是直接继承自 `RedissonMultiLock` 的。

- **MultiLock（联锁）：** 解决业务问题。面对的是**不同**的资源（锁A、锁B、锁C）。要求必须 **100%（全部）** 拿到才算成功。
    
- **RedLock（红锁）：** 解决高可用问题。面对的是**同一个**资源，只不过为了防备 Redis 节点宕机，故意向 5 个完全独立的主节点去申请这同一把锁。要求只要拿到 **N/2 + 1（多数派，比如 3 个）** 就算成功。
    

> **⚠️ 实战排雷提示：** 目前业界其实**非常不推荐使用 RedLock**。因为它不仅部署极其繁琐（需要多套独立 Redis），而且被著名分布式专家 Martin Kleppmann 证明了在极端网络分区和时钟跳跃下依然存在安全性漏洞。
> 
> 真实的生产环境里，如果你追求极端的锁一致性，应该直接换用 Zookeeper 或 etcd（强一致性 CP 模型）。
> 
> 但 **MultiLock 是非常有用的**，尤其是在订单结算、库存调拨等涉及多个业务资源的场景，它依然是防死锁的最佳实践。