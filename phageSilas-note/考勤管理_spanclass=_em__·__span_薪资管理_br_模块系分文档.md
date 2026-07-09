
# 考勤管理 · 薪资管理  
模块系分文档

Human Resource Management System — Module Specification

DocumentHRMS-ATT-SAL-SAD

Version1.0 (初稿)

Date2026-07-09

Author凯贾

SourceHRMS 产品规格说明书 §6 / §7

StatusDraft for Review

### Modules in Scope

M-01 / §6

考勤管理

考勤规则配置、网页打卡、请假流程、月度统计与可视化。

M-02 / §7

薪资管理

薪资账套、员工薪资档案、月度核算、异常检测、工资条。

© HRMS Engineering Confidential — Internal Use

## Table of Contents

M-01考勤管理

*   1项目背景
*   2相关资料
*   3参与人
*   4功能模块
*   5功能模块树
*   6流程图
*   7UML 图
*   8数据库设计
*   9API 设计
*   10关键技术设计
*   11排期

M-02薪资管理

*   1项目背景
*   2相关资料
*   3参与人
*   4功能模块
*   5功能模块树
*   6流程图
*   7UML 图
*   8数据库设计
*   9API 设计
*   10关键技术设计
*   11排期

M-01 / §6 考勤管理

ATTENDANCE MANAGEMENT

## 1项目背景

本模块来源于 HRMS（人资管理系统）产品规格说明书第 6 部分——考勤管理。当前公司考勤依赖纸质签到与 Excel 统计，存在打卡数据分散、请假审批不规范、统计效率低、考勤数据无法直接驱动薪资核算等问题。

本模块旨在实现考勤规则配置、网页打卡、请假流程、月度统计与可视化的全流程线上化，为薪资核算模块提供准确的考勤数据支撑，并为 HR 与部门主管提供实时考勤洞察。

## 2相关资料

*   人资管理系统（HRMS）详细产品规格说明书 §6 考勤管理
*   HRMS-员工档案管理系分（数据基础：员工 / 部门 / 职位）
*   AntV 图表库官方文档（折线图 / 饼图 / 柱状图 / 日历图）

## 3参与人

PM

产品经理

UI

设计师

FE

前端工程师

BE

后端工程师

QA

测试工程师

## 4功能模块

本模块核心功能包括：

*   **考勤规则配置**：考勤组定义（适用人员 / 班次类型 / 上下班时间 / 弹性范围 / 迟到早退阈值）、工作日设置（标准工作日 / 休息日 / 法定节假日）
*   **打卡功能**：网页端打卡、IP 白名单与 GPS 定位校验、上下班打卡状态判定（正常 / 迟到 / 早退 / 缺卡 / 旷工）、补卡申请
*   **请假管理**：请假类型（年假 / 病假 / 事假 / 婚假 / 产假 / 丧假 / 调休）、假期余额自动计算（年假按工龄、调休按加班累计）、请假申请与多级审批流
*   **考勤统计**：个人维度统计（出勤 / 迟到 / 早退 / 旷工 / 请假 / 加班 / 年假余额）、部门维度统计（出勤率 / 迟到率 / 请假率）、AntV 数据可视化

## 5功能模块树

考勤管理
├── 考勤规则配置
│   ├── 考勤组定义
│   ├── 考勤组成员管理
│   ├── 工作日设置
│   └── 班次配置（固定班/弹性班/排班制）
├── 打卡功能
│   ├── 网页端打卡
│   ├── IP/GPS 校验
│   ├── 打卡规则
│   ├── 打卡状态判定
│   └── 补卡申请
├── 请假管理
│   ├── 请假类型配置
│   ├── 假期余额计算（年假/调休）
│   ├── 请假申请
│   ├── 审批流路由
│   └── 余额扣减与回滚
└── 考勤统计
├── 个人维度统计
├── 部门维度统计
├── AntV 可视化报表
└── 月度定时归档

## 6流程图

### 6-1 打卡流程

员工进入打卡页面
│
▼
系统读取员工所属考勤组
│
├── 未配置考勤组 → 提示"未配置考勤组，请联系HR"
│
▼
校验当日是否为工作日
│
├── 休息日/法定节假日 → 提示"今日非工作日"
│
▼
校验打卡位置（IP 白名单 / GPS 范围）
│
├── 不在范围 → 提示"不在打卡范围"
│
▼
记录打卡时间 / IP / GPS
│
▼
根据班次与阈值判定打卡状态
│
├── 上班打卡 ≤ 规定时间            → 正常
├── 规定时间 &lt; 打卡 ≤ 规定+阈值     → 迟到
├── 打卡 &gt; 规定+阈值              → 旷工半天
├── 下班打卡 ≥ 规定时间            → 正常
├── 规定-阈值 ≤ 打卡 &lt; 规定        → 早退
├── 打卡 &lt; 规定-阈值              → 旷工半天
└── 当日无打卡记录                 → 缺勤
│
▼
写入 attendance\_record，刷新当日缓存状态

### 6-2 请假申请流程

员工发起请假申请
│
▼
系统校验请假类型与余额
│
├── 余额不足（年假/调休）→ 提示"余额不足"
├── 需上传证明（病假&amp; gt;1天/婚假/产假）→ 校验附件
│
▼
系统按规则计算请假天数（支持 0.5 天）
│
▼
按类型 + 天数路由审批流
│
├── 年假/调休 ≤ 3 天  → 直接上级
├── 年假/调休 &gt; 3 天  → 直接上级 → 部门负责人
├── 病假/事假 ≤ 1 天  → 直接上级
├── 病假/事假 &gt; 1 天  → 直接上级 → 部门负责人
└── 婚假/产假/丧假    → 直接上级 → HR 备案
│
▼
审批通过 → 扣减假期余额、写入考勤统计
审批拒绝 → 释放预扣减余额
审批撤回 → 释放预扣减余额

### 6-3 月度考勤统计流程

