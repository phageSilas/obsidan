可以用，而且这些 Redis “特色数据结构”比单纯缓存更容易讲出亮点。你的两个模块里，**考勤模块最适合用 Redis 的 Bitmap、GEO、Set、ZSet、Stream；薪资模块适合用 ZSet、Hash、Set、Stream，但要避开长时间缓存敏感金额明细**。

下面按 Redis 能力说。

## 1. Bitmap：打卡、补签、出勤日历

Bitmap 最适合做“某天是否发生过某行为”。

### 考勤模块：员工月度打卡日历

比如一个员工一个月每天是否打卡：

```
attendance:bitmap:clock:{employeeId}:{yyyyMM}
```

第几天作为 offset：

```
offset = dayOfMonth - 1
```

员工 7 月 11 日打卡成功：

```
SETBIT attendance:bitmap:clock:1001:202607 10 1
```

查询 7 月 11 日是否打卡：

```
GETBIT attendance:bitmap:clock:1001:202607 10
```

统计本月已打卡天数：

```
BITCOUNT attendance:bitmap:clock:1001:202607
```

适合功能：

- 我的考勤日历
- 月度出勤天数统计
- 判断是否缺卡
- 补卡后快速更新出勤状态

### 考勤模块：补卡状态 Bitmap

可以单独记录补卡成功的日期：

```
attendance:bitmap:correction:{employeeId}:{yyyyMM}
```

补卡审批通过后：

```
SETBIT attendance:bitmap:correction:1001:202607 10 1
```

前端日历展示时可以区分：

```
打卡 bitmap = 1，补卡 bitmap = 0 -> 正常打卡
打卡 bitmap = 1，补卡 bitmap = 1 -> 补卡成功
打卡 bitmap = 0 -> 缺卡
```

这个就可以包装成：**用 Bitmap 实现个人月度考勤状态快速判定**。

### 考勤模块：请假日历 Bitmap

请假审批通过后，也可以记录请假日期：

```
attendance:bitmap:leave:{employeeId}:{yyyyMM}
```

适合做：

- 日历标记请假日
- 月度请假天数快速统计
- 薪资模块读取考勤统计前做辅助判断

注意：如果支持 0.5 天请假，Bitmap 只能表示“是否请假”，不能表达上午 / 下午半天。可以扩展成两个 bitmap：

```
attendance:bitmap:leave:am:{employeeId}:{yyyyMM}
attendance:bitmap:leave:pm:{employeeId}:{yyyyMM}
```

## 2. GEO：打卡地点范围校验

GEO 非常适合你的考勤打卡。

### 考勤模块：办公地点地理信息

可以把公司办公地点、分支机构、考勤组打卡点存到 Redis GEO。

```
attendance:geo:workplace
```

写入地点：

```
GEOADD attendance:geo:workplace 116.397128 39.916527 office_beijing
GEOADD attendance:geo:workplace 121.473701 31.230416 office_shanghai
```

员工打卡时：

```
GEODIST attendance:geo:workplace office_beijing user_location m
```

或者用：

```
GEOSEARCH attendance:geo:workplace FROMLONLAT {lng} {lat} BYRADIUS 500 m
```

适合功能：

- 判断员工是否在办公地点 500 米范围内
- 多办公点打卡
- 外勤地点打卡
- 考勤组绑定不同打卡地点

### 考勤组绑定 GEO 范围

比如：

```
attendance:geo:group:{groupId}
```

一个考勤组可以配置多个办公地点：

```
GEOADD attendance:geo:group:1 116.397128 39.916527 HQ
GEOADD attendance:geo:group:1 116.401000 39.920000 branch_1
```

员工打卡时：

```
GEOSEARCH attendance:geo:group:1 FROMLONLAT userLng userLat BYRADIUS 500 m
```

如果返回非空，说明在打卡范围内。

### 注意

Redis GEO 适合快速粗判断，但最终严谨校验可以在服务端保留一层规则：

```
Redis GEO 快速判断
+
后端业务规则校验
```

因为打卡有安全诉求，不能只信前端传来的 GPS。

## 3. Set：权限范围、考勤组成员、去重

Set 适合做“集合关系”。

### 考勤模块：考勤组成员集合

```
attendance:group:members:{groupId}
```

存员工 ID：

