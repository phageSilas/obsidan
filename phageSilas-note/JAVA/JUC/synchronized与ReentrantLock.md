
# synchronized
## 一、底层原理：对象头、Monitor 与锁升级

每个 Java 对象在堆中都有**对象头**，其中的 **Mark Word** 记录了锁状态。`synchronized` 依赖操作系统底层的 **Monitor（管程）** 来实现，但 JVM 会通过锁升级优化性能：

### 1. 对象头与 Monitor
- **Mark Word**：在无锁、偏向锁、轻量级锁、重量级锁状态下，存储不同信息（如线程 ID、指向栈中锁记录的指针、指向重量级 Monitor 对象的指针等）。
- **重量级 Monitor**：当锁膨胀到重量级时，Mark Word 指向一个 C++ 的 `ObjectMonitor` 对象，包含：
  - `_owner`：当前持有锁的线程。
  - `_EntryList`：等待获取锁的阻塞线程队列（BLOCKED）。
  - `_WaitSet`：调用了 `wait()` 的线程队列（WAITING）。

### 2. 锁升级过程（偏向锁 → 轻量级锁 → 重量级锁）
- **偏向锁**（Biased Lock）：认为锁总是由同一个线程获取，首次获得时在 Mark Word 记录线程 ID，后续该线程进入无需 CAS 操作。当有其他线程竞争时撤销偏向。
- **轻量级锁**（Lightweight Lock）：通过 CAS 将 Mark Word 指向当前线程栈帧中的 Lock Record。失败则自旋等待，避免内核态切换。
- **重量级锁**（Heavyweight Lock）：自旋到一定次数仍未获得锁，则膨胀为重量级锁，未获锁的线程进入 `_EntryList` 并阻塞（BLOCKED），由操作系统调度。

**所以，synchronized 不是一开始就很重，而是在竞争激烈时才升级为内核态的互斥量。**

---

## 二、三种使用方式与锁对象选择

```java
// 1. 实例方法 —— 锁是当前实例对象 this
public synchronized void instanceMethod() {}

// 2. 静态方法 —— 锁是当前类的 Class 对象
public static synchronized void staticMethod() {}

// 3. 同步代码块 —— 可任意指定锁对象
Object lock = new Object();
synchronized(lock) {
    // 同步体
}
```

**关键建议**：
- 避免用字符串常量或基本类型包装类（如 `String`、`Integer`）做锁，因为 JVM 的缓存机制可能导致不同代码块无意间共用同一个锁。
- 使用私有 `final` 对象作为锁，防止外部修改引用。
- `synchronized` 是**可重入锁**：**同一线程**在外层获得锁后，内层可直接进入，不会自己阻塞自己。

---

## 三、锁释放的完整时机

你已经知道“执行完就释放”，这里更精确地总结：

| 时机 | 是否释放锁 | 线程状态变化 |
|------|------------|--------------|
| 正常执行完同步块 / 方法体 | ✅ 立即释放 | RUNNABLE → 释放后由 JVM 调度 |
| 代码块内抛出未捕获异常 | ✅ 释放（自动） | 异常传播，退出同步块后释放 |
| 调用锁对象的 `wait()` | ✅ **暂时释放** | 当前线程进入 `_WaitSet`（WAITING），释放锁，等待被 `notify`/`notifyAll` |
| 线程执行完毕（run 结束） | ✅ 隐式释放 | 线程 TERMINATED，持有的所有锁释放 |
| 调用 `Thread.sleep()` / `join()` 等 | ❌ **不释放** | 线程仍持有锁，其他线程无法进入 |

**Happens-Before 保证**：一个线程释放锁，与另一个随后获取同一个锁的线程之间，存在 happens-before 关系。即：前者的所有写操作对后者可见。这是可见性的根源。

---

## 四、中断机制：synchronized 与线程中断的交互

这是 `synchronized` 与 JUC 显式锁（如 `ReentrantLock`）的核心区别之一。