定时任务触发（每月 1 日凌晨 02:00）
│
▼
扫描上月所有在职员工的打卡记录
│
▼
按规则汇总个人维度
│
├── 应出勤天数 = 当月工作日数
├── 实际出勤天数 = 有打卡记录的天数
├── 迟到次数 / 早退次数 / 旷工天数 / 请假天数
└── 加班时长、年假余额
│
▼
汇总部门维度
│
├── 部门出勤率 = 实际出勤天数 / 应出勤天数
├── 部门迟到率 = 迟到人次 / 部门人数
└── 部门请假率 = 请假天数 / 应出勤天数
│
▼
生成 AntV 可视化数据集并缓存
│
▼
写入 attendance\_stat，归档上月打卡明细

## 7UML 图

### 7-1 考勤管理核心领域模型

``` 
@startuml
class AttendanceGroup {
  - id: Long
  - name: String
  - memberType: MemberType
  - shiftType: ShiftType
  - workStart: LocalTime
  - workEnd: LocalTime
  - restStart: LocalTime
  - restEnd: LocalTime
  - flexibleRange: String
  - lateThreshold: Integer
  - earlyLeaveThreshold: Integer
}

class WorkSchedule {
  - id: Long
  - groupId: Long
  - date: LocalDate
  - dayType: DayType
}

enum DayType {
  STANDARD_WORKDAY
  REST_DAY
  HOLIDAY
}

class AttendanceRecord {
  - id: Long
  - employeeId: Long
  - groupId: Long
  - recordDate: LocalDate
  - clockInTime: LocalDateTime
  - clockOutTime: LocalDateTime
  - clockInStatus: ClockStatus
  - clockOutStatus: ClockStatus
  - ipAddress: String
  - gpsLocation: String
}

enum ClockStatus {
  NORMAL
  LATE
  EARLY_LEAVE
  ABSENCE
  MISSING
}

class LeaveType {
  - id: Long
  - name: String
  - hasBalance: Boolean
  - needProof: Boolean
}

class LeaveBalance {
  - id: Long
  - employeeId: Long
  - leaveTypeId: Long
  - year: Integer
  - totalDays: BigDecimal
  - usedDays: BigDecimal
  - remainingDays: BigDecimal
}

class LeaveRequest {
  - id: Long
  - employeeId: Long
  - leaveTypeId: Long
  - startTime: LocalDateTime
  - endTime: LocalDateTime
  - days: BigDecimal
  - reason: String
  - attachmentUrl: String
  - status: ApprovalStatus
}

class AttendanceStat {
  - id: Long
  - employeeId: Long
  - statMonth: String
  - shouldAttendDays: Integer
  - actualAttendDays: Integer
  - lateCount: Integer
  - earlyLeaveCount: Integer
  - absenceDays: BigDecimal
  - leaveDays: BigDecimal
  - overtimeHours: BigDecimal
}

AttendanceGroup *-- WorkSchedule
AttendanceRecord --> AttendanceGroup
LeaveBalance --> LeaveType
LeaveRequest --> LeaveType
@enduml

```

### 7-2 打卡时序图

```
@startuml
actor Employee as emp
participant "前端" as fe
participant "网关/鉴权" as gateway
participant "考勤服务" as attSvc
participant "规则校验服务" as ruleSvc
database "MySQL" as db
database "Redis" as cache

emp -> fe : 点击"打卡"
fe -> gateway : POST /api/v1/attendance/clock
gateway -> attSvc : 转发打卡请求
attSvc -> cache : 查询员工考勤组(预热)
cache --> attSvc : 返回考勤组配置
attSvc -> ruleSvc : 校验打卡条件(IP/GPS/工作日)
ruleSvc --> attSvc : 校验结果
alt 校验通过
    attSvc -> db : INSERT 打卡记录
    attSvc -> cache : 更新当日打卡状态
    attSvc --> fe : 返回打卡成功+状态
    fe --> emp : 展示打卡结果
else 校验失败
    attSvc --> fe : 返回失败原因
    fe --> emp : 提示失败原因
end
@enduml

```
![[image-10.png]]
### 7-3 请假申请时序图

@startuml
actor Employee as emp
participant "前端" as fe
participant "考勤服务" as attSvc
participant "审批引擎" as wf
participant "通知服务" as notifySvc
database "MySQL" as db
database "Redis" as cache
emp -&gt; fe : 提交请假申请
fe -&gt; attSvc : POST /api/v1/leaves
attSvc -&gt; cache : 查询假期余额
cache --&gt; attSvc : 返回余额
alt 余额充足 &amp; 附件齐全
attSvc -&gt; db : 预扣减假期余额(乐观锁)
attSvc -&gt; db : INSERT leave\_request
attSvc -&gt; wf : 创建审批流程(按规则路由)
wf --&gt; attSvc : 返回流程ID
attSvc -&gt; notifySvc : 通知审批人
attSvc --&gt; fe : 申请成功
fe --&gt; emp : 展示审批进度
else 余额不足
attSvc --&gt; fe : 余额不足
fe --&gt; emp : 提示调整天数
end
note over wf, db
审批通过 → 确认扣减余额
审批拒绝/撤回 → 回滚预扣减余额
end note
@enduml

### 7-4 月度考勤统计时序图

@startuml
participant "定时任务调度" as scheduler
participant "考勤统计服务" as statSvc
participant "考勤服务" as attSvc
database "MySQL" as db
database "Redis" as cache
scheduler -&gt; statSvc : 触发月度统计(每月1日02:00)
statSvc -&gt; attSvc : 获取上月所有员工打卡记录
attSvc -&gt; db : SELECT attendance\_record WHERE month=last
db --&gt; attSvc : 返回记录列表
attSvc --&gt; statSvc : 返回打卡数据
statSvc -&gt; statSvc : 汇总个人维度(出勤/迟到/早退/旷工/请假/加班)
statSvc -&gt; statSvc : 汇总部门维度(出勤率/迟到率/请假率)
statSvc -&gt; db : 批量 UPSERT attendance\_stat
statSvc -&gt; cache : 缓存 AntV 可视化数据集
statSvc --&gt; scheduler : 统计完成，归档上月数据
@enduml