```
SADD attendance:group:members:1 1001 1002 1003
```

判断员工是否属于考勤组：

```
SISMEMBER attendance:group:members:1 1001
```

适合功能：

- 打卡时快速判断员工是否在考勤组
- HR 调整考勤组后刷新集合
- 月度统计时快速拿到考勤组成员

### 考勤模块：迟到员工集合

```
attendance:late:{yyyyMMdd}
```

打卡迟到：

```
SADD attendance:late:20260711 1001
```

统计当天迟到人数：

```
SCARD attendance:late:20260711
```

适合：

- 首页考勤看板
- HR 当天异常监控
- 部门迟到人数统计

### 薪资模块：薪资批次员工集合

```
salary:batch:employees:{batchId}
```

适合：

- 批次范围内员工快速判断
- 分片计算时拆分员工
- 防止重复加入批次

## 4. ZSet：排行榜、时间线、延迟处理

ZSet 适合“按时间或分数排序”。

### 考勤模块：打卡时间排序

```
attendance:clock:zset:{yyyyMMdd}
```

score 用打卡时间戳：

```
ZADD attendance:clock:zset:20260711 1783741320 1001
```

适合功能：

- 当天最早打卡榜
- 当天迟到员工按迟到时间排序
- HR 实时查看打卡动态

### 考勤模块：补卡申请时间线

```
attendance:correction:timeline:{employeeId}
```

score 用申请时间戳：

```
ZADD attendance:correction:timeline:1001 1783741320 correctionId
```

适合：

- 个人补卡记录时间线
- 主管审批列表按时间排序

### 薪资模块：薪资异常优先级排序

```
salary:exception:zset:{batchId}
```

score 可以设计：

```
BLOCK = 300
RED = 200
YELLOW = 100
```

或者叠加金额变动比例：

```
score = 异常等级权重 + 变动比例
```

适合：

- HR 优先处理阻断异常
- 薪资预览页异常排序
- 财务审批前风险排序

### 薪资模块：工资条查看时间线

```
salary:payslip:view:timeline:{employeeId}
```

score 为查看时间戳。

适合：

- 工资条查看日志
- 安全审计
- 最近查看记录

## 5. Hash：对象缓存

Hash 很适合存结构化对象。

### 考勤模块：当天打卡状态

```
attendance:daily:record:{employeeId}:{date}
```

字段：

```
clockInTime
clockInStatus
clockOutTime
clockOutStatus
groupId
```

使用：

```
HGETALL attendance:daily:record:1001:20260711
```

打卡成功后局部更新：

```
HSET attendance:daily:record:1001:20260711 clockInTime "09:02:11" clockInStatus "LATE"
```

比直接 JSON 字符串更适合局部更新。

### 薪资模块：批次摘要

```
salary:batch:summary:{batchId}
```

字段：

```
status
totalCount
totalGrossSalary
totalNetSalary
yellowWarningCount
redWarningCount
blockCount
```

适合：

- 批次列表
- 批次详情头部
- 前端轮询任务状态

注意：金额字段要短 TTL，且状态变化时删除或刷新。

## 6. HyperLogLog：去重统计

HyperLogLog 适合做近似去重统计，不适合精确薪资业务。

### 考勤模块：访问统计 / 使用人数

```
PFADD attendance:clock:uv:20260711 employeeId
PFCOUNT attendance:clock:uv:20260711
```

适合：

- 每日打卡页访问人数
- 我的考勤页访问人数
- 系统使用活跃度

不适合：

- 精确出勤人数
- 薪资核算人数

薪资模块建议少用 HLL，因为薪资需要精确。


## 我最建议你写进设计里的 Redis 高级点

### 考勤模块最推荐

```
1. Bitmap 实现员工月度打卡 / 补卡 / 请假日历快速判定
2. GEO 实现多办公地点 GPS 打卡范围校验
3. Set 实现考勤组成员快速判断与当天迟到员工集合
4. Hash 缓存员工当天打卡状态
5. ZSet 构建当天打卡时间线和异常排序
```

### 薪资模块最推荐

```
1. Hash 缓存薪资批次摘要和异常汇总
2. ZSet 按异常等级和金额变动比例排序薪资异常
3. Set 维护薪资批次员工集合，辅助分片核算
4. 短 TTL String 保存工资条二次验证状态
```