### 1. **在等待获取锁时（阻塞在 entry）**
`synchronized` **无法响应中断**。  
当一个线程尝试进入同步块，而锁已被别的线程持有，它会进入 BLOCKED 状态，挂在 Monitor 的 `_EntryList` 上。  
- 此时若其他线程调用该线程的 `interrupt()`，**不会抛出 `InterruptedException`**。
- 只是把线程的中断标志位置为 `true`，但线程**依然保持阻塞**，必须等到获取锁之后，才能检测到中断标志。
- 这意味着**你无法通过中断让一个等待 `synchronized` 锁的线程提前放弃等待**。

**对比**：`ReentrantLock.lockInterruptibly()` 允许线程在等待锁期间响应中断，立即抛出 `InterruptedException` 并退出等待。

### 2. **在同步块内部**
线程已持有锁时，可以检测自己的中断状态：
```java
synchronized (lock) {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
}
```
或者调用会响应中断的方法（如 `wait()`、`sleep()`），此时会抛出 `InterruptedException`。

### 3. **`wait()` 期间的中断**
在同步块内调用 `lock.wait()` 会释放锁并进入 `_WaitSet`，此时线程处于 WAITING 状态。  
如果此时被中断，会**立即抛出 `InterruptedException`，并从 `wait()` 方法返回**，但注意：  
- 线程需要在抛出异常后**重新获取锁**才能继续执行（因为 `wait()` 必须在同步块内，异常会退出同步块，但锁在抛出异常前已被释放，退出块时锁是释放的）。
- 抛出异常后如果继续执行 catch 块，需要再次进入同步块才持有锁。

### 4. 代码演示：中断对 synchronized 的无效性
```java
Object lock = new Object();
Thread t = new Thread(() -> {
    System.out.println("尝试获取锁...");
    synchronized (lock) {  // 如果锁被占用，这里将阻塞，且无法被中断
        System.out.println("获得锁，开始执行");
    }
});

// 让主线程先占有锁
synchronized (lock) {
    t.start();
    Thread.sleep(100);  // 确保 t 已进入 BLOCKED
    t.interrupt();      // 尝试中断 t
    System.out.println("主线程调用了 interrupt，t 的中断状态: " + t.isInterrupted());
}
// 主线程释放锁后，t 才能获得锁并继续，然后结束
```
输出类似：
> 尝试获取锁...
> 主线程调用了 interrupt，t 的中断状态: true
> 获得锁，开始执行

可以看到，即使被中断，线程依然等待并最终获得锁，并没有提前放弃。这就是 `synchronized` 不可中断等待的特性。

---

## 五、与 JUC Lock 的核心对比

| 特性    | synchronized      | Lock（如 ReentrantLock）                      |
| ----- | ----------------- | ------------------------------------------ |
| 类型    | JVM 关键字，隐式        | java.util.concurrent.locks 接口，显式           |
| 获取锁方式 | 自动，不支持超时、可中断      | `lock()`、`tryLock()`、`lockInterruptibly()` |
| 等待可中断 | ❌ 不可中断            | ✅ `lockInterruptibly()` 支持                 |
| 公平性   | 非公平（仅一种）          | 支持公平 / 非公平                                 |
| 条件队列  | 单一（`wait/notify`） | 多个 `Condition`                             |
| 释放    | 自动（正常/异常结束）       | 必须在 `finally` 中手动 `unlock()`               |
| 性能    | 无竞争时偏向锁极快         | 在高竞争下通常更稳定                                 |

---

# 可重入
## 什么是可重入

**可重入**（Reentrant）是指：**同一个线程在外层方法获得锁之后，内层方法可以直接再次获得同一把锁，而不会被自己阻塞**。如果没有可重入性，线程会在第二次请求同一把锁时陷入死锁——因为自己已经持有了锁，却还在等待自己释放。

