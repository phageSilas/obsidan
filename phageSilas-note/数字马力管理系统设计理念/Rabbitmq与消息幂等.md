RabbitMQ 在你负责的 HRMS 模块里很适合用，而且比 Redis 锁更容易体现“系统解耦、削峰、异步化、最终一致性”。

但有一点要先纠正：**RabbitMQ 一般不直接“发送给前端”**。更常见设计是：

```
后端任务完成 -> 发送 MQ 消息 -> 消费者处理结果 -> 写数据库 / Redis
前端通过轮询 / WebSocket / SSE 查询任务进度或接收通知
```

也就是说，RabbitMQ 负责后端异步流程，前端通过接口或推送通道感知结果。

## 考勤模块适合用 MQ 的地方

### 1. 月度考勤统计异步生成

这是最适合 MQ 的点。

场景：

HR 点击“生成月度考勤统计”，或者每月 1 日定时任务触发。统计全公司员工考勤是重任务，不适合接口同步等待。

流程可以设计成：

```
HR 点击生成统计
-> 后端创建统计任务记录
-> 投递 MQ：attendance.stat.generate
-> 前端展示“统计中”
-> 消费者按员工/部门分片统计
-> 写入 hr_attendance_month_stat
-> 更新任务状态为成功/失败
-> 前端轮询任务状态或 WebSocket 通知完成
```

消息体示例：

```
{
  "messageId": "ATT_STAT_202607_001",
  "statMonth": "2026-07",
  "deptId": 1001,
  "operatorId": 1,
  "traceId": "xxx"
}
```

亮点：

- 避免接口超时
- 削峰，防止一次性扫全表
- 支持失败重试
- 支持分片并行统计
- 支持前端展示进度

### 2. 打卡后异步刷新考勤状态

员工打卡主链路要快，不建议同步做太多事情。

可以这样：

```
员工打卡成功
-> 写入 hr_attendance_record
-> 发送 MQ：attendance.clock.created
-> 异步刷新当天考勤状态缓存
-> 异步检查异常打卡
-> 异步生成通知
```

适合的异步动作：

- 刷新 `attendance:record:daily:{employeeId}:{date}` 缓存
- 判断是否迟到 / 早退 / 缺卡
- 写异常日志
- 通知主管或 HR

### 3. 补卡 / 请假审批结果异步同步考勤统计

请假审批通过后，考勤统计需要更新请假天数；补卡审批通过后，原本缺卡状态也需要修正。

流程：

```
审批中心审批通过
-> 发送 MQ：approval.result.changed
-> 考勤消费者接收
-> 判断业务类型 LEAVE / CORRECTION
-> 更新请假记录 / 补卡记录
-> 刷新考勤记录和月度统计缓存
```

这个设计很高级，因为它体现了模块解耦：

- 审批中心不直接侵入考勤逻辑
- 考勤模块通过订阅审批结果维护自身状态
- 后续薪资模块也可以订阅考勤统计完成消息

## 薪资模块适合用 MQ 的地方

### 1. 薪资批次异步核算

薪资批次计算也非常适合 MQ。

```
HR 点击开始计算
-> 创建/更新批次状态为 CALCULATING
-> 投递 MQ：salary.batch.calculate
-> 前端展示“计算中”
-> 消费者读取员工薪资档案、考勤统计、账套规则
-> 生成 salary_batch_item
-> 异常检测
-> 更新批次状态为 PENDING_CONFIRM
```

这个比同步计算好很多，因为薪资核算涉及：

- 员工薪资档案
- 考勤月度汇总
- 个税计算
- 社保公积金
- 异常检测
- 批量写明细

如果同步接口做，容易超时，也不好重试。

### 2. 薪资批次分片计算

如果员工数量多，可以拆消息：

```
salary.batch.calculate
-> 拆分为多个 salary.batch.calculate.shard
-> 每个分片处理 200 个员工
-> 所有分片完成后汇总批次状态
```

消息示例：

```
{
  "messageId": "SAL_CALC_202607_001_03",
  "batchId": 202607001,
  "salaryMonth": "2026-07",
  "shardIndex": 3,
  "shardTotal": 10,
  "employeeIds": [101, 102, 103]
}
```

