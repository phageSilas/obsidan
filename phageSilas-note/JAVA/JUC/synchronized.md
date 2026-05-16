下面介绍 `synchronized`，覆盖原理、使用方式、锁释放机制以及**中断行为**这几个你关心的重点。

---

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