Java 中的 `synchronized` 是内置可重入的，JUC 中的 `ReentrantLock` 从名字就能看出来也是可重入的。

---

## 为什么需要可重入

考虑一个常见场景：一个类的多个同步方法互相调用。例如：

```java
public class Counter {
    public synchronized void increment() {
        // ...
    }

    public synchronized void reset() {
        // 重置逻辑中需要先记录当前值，然后调用 increment 初始化
        increment();
    }
}
```

如果 `synchronized` 不是可重入的，当线程执行 `reset()` 时已经持有 `this` 锁，再调用 `increment()` 时会尝试再次获取同一个锁，导致自己被阻塞，形成**死锁**。可重入性正好避免了这个问题。

---

## `synchronized` 如何实现可重入

JVM 底层在每个 Monitor（管程）中记录：
- **持有者线程 ID**（`_owner`）
- **重入计数**（`_recursions`）

当线程进入同步块时：
1. 如果 `_owner` 为 `null`，通过 CAS 将自己设为 `_owner`，`_recursions = 1`。
2. 如果 `_owner` 就是当前线程，`_recursions++`，直接进入。
3. 否则阻塞等待。

退出同步块时：
1. `_recursions--`。
2. 当 `_recursions == 0` 时，说明完全退出最外层，将 `_owner` 设为 `null`，唤醒等待队列中的线程。

这保证了同一线程多次进入同一把锁的同步代码块时，不会被自己阻塞。

---

## 可重入的简单例子

```java
public class ReentrantExample {
    // 锁对象
    private final Object lock = new Object();

    public void outer() {
        System.out.println(Thread.currentThread().getName() + " 进入 outer");
        synchronized (lock) {
            System.out.println(Thread.currentThread().getName() + " 获得锁，执行 outer");
            // 调用内层同步方法
            inner();
            System.out.println(Thread.currentThread().getName() + " 退出 outer");
        }
    }

    public void inner() {
        System.out.println(Thread.currentThread().getName() + " 尝试进入 inner");
        synchronized (lock) {
            // 这里实际上会再次获取同一把锁，因为可重入不会阻塞
            System.out.println(Thread.currentThread().getName() + " 获得锁，执行 inner");
        }
        System.out.println(Thread.currentThread().getName() + " 退出 inner");
    }

    public static void main(String[] args) {
        ReentrantExample example = new ReentrantExample();
        example.outer();
    }
}
```

**输出**：
```
main 进入 outer
main 获得锁，执行 outer
main 尝试进入 inner
main 获得锁，执行 inner
main 退出 inner
main 退出 outer
```

**分析**：线程 `main` 在 `outer()` 中拿到 `lock` 锁后，调用 `inner()`，`inner()` 再次需要 `lock` 锁。因为线程相同、锁相同，JVM 允许直接重入，不会发生死锁。

---

## 用 `ReentrantLock` 展示可重入

```java
import java.util.concurrent.locks.ReentrantLock;

public class LockReentrantExample {
    private final ReentrantLock lock = new ReentrantLock();

    public void outer() {
        lock.lock();
        try {
            System.out.println("outer");
            inner();
        } finally {
            lock.unlock();
        }
    }

    public void inner() {
        lock.lock();
        try {
            System.out.println("inner");
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        new LockReentrantExample().outer();
    }
}
```

输出：
```
outer
inner
```

注意：`lock()` 和 `unlock()` 必须成对出现，且重入多少次就要解锁多少次，否则锁不会释放。`synchronized` 隐式处理，更简洁。

---

## 不可重入会怎样？（假想场景）

如果一个锁不可重入，上述 `outer()` 调用 `inner()` 时，线程已经持有锁，但第二次 `lock.lock()` 会被判定为自己还没释放，于是将自己阻塞，导致**永久的死锁**。

---

## 总结