## 8数据库设计

### 8-1 考勤组表 attendance\_group

CREATE TABLE IF NOT EXISTS attendance\_group (
id                      BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT COMMENT '主键ID',
name                    VARCHAR(64)     NOT NULL COMMENT '考勤组名称',
shift\_type              TINYINT         NOT NULL COMMENT '班次类型：1=固定班 2=弹性班 3=排班制',
work\_start              TIME            NOT NULL COMMENT '上班时间',
work\_end                TIME            NOT NULL COMMENT '下班时间',
rest\_start              TIME                     COMMENT '午休开始',
rest\_end                TIME                     COMMENT '午休结束',
flexible\_start          TIME                     COMMENT '弹性最早打卡',
flexible\_end            TIME                     COMMENT '弹性最晚打卡',
late\_threshold          INT             NOT NULL DEFAULT 15 COMMENT '迟到阈值（分钟）',
early\_leave\_threshold   INT             NOT NULL DEFAULT 15 COMMENT '早退阈值（分钟）',
status                  TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
create\_time             DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time             DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
is\_deleted              TINYINT         NOT NULL DEFAULT 0,
PRIMARY KEY (id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组表';

### 8-2 考勤组成员表 attendance\_group\_member

CREATE TABLE IF NOT EXISTS attendance\_group\_member (
id           BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
group\_id     BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
member\_type  TINYINT         NOT NULL COMMENT '成员类型：1=部门 2=职位 3=个人',
member\_id    BIGINT UNSIGNED NOT NULL COMMENT '成员ID',
create\_time  DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
KEY idx\_group\_id (group\_id),
KEY idx\_member (member\_type, member\_id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组成员表';

### 8-3 工作日设置表 work\_schedule

CREATE TABLE IF NOT EXISTS work\_schedule (
id             BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
group\_id       BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
schedule\_date  DATE            NOT NULL COMMENT '日期',
day\_type       TINYINT         NOT NULL COMMENT '1=标准工作日 2=休息日 3=法定节假日',
create\_time    DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_group\_date (group\_id, schedule\_date),
KEY idx\_schedule\_date (schedule\_date)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工作日设置表';

### 8-4 打卡记录表 attendance\_record

CREATE TABLE IF NOT EXISTS attendance\_record (
id                 BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
employee\_id        BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
group\_id           BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
record\_date        DATE            NOT NULL COMMENT '打卡日期',
clock\_in\_time      DATETIME                 COMMENT '上班打卡时间',
clock\_out\_time     DATETIME                 COMMENT '下班打卡时间',
clock\_in\_status    TINYINT                 COMMENT '上班状态：1=正常 2=迟到 3=缺卡 4=旷工',
clock\_out\_status   TINYINT                 COMMENT '下班状态：1=正常 2=早退 3=缺卡 4=旷工',
clock\_in\_ip        VARCHAR(64)              COMMENT '上班打卡IP',
clock\_out\_ip       VARCHAR(64)              COMMENT '下班打卡IP',
clock\_in\_gps       VARCHAR(64)              COMMENT '上班打卡GPS',
clock\_out\_gps      VARCHAR(64)              COMMENT '下班打卡GPS',
create\_time        DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time        DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_emp\_date (employee\_id, record\_date),
KEY idx\_group\_date (group\_id, record\_date),
KEY idx\_record\_date (record\_date)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '打卡记录表';

### 8-5 请假类型表 leave\_type

CREATE TABLE IF NOT EXISTS leave\_type (
id           BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
name         VARCHAR(32)     NOT NULL COMMENT '类型名称：年假/病假/事假/婚假/产假/丧假/调休',
code         VARCHAR(32)     NOT NULL COMMENT '类型编码',
has\_balance  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否管理余额：0=否 1=是',
need\_proof   TINYINT         NOT NULL DEFAULT 0 COMMENT '是否需要证明材料',
sort\_order   INT             NOT NULL DEFAULT 0,
create\_time  DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_code (code)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假类型表';

### 8-6 假期余额表 leave\_balance

CREATE TABLE IF NOT EXISTS leave\_balance (
id              BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
employee\_id     BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
leave\_type\_id   BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
year            SMALLINT        NOT NULL COMMENT '年份',
total\_days      DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '总额度',
used\_days       DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '已使用',
remaining\_days  DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '剩余',
version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
create\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_emp\_type\_year (employee\_id, leave\_type\_id, year),
KEY idx\_employee\_id (employee\_id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '假期余额表';

### 8-7 请假申请表 leave\_request

CREATE TABLE IF NOT EXISTS leave\_request (
id              BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
request\_no      VARCHAR(32)     NOT NULL COMMENT '申请单号',
employee\_id     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
leave\_type\_id   BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
start\_time      DATETIME        NOT NULL COMMENT '开始时间',
end\_time        DATETIME        NOT NULL COMMENT '结束时间',
start\_period    TINYINT         NOT NULL COMMENT '开始时段：1=上午 2=下午',
end\_period      TINYINT         NOT NULL COMMENT '结束时段：1=上午 2=下午',
days            DECIMAL(6,1)    NOT NULL COMMENT '请假天数（支持0.5天）',
reason          VARCHAR(512)    NOT NULL COMMENT '请假事由',
attachment\_url  VARCHAR(512)             COMMENT '附件URL',
handover\_id     BIGINT UNSIGNED          COMMENT '工作交接人ID',
status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=草稿 1=审批中 2=已批准 3=已拒绝 4=已撤回',
create\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_request\_no (request\_no),
KEY idx\_employee\_id (employee\_id),
KEY idx\_status (status)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假申请表';

### 8-8 考勤统计表 attendance\_stat

CREATE TABLE IF NOT EXISTS attendance\_stat (
id                       BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
employee\_id              BIGINT UNSIGNED NOT NULL,
stat\_month               CHAR(7)         NOT NULL COMMENT '统计月份，如 2024-07',
should\_attend\_days       INT             NOT NULL COMMENT '应出勤天数',
actual\_attend\_days       INT             NOT NULL COMMENT '实际出勤天数',
late\_count               INT             NOT NULL DEFAULT 0 COMMENT '迟到次数',
early\_leave\_count        INT             NOT NULL DEFAULT 0 COMMENT '早退次数',
absence\_days             DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '旷工天数',
leave\_days               DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '请假天数',
overtime\_hours           DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '加班时长',
annual\_leave\_remaining   DECIMAL(6,1)             COMMENT '年假余额',
create\_time              DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_emp\_month (employee\_id, stat\_month),
KEY idx\_stat\_month (stat\_month)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤统计表';

## 9API 设计

GET /api/v1/attendance/groups 查询考勤组列表

#### 请求参数

参数

类型

必填

描述

keyword

String

否

考勤组名称模糊匹配

status

Integer

否

0=禁用 1=启用

page

Integer

否

页码，默认1

size

Integer

否

每页条数，默认20

POST /api/v1/attendance/groups 创建考勤组

POST /api/v1/attendance/clock 员工打卡

#### 请求参数

参数

类型

必填

描述

clockType

Integer

是

1=上班打卡 2=下班打卡

ipAddress

String

是

客户端IP（由网关透传）

gpsLocation

String

否

GPS定位（经纬度），可选

deviceInfo

String

否

设备信息

#### 响应示例

{
"code": 0,
"message": "success",
"data": {
"recordId": 8848,
"clockTime": "2024-07-09T09:02:13",
"clockStatus": "LATE",
"statusDesc": "迟到"
}
}

POST /api/v1/leaves 提交请假申请

#### 请求参数

参数

类型

必填

描述

leaveTypeId

Long

是

请假类型ID

startTime

DateTime

是

开始时间

endTime

DateTime

是

结束时间

startPeriod

Integer

是

开始时段：1=上午 2=下午

endPeriod

Integer

是

结束时段：1=上午 2=下午

reason

String

是

请假事由

attachmentUrl

String

条件必填

病假/婚假/产假需上传证明

handoverId

Long

否

工作交接人ID

GET /api/v1/leaves/balances 查询假期余额

GET /api/v1/attendance/stats/personal 个人维度考勤统计

#### 请求参数

参数

类型

必填

描述

employeeId

Long

否

不传则取当前登录用户

statMonth

String

是

统计月份，如 2024-07

GET /api/v1/attendance/stats/visualization 考勤可视化数据（AntV）

#### 响应数据集

图表

图表类型

数据内容

适用角色

部门出勤率趋势

折线图

近 6 个月各部门出勤率变化

HR、部门主管

请假类型分布

饼图/环形图

当月各请假类型占比

HR

迟到早退排行

柱状图

部门维度迟到早退人次对比

HR、部门主管

考勤日历图

折线/柱状图

每日出勤状态

普通员工、HR

## 10关键技术设计

### 10-1 打卡防作弊方案

*   支持配置 IP 白名单：员工打卡时由网关透传真实 IP，服务端校验是否在白名单网段内
*   支持配置 GPS 定位范围：前端通过 H5 `navigator.geolocation` 获取定位，服务端按预定义圆形区域（中心点 + 半径）判定
*   IP 与 GPS 至少满足一项，否则拒绝打卡并记录异常日志
*   设备指纹辅助识别代打卡风险（UA + IP + 时序特征）

### 10-2 假期余额并发扣减方案

采用乐观锁 + 状态机双重保障，避免并发请假导致余额超扣：

*   提交请假申请时执行预扣减：`UPDATE leave_balance SET used_days = used_days + ?, remaining_days = remaining_days - ?, version = version + 1 WHERE id = ? AND version = ?`
*   审批通过 → 确认扣减，状态置为 `CONFIRMED`
*   审批拒绝 / 撤回 → 回滚预扣减，状态置为 `ROLLED_BACK`
*   乐观锁失败时返回 `40909 CONFLICT`，前端提示"余额已变化，请刷新重试"

### 10-3 年假余额自动计算规则

入职工龄 &lt; 1 年              → 0 天
入职满 1 年不满 10 年         → 5 天/年
入职满 10 年不满 20 年        → 10 天/年
入职满 20 年                  → 15 天/年
当年入职按剩余月份折算：
折算天数 = (12 - 入职月份 + 1) / 12 × 对应天数
四舍五入至 0.5 天
调休：加班时长 1:1 转换，加班当月及次月有效，过期清零

### 10-4 月度统计定时任务方案

*   调度：RabbitMQ 延迟队列 + XXL-JOB 分布式调度，每月 1 日 02:00 触发
*   分片：按 employeeId 取模分片，单批 200 人，并发度 4，避免长事务
*   幂等：`attendance_stat` 表 `uk_emp_month` 唯一索引 + UPSERT，支持重跑
*   失败重试：单员工失败记录到 `stat_failed_log`，人工补跑
*   可视化数据集生成后写入 Redis，TTL 7 天，HR 看板直接读取

### 10-5 数据可视化方案（AntV）

*   折线图：@antv/g2 `Line`，展示部门出勤率 6 个月趋势
*   饼图：@antv/g2 `Pie`，展示当月请假类型占比
*   柱状图：@antv/g2 `Column`，展示部门迟到早退人次对比
*   日历图：@antv/g2 `Calendar`，展示每日出勤状态
*   统一封装 组件，按角色权限控制可见图表

## 11排期

01

需求评审评审产品规格，确认考勤规则与审批流路由

1 天

02

技术方案完成系分评审，确认数据库与接口方案

2 天

03

数据库开发建表、索引优化、初始化数据（节假日/请假类型）

2 天

04

后端开发考勤组、打卡、请假、统计、定时任务

9 天

05

前端开发打卡页、请假申请、统计看板、AntV 图表

8 天

06

联调测试前后端联调、并发扣减测试、防作弊测试

4 天

07

回归上线全量回归、预发验证、正式上线

2 天

总预估工期 28 个工作日

M-02 / §7 薪资管理

SALARY MANAGEMENT

## 1项目背景

本模块来源于 HRMS 产品规格说明书第 7 部分——薪资管理。当前公司薪资核算依赖 Excel 模板与手工计算，存在算薪效率低、考勤数据未打通、异常难发现、工资条分发不透明等问题。

本模块旨在建立统一的薪资账套与员工薪资档案，实现月度薪资自动核算、异常检测、财务审批与工资条线上分发，并基于 AntV 提供薪资成本可视化看板，支撑 HR 与财务的精细化运营。

## 2相关资料

*   人资管理系统（HRMS）详细产品规格说明书 §7 薪资管理
*   HRMS-员工档案管理系分（员工薪资档案字段定义）
*   考勤管理模块系分（考勤统计数据来源）
*   国家税务总局累计预扣法公告（个税计算规则）

## 3参与人

PM

产品经理

UI

设计师

FE

前端工程师

BE

后端工程师

QA

测试工程师

## 4功能模块

本模块核心功能包括：

*   **薪资账套**：账套定义（名称 / 适用范围 / 生效日期 / 工资项目）、工资项目类型（固定收入 / 变动收入 / 考勤扣款 / 社保扣除 / 公积金扣除 / 个税）、典型账套示例（标准职员）
*   **员工薪资设置**：薪资档案（适用账套 / 基本工资 / 各项津贴基数 / 社保公积金基数 / 调薪历史）、试用期薪资（80%–100% 比例）
*   **月度薪资核算**：核算流程（创建批次 → 自动计算 → 异常检测 → HR 确认 → 财务审批 → 发放）、批次状态机、异常检测规则、AntV 数据可视化
*   **工资条**：员工查看规则（二次验证）、工资条内容示例、个人薪资趋势图（AntV 折线图）

## 5功能模块树

薪资管理
├── 薪资账套（工资模板）
│   ├── 账套定义
│   ├── 工资项目类型
│   ├── 典型账套示例
│   └── 账套启用/停用
├── 员工薪资设置
│   ├── 薪资档案
│   ├── 试用期薪资比例
│   └── 调薪历史
├── 月度薪资核算
│   ├── 核算流程
│   ├── 薪资批次状态机
│   ├── 异常检测规则
│   ├── 财务审批
│   └── AntV 数据可视化
└── 工资条
├── 员工查看规则
├── 工资条内容
├── 二次验证
└── 个人薪资趋势图

## 6流程图

### 6-1 月度薪资核算流程

HR 创建薪资批次（选择月份 / 范围）
│
▼
状态：草稿 ──&gt; 计算中
│
▼
系统拉取当月考勤统计、加班时长、请假天数
│
▼
按账套公式逐项计算：
├── 固定收入（直接取值）
├── 变动收入（公式计算：绩效 / 加班费）
├── 考勤扣款（迟到 / 请假）
├── 社保扣除（基数 × 比例）
├── 公积金扣除（基数 × 12%）
└── 个税（累计预扣法）
│
▼
异常检测
│
├── 请假 &gt; 15 天     → 黄色预警
├── 加班费 &gt; 50 小时 → 黄色预警
├── 较上月变动 &gt; 30% → 红色预警（需确认）
└── 未设薪资档案     → 红色阻断（无法计算）
│
▼
状态：计算中 ──&gt; 待确认
│
▼
HR 预览 / 调整 / 提交审批 ──&gt; 审批中
│
▼
财务专员审批 ── \[老板\]（金额超阈值时二审）
│
├── 驳回 → 修改后重新提交
└── 通过 → 已通过
│
▼
实际发放后标记已发放 ──&gt; 工资条对员工可见
│
▼
状态归档

### 6-2 工资条查看流程

员工进入"我的薪资"
│
▼
选择月份查看工资条
│
▼
触发二次验证（短信验证码 / 密码）
│
├── 验证失败 → 拒绝查看
└── 验证通过 → 返回工资条详情
│
▼
展示工资项目明细 + 个人薪资趋势图（AntV 折线图，近6月实发）

### 6-3 试用期薪资计算流程

读取员工在职状态
│
├── 状态 = 试用期
│     │
│     ▼
│   基本工资 × 试用期比例（80%–100%）
│   津贴同步按比例折算
│   社保公积金按全额基数缴纳
│
└── 状态 = 正式
│
▼
全额基本工资 + 全额津贴
社保公积金按全额基数缴纳

## 7UML 图

### 7-1 薪资管理核心领域模型

@startuml
class SalaryAccount {
id: Long
name: String                 "账套名称"
scopeType: ScopeType         "1=部门 2=职位 3=职级"
scopeValue: String
effectiveDate: LocalDate     "生效日期"
status: Integer              "0=停用 1=启用"
}
class SalaryItem {
id: Long
accountId: Long
name: String                 "项目名称"
itemType: SalaryItemType
formula: String              "计算公式/规则"
sortOrder: Integer
}
enum SalaryItemType {
FIXED\_INCOME        "固定收入"
VARIABLE\_INCOME     "变动收入"
ATTENDANCE\_DEDUCT   "考勤扣款"
SOCIAL\_INSURANCE    "社保扣除"
HOUSING\_FUND        "公积金扣除"
INCOME\_TAX          "个税"
}
class EmployeeSalary {
id: Long
employeeId: Long
accountId: Long               "适用账套"
baseSalary: BigDecimal        "基本工资"
allowanceBase: BigDecimal     "津贴基数"
socialInsuranceBase: BigDecimal
housingFundBase: BigDecimal
performanceBase: BigDecimal
probationRatio: BigDecimal    "试用期比例 0.8~1.0"
effectiveDate: LocalDate
}
class SalaryAdjustment {
id: Long
employeeSalaryId: Long
adjustDate: LocalDate
oldBaseSalary: BigDecimal
newBaseSalary: BigDecimal
reason: String
operatorId: Long
}
class SalaryBatch {
id: Long
batchNo: String               "批次编号"
month: String                 "yyyy-MM"
status: BatchStatus
totalCount: Integer
totalGross: BigDecimal        "应发总额"
totalNet: BigDecimal          "实发总额"
createdBy: Long
createTime: LocalDateTime
}
enum BatchStatus {
DRAFT         "草稿"
CALCULATING   "计算中"
PENDING\_CONFIRM "待确认"
APPROVING     "审批中"
APPROVED      "已通过"
PAID          "已发放"
REJECTED      "已驳回"
}
class SalaryBatchItem {
id: Long
batchId: Long
employeeId: Long
baseSalary: BigDecimal
allowance: BigDecimal
performance: BigDecimal
overtimePay: BigDecimal
lateDeduction: BigDecimal
leaveDeduction: BigDecimal
socialInsurance: BigDecimal
housingFund: BigDecimal
incomeTax: BigDecimal
grossSalary: BigDecimal       "应发"
netSalary: BigDecimal         "实发"
exceptionLevel: Integer       "0=正常 1=黄色 2=红色"
}
class Payslip {
id: Long
batchItemId: Long
employeeId: Long
month: String
viewed: Boolean
viewTime: LocalDateTime
}
SalaryAccount \*-- SalaryItem
EmployeeSalary --&gt; SalaryAccount
EmployeeSalary \*-- SalaryAdjustment
SalaryBatch \*-- SalaryBatchItem
SalaryBatchItem --&gt; EmployeeSalary
Payslip --&gt; SalaryBatchItem
@enduml

### 7-2 薪资核算时序图

@startuml
actor HR as hr
participant "前端" as fe
participant "薪资服务" as salSvc
participant "考勤服务" as attSvc
participant "计算引擎" as calcEngine
participant "异常检测" as ruleSvc
participant "审批引擎" as wf
database "MySQL" as db
database "Redis" as cache
hr -&gt; fe : 创建薪资批次（月份/范围）
fe -&gt; salSvc : POST /api/v1/salary/batches
salSvc -&gt; db : INSERT salary\_batch (status=DRAFT)
salSvc --&gt; fe : 返回批次ID
hr -&gt; fe : 点击"开始计算"
fe -&gt; salSvc : POST /api/v1/salary/batches/{id}/calculate
salSvc -&gt; db : UPDATE status=CALCULATING
salSvc -&gt; attSvc : 拉取当月考勤统计
attSvc --&gt; salSvc : 返回考勤数据
salSvc -&gt; cache : 查询员工薪资档案(预热)
cache --&gt; salSvc : 返回档案
salSvc -&gt; calcEngine : 逐项计算（固定/变动/扣款/社保/个税）
calcEngine --&gt; salSvc : 返回计算结果
salSvc -&gt; ruleSvc : 异常检测
ruleSvc --&gt; salSvc : 返回异常标记
salSvc -&gt; db : 批量 INSERT salary\_batch\_item
salSvc -&gt; db : UPDATE status=PENDING\_CONFIRM
salSvc -&gt; cache : 缓存可视化数据集
salSvc --&gt; fe : 计算完成
fe --&gt; hr : 展示预览与异常列表
@enduml

### 7-3 工资条查看时序图

@startuml
actor Employee as emp
participant "前端" as fe
participant "网关/鉴权" as gateway
participant "薪资服务" as salSvc
participant "二次验证服务" as verifySvc
database "MySQL" as db
emp -&gt; fe : 选择月份查看工资条
fe -&gt; verifySvc : 请求发送短信验证码
verifySvc --&gt; fe : 验证码已发送
emp -&gt; fe : 输入验证码
fe -&gt; gateway : POST /api/v1/salary/payslips/{month} (含验证码)
gateway -&gt; verifySvc : 校验验证码
verifySvc --&gt; gateway : 校验通过
gateway -&gt; salSvc : 查询工资条
salSvc -&gt; db : SELECT salary\_batch\_item + payslip
db --&gt; salSvc : 返回工资条数据
salSvc -&gt; db : UPDATE payslip SET viewed=1
salSvc --&gt; fe : 返回工资条明细
fe --&gt; emp : 展示工资项目 + 趋势图
@enduml

## 8数据库设计

### 8-1 薪资账套表 salary\_account

CREATE TABLE IF NOT EXISTS salary\_account (
id              BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
name            VARCHAR(64)     NOT NULL COMMENT '账套名称',
scope\_type      TINYINT         NOT NULL COMMENT '适用范围：1=部门 2=职位 3=职级',
scope\_value     VARCHAR(128)    NOT NULL COMMENT '范围值（逗号分隔）',
effective\_date  DATE            NOT NULL COMMENT '生效日期',
status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
create\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time     DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
is\_deleted      TINYINT         NOT NULL DEFAULT 0,
PRIMARY KEY (id),
KEY idx\_status (status)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资账套表';

### 8-2 工资项目表 salary\_item

CREATE TABLE IF NOT EXISTS salary\_item (
id           BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
account\_id   BIGINT UNSIGNED NOT NULL COMMENT '账套ID',
name         VARCHAR(64)     NOT NULL COMMENT '项目名称',
item\_type    TINYINT         NOT NULL COMMENT '1=固定收入 2=变动收入 3=考勤扣款 4=社保 5=公积金 6=个税',
formula      VARCHAR(512)             COMMENT '计算公式/规则',
sort\_order   INT             NOT NULL DEFAULT 0,
create\_time  DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
KEY idx\_account\_id (account\_id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资项目表';

### 8-3 员工薪资档案表 employee\_salary

CREATE TABLE IF NOT EXISTS employee\_salary (
id                      BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
employee\_id             BIGINT UNSIGNED NOT NULL,
account\_id              BIGINT UNSIGNED NOT NULL COMMENT '适用账套ID',
base\_salary             DECIMAL(12,2)   NOT NULL COMMENT '基本工资',
allowance\_base          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '津贴基数',
social\_insurance\_base   DECIMAL(12,2)   NOT NULL COMMENT '社保基数',
housing\_fund\_base       DECIMAL(12,2)   NOT NULL COMMENT '公积金基数',
performance\_base        DECIMAL(12,2)            DEFAULT 0 COMMENT '绩效基数',
probation\_ratio         DECIMAL(5,4)    NOT NULL DEFAULT 1.0000 COMMENT '试用期比例 0.8~1.0',
effective\_date          DATE            NOT NULL COMMENT '生效日期',
create\_time             DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time             DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_employee\_id (employee\_id),
KEY idx\_account\_id (account\_id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工薪资档案表';

### 8-4 调薪历史表 salary\_adjustment

CREATE TABLE IF NOT EXISTS salary\_adjustment (
id                  BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
employee\_id         BIGINT UNSIGNED NOT NULL,
adjust\_date         DATE            NOT NULL COMMENT '调薪日期',
old\_base\_salary     DECIMAL(12,2)   NOT NULL COMMENT '原基本工资',
new\_base\_salary     DECIMAL(12,2)   NOT NULL COMMENT '新基本工资',
reason              VARCHAR(256)             COMMENT '调薪原因',
operator\_id         BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
create\_time         DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
KEY idx\_employee\_id (employee\_id),
KEY idx\_adjust\_date (adjust\_date)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调薪历史表';

### 8-5 薪资批次表 salary\_batch

CREATE TABLE IF NOT EXISTS salary\_batch (
id             BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
batch\_no       VARCHAR(32)     NOT NULL COMMENT '批次编号',
month          CHAR(7)         NOT NULL COMMENT '薪资月份 yyyy-MM',
status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0=草稿 1=计算中 2=待确认 3=审批中 4=已通过 5=已发放 6=已驳回',
total\_count    INT             NOT NULL DEFAULT 0 COMMENT '员工数',
total\_gross    DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '应发总额',
total\_net      DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '实发总额',
created\_by     BIGINT UNSIGNED NOT NULL,
create\_time    DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
update\_time    DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_batch\_no (batch\_no),
UNIQUE KEY uk\_month (month)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资批次表';

### 8-6 薪资明细表 salary\_batch\_item

CREATE TABLE IF NOT EXISTS salary\_batch\_item (
id                  BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
batch\_id            BIGINT UNSIGNED NOT NULL,
employee\_id         BIGINT UNSIGNED NOT NULL,
base\_salary         DECIMAL(12,2)   NOT NULL COMMENT '基本工资',
allowance           DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '岗位津贴',
performance         DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '绩效奖金',
overtime\_pay        DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '加班费',
late\_deduction      DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '迟到扣款',
leave\_deduction     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '请假扣款',
social\_insurance    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '社保扣除',
housing\_fund        DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '公积金扣除',
income\_tax          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '个人所得税',
gross\_salary        DECIMAL(12,2)   NOT NULL COMMENT '应发工资',
net\_salary          DECIMAL(12,2)   NOT NULL COMMENT '实发工资',
exception\_level     TINYINT         NOT NULL DEFAULT 0 COMMENT '0=正常 1=黄色预警 2=红色阻断',
exception\_remark    VARCHAR(256)             COMMENT '异常说明',
create\_time         DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_batch\_emp (batch\_id, employee\_id),
KEY idx\_employee\_id (employee\_id)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资明细表';

### 8-7 工资条表 payslip

CREATE TABLE IF NOT EXISTS payslip (
id               BIGINT UNSIGNED NOT NULL AUTO\_INCREMENT,
batch\_item\_id    BIGINT UNSIGNED NOT NULL COMMENT '薪资明细ID',
employee\_id      BIGINT UNSIGNED NOT NULL,
month            CHAR(7)         NOT NULL,
viewed           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未查看 1=已查看',
view\_time        DATETIME                 COMMENT '首次查看时间',
create\_time      DATETIME        NOT NULL DEFAULT CURRENT\_TIMESTAMP,
PRIMARY KEY (id),
UNIQUE KEY uk\_batch\_item\_id (batch\_item\_id),
UNIQUE KEY uk\_emp\_month (employee\_id, month)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资条表';

## 9API 设计

POST /api/v1/salary/accounts 创建薪资账套

#### 请求参数

参数

类型

必填

描述

name

String

是

账套名称

scopeType

Integer

是

1=部门 2=职位 3=职级

scopeValue

String

是

范围值

effectiveDate

Date

是

生效日期

items

Array

是

工资项目列表

PUT /api/v1/salary/employees/{employeeId} 设置/更新员工薪资档案

#### 请求参数

参数

类型

必填

描述

accountId

Long

是

薪资账套ID

baseSalary

BigDecimal

是

基本工资

allowanceBase

BigDecimal

否

津贴基数

socialInsuranceBase

BigDecimal

是

社保基数

housingFundBase

BigDecimal

是

公积金基数

performanceBase

BigDecimal

否

绩效基数

probationRatio

BigDecimal

否

试用期比例 0.8~1.0

effectiveDate

Date

是

生效日期

adjustReason

String

否

调薪原因（变化时必填）

POST /api/v1/salary/batches 创建薪资批次

#### 请求参数

参数

类型

必填

描述

month

String

是

薪资月份 yyyy-MM

scopeType

Integer

是

1=全公司 2=部门 3=个人

scopeValue

String

否

范围值

POST /api/v1/salary/batches/{id}/calculate 触发薪资核算

#### 响应示例

{
"code": 0,
"message": "success",
"data": {
"batchId": 2024,
"batchNo": "SAL-202407-001",
"status": "PENDING\_CONFIRM",
"totalCount": 156,
"totalGross": 2856421.50,
"totalNet": 2218436.78,
"exceptionSummary": {
"yellowCount": 8,
"redCount": 2,
"blockCount": 1
}
}
}

POST /api/v1/salary/batches/{id}/submit 提交财务审批

GET /api/v1/salary/batches/{id}/preview 薪资预览（含异常标记）

GET /api/v1/salary/batches/{id}/visualization 薪资可视化数据（AntV）

#### 响应数据集

图表

图表类型

数据内容

适用角色

薪资成本月度趋势

折线图

近 6 个月应发/实发总额变化

HR、财务

部门薪资分布

柱状图

各部门薪资成本对比

HR、财务

薪资构成占比

饼图/环形图

当月各项目占比

HR

社保公积金对比

分组柱状图

各项社保公积金扣除对比

HR、财务

薪资变动分布

直方图

薪资较上月变动幅度员工分布

HR、财务

GET /api/v1/salary/payslips/{month} 员工查看工资条（需二次验证）

#### 请求头

Header

必填

描述

X-Verify-Token

是

二次验证 token（短信验证码换取）

#### 响应示例

{
"code": 0,
"message": "success",
"data": {
"month": "2024-07",
"employeeNo": "202401005",
"items": \[
{ "name": "基本工资", "amount": 15000.00 },
{ "name": "岗位津贴", "amount": 3000.00 },
{ "name": "绩效奖金", "amount": 2000.00 },
{ "name": "迟到扣款", "amount": -100.00 },
{ "name": "社保扣除", "amount": -1575.00 },
{ "name": "公积金扣除", "amount": -1800.00 },
{ "name": "个人所得税", "amount": -412.50 }
\],
"grossSalary": 19800.00,
"netSalary": 15912.50,
"trend": \[
{ "month": "2024-02", "netSalary": 15200.00 },
{ "month": "2024-03", "netSalary": 15500.00 },
{ "month": "2024-04", "netSalary": 15600.00 },
{ "month": "2024-05", "netSalary": 15800.00 },
{ "month": "2024-06", "netSalary": 16000.00 },
{ "month": "2024-07", "netSalary": 15912.50 }
\]
}
}

## 10关键技术设计

### 10-1 薪资计算引擎方案

采用规则引擎 + 公式表达式分离设计：

*   固定收入项：直接取 `employee_salary` 表对应字段值
*   变动收入项：使用 Aviator 表达式引擎，公式示例：`performanceBase * performanceCoeff`、`hourlyRate * multiplier * overtimeHours`
*   考勤扣款项：从 `attendance_stat` 拉取数据按规则计算：`50 * lateCount`、`dailyWage * leaveDays`
*   社保公积金：基数 × 比例（养老 8% / 医疗 2% / 失业 0.5% / 公积金 12%）
*   个税：累计预扣法，按年累计应纳税额查找对应税率区间
*   试用期员工：基本工资与津贴按 `probationRatio` 折算，社保公积金按全额基数

### 10-2 异常检测规则方案

规则 1：当月请假天数 &gt; 15 天         → 黄色预警
规则 2：当月加班费 &gt; 50 小时          → 黄色预警
规则 3：薪资较上月变动 &gt; 30%          → 红色预警（需 HR 确认）
规则 4：新入职员工未设置薪资档案       → 红色阻断（无法计算，跳过）
规则 5：实发工资 ≤ 0                  → 红色阻断（数据异常）
异常处理：
黄色预警：标记但可继续提交审批
红色预警：必须确认后才能提交
红色阻断：跳过该员工，批次可继续

### 10-3 薪资数据安全方案

*   薪资档案、明细表访问全程审计日志，记录操作人、时间、IP
*   工资条查看需二次验证（短信验证码 / 密码），token 有效期 5 分钟，单次使用
*   薪资列表导出需 HR 申请 + 财务审批，导出文件水印（操作人 + 时间）
*   接口层基于角色控制：HR 看全量、财务看汇总、员工仅看本人
*   数据库敏感字段（如 `base_salary`）可选 AES-256 加密存储

### 10-4 个税累计预扣法计算

累计预扣预缴应纳税所得额 =
累计收入 - 累计免税收入 - 累计减除费用 - 累计专项扣除 - 累计专项附加扣除
本期应预扣预缴税额 =
(累计预扣预缴应纳税所得额 × 预扣率 - 速算扣除数) - 累计已预扣税额
预扣率表（综合所得）：
不超过 36000 元            3%      0
36000-144000 元           10%    2520
144000-300000 元          20%   16920
300000-420000 元          25%   31920
420000-660000 元          30%   52920
660000-960000 元          35%   85920
超过 960000 元            45%  181920

### 10-5 数据可视化方案（AntV）

*   折线图：`@antv/g2 Line`，薪资成本月度趋势（应发/实发双线）
*   柱状图：`@antv/g2 Column`，部门薪资分布与社保公积金对比
*   饼图：`@antv/g2 Pie`，薪资构成占比
*   直方图：`@antv/g2 Histogram`，薪资变动分布
*   个人趋势图：员工"我的薪资"页使用 `Line` 展示近 6 月实发工资
*   异常看板：HR 预览页突出展示黄色/红色异常员工列表，支持快速跳转调整

## 11排期

01

需求评审评审产品规格，确认账套字段、异常规则、审批流

1 天

02

技术方案完成系分评审，确认计算引擎与数据库方案

2 天

03

数据库开发建表、索引、初始化账套与税率配置

2 天

04

后端开发账套、薪资档案、核算引擎、异常检测、审批

10 天

05

前端开发账套配置、核算预览、工资条、AntV 看板

9 天

06

联调测试前后端联调、核算准确性测试、异常场景测试

5 天

07

回归上线全量回归、预发验证、正式上线

2 天

总预估工期 31 个工作日

HRMS-ATT-SAL-SAD · v1.0 END OF DOCUMENT