亮点：

- 控制单次事务大小
- 提高计算吞吐
- 单分片失败可以局部重试
- 避免一个大任务失败后全部重来

### 3. 工资条生成与通知

薪资审批通过或确认发放后，可以异步生成工资条。

```
薪资批次确认发放
-> 发送 MQ：salary.payslip.generate
-> 消费者生成 hr_payslip
-> 发送 MQ：salary.payslip.notify
-> 通知员工工资条已开放
```

前端“我的工资条”只查询 `visible_status=1` 的工资条。

### 4. 薪资看板数据异步生成

薪资可视化看板聚合成本较高，可以异步生成 Redis 数据集。

```
薪资批次计算完成
-> 发送 MQ：salary.visualization.generate
-> 消费者聚合薪资趋势、部门分布、构成占比
-> 写入 Redis
-> 前端打开看板时优先读缓存
```

## MQ 幂等怎么设计

这个一定要写进系分里，面试也很好讲。
![[image.png]]
### 1. 每条消息必须有全局唯一 messageId

比如：

```
ATT_STAT_202607_DEPT_1001
SAL_BATCH_202607001_SHARD_03
PAYSLIP_GENERATE_202607001
```

消费者处理前先查是否处理过。

可以建一张表：

```
hr_mq_consume_log
```

核心字段：

```
message_id     消息唯一ID
biz_type       业务类型
biz_id         业务ID
consume_status 处理状态：PROCESSING / SUCCESS / FAILED
retry_count    重试次数
error_msg      错误信息
create_time
update_time
```

消费逻辑：

```
收到消息
-> 根据 messageId 插入消费日志
-> 如果唯一键冲突，说明已消费或处理中，直接跳过
-> 执行业务逻辑
-> 成功后更新 consume_status = SUCCESS
-> 失败则记录 FAILED，交给重试或死信队列
```

### 2. 数据库唯一约束兜底

比如月度考勤统计：

```
employee_id + stat_month + is_deleted
```

薪资明细：

```
batch_id + employee_id
```

工资条：

```
employee_id + salary_month + is_deleted
```

即使 MQ 重复投递，也不会重复生成数据。

### 3. 业务状态机防重复

比如薪资批次：

```
只有 DRAFT / REJECTED 才能进入 CALCULATING
只有 CALCULATING 才能写入计算结果
只有 PENDING_CONFIRM 才能提交审批
只有 APPROVED 才能确认发放
```

消费者拿到消息后先查状态：

```
如果批次已 PAID，直接丢弃消息
如果批次不是 CALCULATING，不执行核算
```

这就是业务幂等。

### 4. Redis 去重适合做短期防抖，不适合做唯一保障

可以用：

```
mq:consume:{messageId}
```

```
SETNX mq:consume:{messageId} 1 EX 10min
```

但它只能作为短期快速拦截，最终还是要靠数据库唯一约束和消费日志表兜底。

## 可以写进你模块系分的高级设计

你可以这样组织：

```
RabbitMQ 异步任务设计：
1. 考勤月度统计异步生成
2. 薪资批次异步核算
3. 审批结果异步回写考勤/薪资状态
4. 工资条异步生成与通知
5. 薪资看板数据异步聚合
```

幂等设计：

```
1. messageId 全局唯一
2. 消费日志表记录处理状态
3. 业务表唯一索引兜底
4. 状态机校验防止重复推进
5. Redis SETNX 做短期重复消费拦截
6. 失败消息进入重试队列，超过次数进入死信队列
```

## 我最推荐你重点包装的 3 个点

1. **考勤月度统计异步化**
    
    - RabbitMQ 投递统计任务
    - 分片统计员工考勤
    - 结果写入月度统计表
    - 前端轮询任务进度
2. **薪资批次异步核算**
    
    - 创建批次后异步计算
    - 分片处理员工薪资
    - 异常检测后更新批次状态
    - 幂等消费防止重复算薪
3. **工资条异步生成**
    
    - 薪资确认发放后投递工资条生成消息
    - 批量生成工资条
    - 员工查看前二次验证
    - 查看日志审计

这三个点和你的模块贴合度最高，也最容易讲出技术深度。