- **可重入**就是同一个线程可以多次获得它已经持有的锁。
- `synchronized` 和 `ReentrantLock` 都支持可重入。
- 它解决了子类方法调用父类同步方法、一个同步方法调用另一个同步方法等场景的死锁问题。
- JVM 通过 Monitor 的持有者和重入计数器实现；`ReentrantLock` 则通过 `AbstractQueuedSynchronizer` 的 `state` 和 `exclusiveOwnerThread` 实现。

# ReentrantLock

## 一、ReentrantLock 的原理（AQS 基础）

`ReentrantLock` 的实现依赖于 **AQS（AbstractQueuedSynchronizer，队列同步器）**。

- **核心字段**：
  - `state`（int 类型）：表示锁的重入次数。0 代表锁空闲，大于 0 代表被持有。
  - `exclusiveOwnerThread`：记录当前持有锁的线程。
  - **CLH 队列**（双向链表）：存放等待获取锁的线程节点。

- **内部类**：`Sync` 继承 AQS，又有两个子类：
  - **NonfairSync（非公平锁，默认）**：新来的线程可以直接参与抢锁（插队），不一定按等待顺序。
  - **FairSync（公平锁）**：严格按 CLH 队列顺序获取锁，先到先得。

- **获取锁 `lock()` 的过程（非公平）**：
  1. CAS 尝试将 `state` 从 0 改为 1。
  2. 成功：设置 `exclusiveOwnerThread` 为自己，获取锁。
  3. 失败：调用 `acquire(1)`，内部再尝试一次，若仍失败，则将该线程封装成 Node 加入 CLH 队列尾部，并**自旋或阻塞（park）**。
  4. 可重入实现：如果当前线程就是 `exclusiveOwnerThread`，则 `state++`，直接成功。

- **释放锁 `unlock()` 的过程**：
  1. `state--`，当 `state == 0` 时，将 `exclusiveOwnerThread` 置为 null。
  2. 唤醒 CLH 队列中的头节点的后继线程（unpark），该线程会再次尝试获取锁。

- **公平锁的区别**：获取锁前先检查 CLH 队列中是否有等待更久的线程，有就直接排队。

---

## 二、ReentrantLock 的使用

### 1. 基本用法（必须手动释放）
```java
Lock lock = new ReentrantLock();
lock.lock();   // 获取锁，不可中断，如果没拿到会一直阻塞
try {
    // 临界区代码
} finally {
    lock.unlock(); // 绝对要在 finally 中释放，避免死锁
}
```

### 2. 可中断获取锁 `lockInterruptibly()`
```java
lock.lockInterruptibly(); // 等待锁期间若被 interrupt()，立即抛出 InterruptedException
try {
    // ...
} finally {
    lock.unlock();
}
```
这是 `synchronized` 做不到的：等待 `synchronized` 的线程对中断“无感”。

### 3. 尝试非阻塞获取锁 `tryLock()`
```java
if (lock.tryLock()) {  // 立即返回，拿不到锁就返回 false
    try {
        // ...
    } finally {
        lock.unlock();
    }
} else {
    // 做别的事情，而不是傻等
}
```
还有带超时参数的 `tryLock(time, unit)`，可以等待指定时间。

### 4. 多条件等待 `Condition`
```java
Lock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();
Condition notEmpty = lock.newCondition();
```
一个 `ReentrantLock` 可以创建多个 `Condition`，实现**精确唤醒**（比如生产-消费者模型，生产者只唤醒消费者，而不是用 `notifyAll()` 唤醒所有线程，提高效率）。而 `synchronized` 只有一个隐式条件队列（`wait/notify/notifyAll`）。

---

## 三、ReentrantLock 的特点

| 特点               | 说明                                                           |
|---------------------|----------------------------------------------------------------|
| **可重入**          | 同 `synchronized`，同一线程可多次获取，`state` 递增。           |
| **可中断获取**      | `lockInterruptibly()` 允许等待锁的线程响应中断。                |
| **超时尝试**        | `tryLock(timeout)` 避免无限期阻塞。                            |
| **公平性可选**      | 构造器 `ReentrantLock(true)` 创建公平锁，减少线程饥饿。        |
| **多条件变量**      | 一个锁可绑定多个 `Condition`，粒度更细的线程协作。              |
| **显式控制**        | 必须手动 `unlock()`，灵活性高但易出错。                         |

---

## 四、ReentrantLock 与 synchronized 全面对比

| 对比维度             | synchronized                         | ReentrantLock                                   |
|----------------------|--------------------------------------|--------------------------------------------------|
| **实现类型**         | JVM 关键字，底层通过 monitor 实现    | JDK 类，基于 AQS（纯 Java 代码）                |
| **锁获取方式**       | 隐式，线程自动获取                   | 显式，必须调用 `lock()` 等方法                  |
| **锁释放**           | 自动（代码块结束或异常）             | 必须手动在 `finally` 中 `unlock()`              |
| **等待可中断**       | **不支持**，阻塞时无法响应中断       | 支持（`lockInterruptibly`）                     |
| **超时尝试**         | 不支持，会一直等                     | 支持（`tryLock(timeout)`）                      |
| **公平性**           | 默认非公平，不可配置                 | 可选公平 / 非公平（默认非公平）                 |
| **条件队列**         | 只有一个隐式条件队列（wait/notify）  | 多个 `Condition`，可精细控制唤醒                |
| **性能（低竞争）**   | 偏向锁/轻量级锁，性能极佳            | 需要 CAS 和 park 操作，开销稍大                 |
| **性能（高竞争）**   | 升级为重量级锁，性能下降             | 通常优于重量级锁，但差距已缩小                   |
| **可重入**           | 是                                   | 是                                               |
| **API 丰富度**       | 只有同步块/方法，较单一              | 功能丰富，如 `getHoldCount` 等监测方法           |
| **死锁检测**         | 难以检测                             | 可通过 `tryLock` 避免，或利用 AQS 状态排查       |
| **使用风险**         | 极低，不会忘记释放                   | 忘了 `unlock` 会导致死锁，必须规范使用           |

---

## 五、优缺点总结

### synchronized 的优点
- **简单安全**：代码退出自动释放，异常也释放，不会有忘掉 `unlock` 的风险。
- **性能优化**：JVM 做了大量锁优化（偏向锁、轻量级锁），无竞争时近乎零开销。
- **代码简洁**：关键字直接使用，语义清晰。

### synchronized 的缺点
- **功能有限**：不能中断等待，不能超时尝试，不能指定公平性。
- **条件队列单一**：只能 `wait/notifyAll`，可能造成虚假唤醒通知所有线程，开销大。
- **黑盒**：无法得知锁状态、等待队列长度等调试信息。

### ReentrantLock 的优点
- **功能强大**：可中断、可超时、公平锁、多条件变量，适合复杂同步场景。
- **灵活控制**：可以实现更精细的同步策略（例如分段锁）。
- **可观测**：提供 `isLocked`, `getQueueLength` 等 API 便于监控。

### ReentrantLock 的缺点
- **使用复杂**：必须显式上锁解锁，忘记 `unlock` 就可能埋下死锁隐患。
- **代码冗长**：每次都要用 `try-finally` 包裹。
- **性能开销**：在竞争极低时，偏向锁的 `synchronized` 更快；Lock 的 CAS 和 park 有原生开销。

---

### 选择建议
- 绝大多数**简单同步需求**，直接用 `synchronized`，代码更安全。
- 如果需要**可中断的锁获取、超时等待、公平锁、多条件协作**，使用 `ReentrantLock`。
- 在 **JDK 6 之后**，两者在一般场景下性能差距很小，优先选写法简单的，需要高级特性时再用 `Lock`。

如果你还想看 `Condition` 的生产者-消费者完整示例，或者 AQS 的实现细节，我可以继续展开。