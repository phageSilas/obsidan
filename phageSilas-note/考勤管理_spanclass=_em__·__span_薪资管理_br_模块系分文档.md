
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
  - id: Long "主键ID"
  - name: String "考勤组名称"
  - memberType: MemberType "成员类型"
  - shiftType: ShiftType "班次类型"
  - workStart: LocalTime "上班时间"
  - workEnd: LocalTime "下班时间"
  - restStart: LocalTime "午休开始时间"
  - restEnd: LocalTime "午休结束时间"
  - flexibleRange: String "弹性范围"
  - lateThreshold: Integer "迟到阈值（分钟）"
  - earlyLeaveThreshold: Integer "早退阈值（分钟）"
}

class WorkSchedule {
  - id: Long "主键ID"
  - groupId: Long "考勤组ID"
  - date: LocalDate "日期"
  - dayType: DayType "日期类型"
}

enum DayType {
  STANDARD_WORKDAY "标准工作日"
  REST_DAY "休息日"
  HOLIDAY "法定节假日"
}

class AttendanceRecord {
  - id: Long "主键ID"
  - employeeId: Long "员工ID"
  - groupId: Long "考勤组ID"
  - recordDate: LocalDate "打卡日期"
  - clockInTime: LocalDateTime "上班打卡时间"
  - clockOutTime: LocalDateTime "下班打卡时间"
  - clockInStatus: ClockStatus "上班状态"
  - clockOutStatus: ClockStatus "下班状态"
  - ipAddress: String "打卡IP地址"
  - gpsLocation: String "GPS位置"
}

enum ClockStatus {
  NORMAL "正常"
  LATE "迟到"
  EARLY_LEAVE "早退"
  ABSENCE "旷工"
  MISSING "缺卡"
}

class LeaveType {
  - id: Long "主键ID"
  - name: String "请假类型名称"
  - hasBalance: Boolean "是否管理余额"
  - needProof: Boolean "是否需要证明材料"
}

class LeaveBalance {
  - id: Long "主键ID"
  - employeeId: Long "员工ID"
  - leaveTypeId: Long "请假类型ID"
  - year: Integer "年份"
  - totalDays: BigDecimal "总天数"
  - usedDays: BigDecimal "已使用天数"
  - remainingDays: BigDecimal "剩余天数"
}

class LeaveRequest {
  - id: Long "主键ID"
  - employeeId: Long "员工ID"
  - leaveTypeId: Long "请假类型ID"
  - startTime: LocalDateTime "开始时间"
  - endTime: LocalDateTime "结束时间"
  - days: BigDecimal "请假天数"
  - reason: String "请假事由"
  - attachmentUrl: String "附件URL"
  - status: ApprovalStatus "审批状态"
}

class AttendanceStat {
  - id: Long "主键ID"
  - employeeId: Long "员工ID"
  - statMonth: String "统计月份"
  - shouldAttendDays: Integer "应出勤天数"
  - actualAttendDays: Integer "实际出勤天数"
  - lateCount: Integer "迟到次数"
  - earlyLeaveCount: Integer "早退次数"
  - absenceDays: BigDecimal "旷工天数"
  - leaveDays: BigDecimal "请假天数"
  - overtimeHours: BigDecimal "加班时长"
}

class Employee {
  - id: Long "主键ID"
  - employeeNo: String "工号"
  - account: String "系统账号"
  - status: EmployeeStatus "在职状态"
  - hireDate: LocalDate "入职日期"
  - createTime: LocalDateTime "创建时间"
}

enum EmployeeStatus {
  PROBATION "试用期"
  REGULAR "正式"
  PENDING_LEAVE "待离职"
  RESIGNED "已离职"
}

AttendanceGroup *-- WorkSchedule
AttendanceRecord --> AttendanceGroup
LeaveBalance --> LeaveType
LeaveRequest --> LeaveType
AttendanceRecord --> Employee
LeaveRequest --> Employee
LeaveBalance --> Employee
AttendanceStat --> Employee
@enduml


```
![[image-13.png|629x339]]

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

```
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
```
![[image-11.png]]
### 7-4 月度考勤统计时序图

```
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
```
![[image-12.png]]
## 8数据库设计

### 8-0 数据库设计口径

本模块数据库设计基于 `01-HRMS全局开发规范.md` 的主数据与命名规范：

*   人资业务表统一使用 `hr_` 前缀。
*   员工、部门、职位、系统账号、文件、审批流不重复建主表，统一依赖 `hr_employee`、`sys_dept`、`sys_post`、`sys_user`、`sys_file`、`hr_approval_instance`、`hr_approval_task`。
*   核心业务表统一包含公共审计字段：`create_by`、`create_time`、`update_by`、`update_time`、`is_deleted`、`version`、`remark`。
*   状态字段返回编码值，前端通过字典或枚举映射展示文案。

公共字段模板如下，后续表结构均默认包含：

```sql
`create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
`version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
`remark` VARCHAR(500) DEFAULT NULL COMMENT '备注'
```

### 8-1 考勤组表 hr_attendance_group

用于定义一组适用相同考勤规则的员工集合，支持固定班、弹性班和排班制。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_name` VARCHAR(64) NOT NULL COMMENT '考勤组名称',
  `shift_type` VARCHAR(32) NOT NULL COMMENT '班次类型：FIXED/FLEXIBLE/SCHEDULED',
  `work_start_time` TIME NOT NULL COMMENT '上班时间',
  `work_end_time` TIME NOT NULL COMMENT '下班时间',
  `rest_start_time` TIME DEFAULT NULL COMMENT '午休开始',
  `rest_end_time` TIME DEFAULT NULL COMMENT '午休结束',
  `flexible_start_time` TIME DEFAULT NULL COMMENT '弹性最早打卡',
  `flexible_end_time` TIME DEFAULT NULL COMMENT '弹性最晚打卡',
  `late_threshold_minutes` INT NOT NULL DEFAULT 15 COMMENT '迟到阈值，单位分钟',
  `early_leave_threshold_minutes` INT NOT NULL DEFAULT 15 COMMENT '早退阈值，单位分钟',
  `clock_ip_whitelist` VARCHAR(500) DEFAULT NULL COMMENT 'IP白名单，多个用逗号分隔',
  `clock_gps_scope` VARCHAR(500) DEFAULT NULL COMMENT 'GPS打卡范围配置',
  `monthly_correction_limit` INT NOT NULL DEFAULT 2 COMMENT '每月补卡次数上限',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启停状态：1启用，0禁用',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_attendance_group_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤组表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `group_name` | `VARCHAR(64)` | 是 | 考勤组名称 | 如标准工时组、弹性工时组 |
| `shift_type` | `VARCHAR(32)` | 是 | 班次类型 | `FIXED`、`FLEXIBLE`、`SCHEDULED` |
| `work_start_time` | `TIME` | 是 | 上班时间 | 如 `09:00` |
| `work_end_time` | `TIME` | 是 | 下班时间 | 如 `18:00` |
| `rest_start_time` | `TIME` | 否 | 午休开始 | 默认可为 `12:00` |
| `rest_end_time` | `TIME` | 否 | 午休结束 | 默认可为 `13:00` |
| `flexible_start_time` | `TIME` | 否 | 弹性最早打卡 | 弹性班适用 |
| `flexible_end_time` | `TIME` | 否 | 弹性最晚打卡 | 弹性班适用 |
| `late_threshold_minutes` | `INT` | 是 | 迟到阈值 | 默认 15 分钟 |
| `early_leave_threshold_minutes` | `INT` | 是 | 早退阈值 | 默认 15 分钟 |
| `clock_ip_whitelist` | `VARCHAR(500)` | 否 | IP 白名单 | 多个 IP 用逗号分隔 |
| `clock_gps_scope` | `VARCHAR(500)` | 否 | GPS 打卡范围 | 可记录中心点与半径配置 |
| `monthly_correction_limit` | `INT` | 是 | 每月补卡次数上限 | 产品规则默认 2 次 |
| `status` | `TINYINT` | 是 | 启停状态 | `1` 启用，`0` 禁用 |

建议索引：`idx_hr_attendance_group_status(status)`。

### 8-2 考勤组成员分支表 hr_attendance_group_member

用于承接考勤组适用范围，可按部门、职位或员工指定。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_group_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
  `member_type` VARCHAR(32) NOT NULL COMMENT '成员类型：DEPT/POST/EMPLOYEE',
  `member_id` BIGINT UNSIGNED NOT NULL COMMENT '成员ID，按成员类型关联部门、职位或员工',
  `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1有效，0失效',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_grp_member` (`group_id`, `member_type`, `member_id`, `is_deleted`),
  KEY `idx_hr_att_grp_member_member` (`member_type`, `member_id`),
  KEY `idx_hr_att_grp_member_group` (`group_id`),
  CONSTRAINT `fk_hr_att_grp_member_group` FOREIGN KEY (`group_id`) REFERENCES `hr_attendance_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤组成员分支表';
```

| 字段名              | 类型                | 必填  | 字段作用  | 约束与说明                                              |
| ---------------- | ----------------- | --- | ----- | -------------------------------------------------- |
| `id`             | `BIGINT UNSIGNED` | 是   | 主键ID  | 自增主键                                               |
| `group_id`       | `BIGINT UNSIGNED` | 是   | 考勤组ID | 关联 `hr_attendance_group.id`                        |
| `member_type`    | `VARCHAR(32)`     | 是   | 成员类型  | `DEPT`、`POST`、`EMPLOYEE`                           |
| `member_id`      | `BIGINT UNSIGNED` | 是   | 成员ID  | 按类型关联 `sys_dept.id`、`sys_post.id`、`hr_employee.id` |
| `effective_date` | `DATE`            | 否   | 生效日期  | 支持未来生效                                             |
| `expire_date`    | `DATE`            | 否   | 失效日期  | 离职或调整考勤组时回填                                        |
| `status`         | `TINYINT`         | 是   | 状态    | `1` 有效，`0` 失效                                      |

建议唯一索引：`uk_hr_attendance_group_member(group_id, member_type, member_id, is_deleted)`。

### 8-3 工作日配置表 hr_attendance_calendar

用于配置考勤组维度的标准工作日、休息日和法定节假日。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_calendar` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
  `calendar_date` DATE NOT NULL COMMENT '日期',
  `day_type` VARCHAR(32) NOT NULL COMMENT '日期类型：WORKDAY/REST_DAY/HOLIDAY',
  `source_type` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '配置来源：MANUAL/SYSTEM_IMPORT',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_cal_group_date` (`group_id`, `calendar_date`, `is_deleted`),
  KEY `idx_hr_att_cal_group` (`group_id`),
  KEY `idx_hr_att_cal_date` (`calendar_date`),
  CONSTRAINT `fk_hr_att_cal_group` FOREIGN KEY (`group_id`) REFERENCES `hr_attendance_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日配置表';
```

| 字段名             | 类型                | 必填  | 字段作用  | 约束与说明                          |
| --------------- | ----------------- | --- | ----- | ------------------------------ |
| `id`            | `BIGINT UNSIGNED` | 是   | 主键ID  | 自增主键                           |
| `group_id`      | `BIGINT UNSIGNED` | 是   | 考勤组ID | 关联 `hr_attendance_group.id`    |
| `calendar_date` | `DATE`            | 是   | 日期    | 工作日判断主维度                       |
| `day_type`      | `VARCHAR(32)`     | 是   | 日期类型  | `WORKDAY`、`REST_DAY`、`HOLIDAY` |
| `source_type`   | `VARCHAR(32)`     | 是   | 配置来源  | `MANUAL`、`SYSTEM_IMPORT`       |

建议唯一索引：`uk_hr_attendance_calendar_group_date(group_id, calendar_date, is_deleted)`。

### 8-4 打卡记录表 hr_attendance_record

记录员工网页端上下班打卡结果，为考勤统计与薪资扣款提供明细来源。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `group_id` BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
  `record_date` DATE NOT NULL COMMENT '打卡日期',
  `clock_in_time` DATETIME DEFAULT NULL COMMENT '上班打卡时间',
  `clock_out_time` DATETIME DEFAULT NULL COMMENT '下班打卡时间',
  `clock_in_status` VARCHAR(32) DEFAULT NULL COMMENT '上班状态：NORMAL/LATE/MISSING/ABSENCE',
  `clock_out_status` VARCHAR(32) DEFAULT NULL COMMENT '下班状态：NORMAL/EARLY_LEAVE/MISSING/ABSENCE',
  `clock_in_ip` VARCHAR(64) DEFAULT NULL COMMENT '上班打卡IP',
  `clock_out_ip` VARCHAR(64) DEFAULT NULL COMMENT '下班打卡IP',
  `clock_in_gps` VARCHAR(128) DEFAULT NULL COMMENT '上班打卡GPS',
  `clock_out_gps` VARCHAR(128) DEFAULT NULL COMMENT '下班打卡GPS',
  `device_info` VARCHAR(255) DEFAULT NULL COMMENT '设备信息',
  `correction_status` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '补卡状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_rec_emp_date` (`employee_id`, `record_date`, `is_deleted`),
  KEY `idx_hr_att_rec_group_date` (`group_id`, `record_date`),
  KEY `idx_hr_att_rec_employee` (`employee_id`),
  CONSTRAINT `fk_hr_att_rec_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_att_rec_group` FOREIGN KEY (`group_id`) REFERENCES `hr_attendance_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打卡记录表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `group_id` | `BIGINT UNSIGNED` | 是 | 考勤组ID | 关联 `hr_attendance_group.id` |
| `record_date` | `DATE` | 是 | 打卡日期 | 每人每天唯一 |
| `clock_in_time` | `DATETIME` | 否 | 上班打卡时间 | 未打卡为空 |
| `clock_out_time` | `DATETIME` | 否 | 下班打卡时间 | 未打卡为空 |
| `clock_in_status` | `VARCHAR(32)` | 否 | 上班状态 | `NORMAL`、`LATE`、`MISSING`、`ABSENCE` |
| `clock_out_status` | `VARCHAR(32)` | 否 | 下班状态 | `NORMAL`、`EARLY_LEAVE`、`MISSING`、`ABSENCE` |
| `clock_in_ip` / `clock_out_ip` | `VARCHAR(64)` | 否 | 打卡 IP | 审计与范围校验 |
| `clock_in_gps` / `clock_out_gps` | `VARCHAR(128)` | 否 | GPS 坐标 | 范围校验 |
| `device_info` | `VARCHAR(255)` | 否 | 设备信息 | 用于风控审计 |
| `correction_status` | `VARCHAR(32)` | 是 | 补卡状态 | `NONE`、`PENDING`、`APPROVED`、`REJECTED` |

建议唯一索引：`uk_hr_attendance_record_emp_date(employee_id, record_date, is_deleted)`。

### 8-5 补卡申请表 hr_attendance_correction_request

用于记录每月最多 2 次的补卡申请，审批任务由审批中心统一承载。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_correction_request` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_no` VARCHAR(64) NOT NULL COMMENT '补卡申请单号',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '申请员工ID',
  `record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '原打卡记录ID',
  `correction_date` DATE NOT NULL COMMENT '补卡日期',
  `correction_type` VARCHAR(32) NOT NULL COMMENT '补卡类型：CLOCK_IN/CLOCK_OUT',
  `correction_time` DATETIME NOT NULL COMMENT '补卡时间',
  `reason` VARCHAR(500) NOT NULL COMMENT '补卡原因',
  `approval_instance_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批实例ID',
  `approval_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_corr_request_no` (`request_no`),
  KEY `idx_hr_att_corr_emp_date` (`employee_id`, `correction_date`),
  KEY `idx_hr_att_corr_record` (`record_id`),
  KEY `idx_hr_att_corr_approval` (`approval_instance_id`),
  CONSTRAINT `fk_hr_att_corr_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_att_corr_record` FOREIGN KEY (`record_id`) REFERENCES `hr_attendance_record` (`id`),
  CONSTRAINT `fk_hr_att_corr_approval` FOREIGN KEY (`approval_instance_id`) REFERENCES `hr_approval_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补卡申请表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `request_no` | `VARCHAR(64)` | 是 | 补卡申请单号 | 全局唯一 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 申请员工ID | 关联 `hr_employee.id` |
| `record_id` | `BIGINT UNSIGNED` | 否 | 原打卡记录ID | 关联 `hr_attendance_record.id` |
| `correction_date` | `DATE` | 是 | 补卡日期 | 用于月度次数限制 |
| `correction_type` | `VARCHAR(32)` | 是 | 补卡类型 | `CLOCK_IN`、`CLOCK_OUT` |
| `correction_time` | `DATETIME` | 是 | 补卡时间 | 审批通过后回写打卡记录 |
| `reason` | `VARCHAR(500)` | 是 | 补卡原因 | 审批表单展示 |
| `approval_instance_id` | `BIGINT UNSIGNED` | 否 | 审批实例ID | 关联 `hr_approval_instance.id` |
| `approval_status` | `VARCHAR(32)` | 是 | 审批状态 | 引用 `approval_status` 字典 |

建议索引：`uk_hr_attendance_correction_request_no(request_no)`、`idx_hr_attendance_correction_employee_month(employee_id, correction_date)`。

### 8-6 请假类型表 hr_leave_type

用于配置年假、病假、事假、婚假、产假、丧假、调休等类型及证明材料规则。

```sql
CREATE TABLE IF NOT EXISTS `hr_leave_type` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `leave_code` VARCHAR(32) NOT NULL COMMENT '请假类型编码',
  `leave_name` VARCHAR(32) NOT NULL COMMENT '请假类型名称',
  `has_balance` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否管理余额',
  `need_proof` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要证明',
  `proof_required_days` DECIMAL(6,1) DEFAULT NULL COMMENT '超过该天数必须上传证明',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_leave_type_code` (`leave_code`, `is_deleted`),
  KEY `idx_hr_leave_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假类型表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `leave_code` | `VARCHAR(32)` | 是 | 请假类型编码 | 如 `ANNUAL`、`SICK`、`PERSONAL` |
| `leave_name` | `VARCHAR(32)` | 是 | 请假类型名称 | 前端展示文案 |
| `has_balance` | `TINYINT(1)` | 是 | 是否管理余额 | 年假、调休为 `1` |
| `need_proof` | `TINYINT(1)` | 是 | 是否需要证明 | 病假、婚假、产假等适用 |
| `proof_required_days` | `DECIMAL(6,1)` | 否 | 超过该天数必须上传证明 | 病假超过 1 天适用 |
| `sort_no` | `INT` | 是 | 排序号 | 列表展示排序 |
| `status` | `TINYINT` | 是 | 状态 | `1` 启用，`0` 禁用 |

建议唯一索引：`uk_hr_leave_type_code(leave_code, is_deleted)`。

### 8-7 假期余额表 hr_leave_balance

用于管理员工年度年假、调休等需要余额控制的假期。

```sql
CREATE TABLE IF NOT EXISTS `hr_leave_balance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `leave_type_id` BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
  `balance_year` SMALLINT NOT NULL COMMENT '余额年份',
  `total_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '总额度',
  `used_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '已使用额度',
  `frozen_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '审批中冻结额度',
  `remaining_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '剩余额度',
  `expire_date` DATE DEFAULT NULL COMMENT '过期日期',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_leave_bal_emp_type_year` (`employee_id`, `leave_type_id`, `balance_year`, `is_deleted`),
  KEY `idx_hr_leave_bal_employee` (`employee_id`),
  KEY `idx_hr_leave_bal_type` (`leave_type_id`),
  CONSTRAINT `fk_hr_leave_bal_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_leave_bal_type` FOREIGN KEY (`leave_type_id`) REFERENCES `hr_leave_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='假期余额表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `leave_type_id` | `BIGINT UNSIGNED` | 是 | 请假类型ID | 关联 `hr_leave_type.id` |
| `balance_year` | `SMALLINT` | 是 | 余额年份 | 年假按自然年计算 |
| `total_days` | `DECIMAL(6,1)` | 是 | 总额度 | 支持 0.5 天 |
| `used_days` | `DECIMAL(6,1)` | 是 | 已使用额度 | 审批通过后扣减 |
| `frozen_days` | `DECIMAL(6,1)` | 是 | 审批中冻结额度 | 审批拒绝/撤回释放 |
| `remaining_days` | `DECIMAL(6,1)` | 是 | 剩余额度 | 可用额度 |
| `expire_date` | `DATE` | 否 | 过期日期 | 调休当月及次月有效 |

建议唯一索引：`uk_hr_leave_balance_emp_type_year(employee_id, leave_type_id, balance_year, is_deleted)`。

### 8-8 请假申请表 hr_leave_request

用于记录请假业务单据，请假审批任务由审批中心承载。

```sql
CREATE TABLE IF NOT EXISTS `hr_leave_request` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_no` VARCHAR(64) NOT NULL COMMENT '请假申请单号',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '申请员工ID',
  `leave_type_id` BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `start_period` VARCHAR(16) NOT NULL COMMENT '开始时段：AM/PM',
  `end_period` VARCHAR(16) NOT NULL COMMENT '结束时段：AM/PM',
  `leave_days` DECIMAL(6,1) NOT NULL COMMENT '请假天数',
  `reason` VARCHAR(500) NOT NULL COMMENT '请假事由',
  `handover_employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '工作交接人',
  `attachment_file_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '证明附件ID',
  `approval_instance_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批实例ID',
  `approval_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_leave_request_no` (`request_no`),
  KEY `idx_hr_leave_req_employee` (`employee_id`),
  KEY `idx_hr_leave_req_type` (`leave_type_id`),
  KEY `idx_hr_leave_req_status` (`approval_status`),
  KEY `idx_hr_leave_req_handover` (`handover_employee_id`),
  KEY `idx_hr_leave_req_file` (`attachment_file_id`),
  KEY `idx_hr_leave_req_approval` (`approval_instance_id`),
  CONSTRAINT `fk_hr_leave_req_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_leave_req_type` FOREIGN KEY (`leave_type_id`) REFERENCES `hr_leave_type` (`id`),
  CONSTRAINT `fk_hr_leave_req_handover` FOREIGN KEY (`handover_employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_leave_req_file` FOREIGN KEY (`attachment_file_id`) REFERENCES `sys_file` (`id`),
  CONSTRAINT `fk_hr_leave_req_approval` FOREIGN KEY (`approval_instance_id`) REFERENCES `hr_approval_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `request_no` | `VARCHAR(64)` | 是 | 请假申请单号 | 全局唯一 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 申请员工ID | 关联 `hr_employee.id` |
| `leave_type_id` | `BIGINT UNSIGNED` | 是 | 请假类型ID | 关联 `hr_leave_type.id` |
| `start_time` | `DATETIME` | 是 | 开始时间 | 支持上午/下午半天 |
| `end_time` | `DATETIME` | 是 | 结束时间 | 不得早于开始时间 |
| `start_period` / `end_period` | `VARCHAR(16)` | 是 | 开始/结束时段 | `AM`、`PM` |
| `leave_days` | `DECIMAL(6,1)` | 是 | 请假天数 | 系统计算，支持 0.5 天 |
| `reason` | `VARCHAR(500)` | 是 | 请假事由 | 审批表单展示 |
| `handover_employee_id` | `BIGINT UNSIGNED` | 否 | 工作交接人 | 关联 `hr_employee.id` |
| `attachment_file_id` | `BIGINT UNSIGNED` | 条件必填 | 证明附件 | 关联 `sys_file.id` |
| `approval_instance_id` | `BIGINT UNSIGNED` | 否 | 审批实例ID | 关联 `hr_approval_instance.id` |
| `approval_status` | `VARCHAR(32)` | 是 | 审批状态 | 引用 `approval_status` 字典 |

建议索引：`uk_hr_leave_request_no(request_no)`、`idx_hr_leave_request_employee(employee_id)`、`idx_hr_leave_request_status(approval_status)`。

### 8-9 加班申请表 hr_overtime_request

用于记录已审批加班数据，并作为调休余额与薪资加班费的数据来源。

```sql
CREATE TABLE IF NOT EXISTS `hr_overtime_request` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_no` VARCHAR(64) NOT NULL COMMENT '加班申请单号',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '申请员工ID',
  `start_time` DATETIME NOT NULL COMMENT '加班开始时间',
  `end_time` DATETIME NOT NULL COMMENT '加班结束时间',
  `overtime_hours` DECIMAL(6,1) NOT NULL COMMENT '加班时长',
  `convert_leave_hours` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '可转调休时长',
  `reason` VARCHAR(500) NOT NULL COMMENT '加班原因',
  `approval_instance_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批实例ID',
  `approval_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_overtime_request_no` (`request_no`),
  KEY `idx_hr_overtime_emp_time` (`employee_id`, `start_time`),
  KEY `idx_hr_overtime_approval` (`approval_instance_id`),
  CONSTRAINT `fk_hr_overtime_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_overtime_approval` FOREIGN KEY (`approval_instance_id`) REFERENCES `hr_approval_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='加班申请表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `request_no` | `VARCHAR(64)` | 是 | 加班申请单号 | 全局唯一 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 申请员工ID | 关联 `hr_employee.id` |
| `start_time` | `DATETIME` | 是 | 加班开始时间 | 用于计算时长 |
| `end_time` | `DATETIME` | 是 | 加班结束时间 | 不得早于开始时间 |
| `overtime_hours` | `DECIMAL(6,1)` | 是 | 加班时长 | 薪资加班费来源 |
| `convert_leave_hours` | `DECIMAL(6,1)` | 是 | 可转调休时长 | 调休余额来源 |
| `reason` | `VARCHAR(500)` | 是 | 加班原因 | 审批表单展示 |
| `approval_instance_id` | `BIGINT UNSIGNED` | 否 | 审批实例ID | 关联 `hr_approval_instance.id` |
| `approval_status` | `VARCHAR(32)` | 是 | 审批状态 | 引用 `approval_status` 字典 |

建议索引：`uk_hr_overtime_request_no(request_no)`、`idx_hr_overtime_request_employee_time(employee_id, start_time)`。

### 8-10 月度考勤统计表 hr_attendance_month_stat

用于沉淀个人月度考勤统计结果，并向薪资核算模块提供考勤扣款、加班费与异常检测依据。

```sql
CREATE TABLE IF NOT EXISTS `hr_attendance_month_stat` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '部门ID',
  `stat_month` CHAR(7) NOT NULL COMMENT '统计月份，格式yyyy-MM',
  `should_attend_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '应出勤天数',
  `actual_attend_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
  `late_count` INT NOT NULL DEFAULT 0 COMMENT '迟到次数',
  `early_leave_count` INT NOT NULL DEFAULT 0 COMMENT '早退次数',
  `absence_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '旷工天数',
  `leave_days` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '请假天数',
  `overtime_hours` DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '加班小时数',
  `annual_leave_remaining` DECIMAL(6,1) DEFAULT NULL COMMENT '年假剩余额度',
  `stat_status` VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT '统计状态：GENERATED/LOCKED',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_month_stat_emp_month` (`employee_id`, `stat_month`, `is_deleted`),
  KEY `idx_hr_att_month_stat_dept_month` (`dept_id`, `stat_month`),
  CONSTRAINT `fk_hr_att_month_stat_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_att_month_stat_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度考勤统计表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `dept_id` | `BIGINT UNSIGNED` | 是 | 部门ID | 关联 `sys_dept.id`，保留统计快照 |
| `stat_month` | `CHAR(7)` | 是 | 统计月份 | 格式 `yyyy-MM` |
| `should_attend_days` | `DECIMAL(6,1)` | 是 | 应出勤天数 | 当月工作日数 |
| `actual_attend_days` | `DECIMAL(6,1)` | 是 | 实际出勤天数 | 有效出勤天数 |
| `late_count` | `INT` | 是 | 迟到次数 | 薪资扣款来源 |
| `early_leave_count` | `INT` | 是 | 早退次数 | 薪资扣款来源 |
| `absence_days` | `DECIMAL(6,1)` | 是 | 旷工天数 | 薪资扣款来源 |
| `leave_days` | `DECIMAL(6,1)` | 是 | 请假天数 | 薪资扣款来源 |
| `overtime_hours` | `DECIMAL(6,1)` | 是 | 加班小时数 | 薪资加班费来源 |
| `annual_leave_remaining` | `DECIMAL(6,1)` | 否 | 年假剩余额度 | 个人中心展示 |
| `stat_status` | `VARCHAR(32)` | 是 | 统计状态 | `GENERATED`、`LOCKED` |

建议唯一索引：`uk_hr_attendance_month_stat_emp_month(employee_id, stat_month, is_deleted)`。

### 8-11 主要表关系

| 业务对象 | 表名 | 关联主数据/上游表 | 说明 |
| --- | --- | --- | --- |
| 考勤组 | `hr_attendance_group` | - | 考勤规则主配置 |
| 考勤组成员 | `hr_attendance_group_member` | `sys_dept` / `sys_post` / `hr_employee` | 按部门、职位、员工绑定适用范围 |
| 工作日配置 | `hr_attendance_calendar` | `hr_attendance_group` | 判断工作日、休息日、节假日 |
| 打卡记录 | `hr_attendance_record` | `hr_employee`、`hr_attendance_group` | 网页打卡明细 |
| 补卡申请 | `hr_attendance_correction_request` | `hr_employee`、`hr_approval_instance` | 补卡审批业务单据 |
| 请假申请 | `hr_leave_request` | `hr_employee`、`hr_leave_type`、`sys_file`、`hr_approval_instance` | 请假审批业务单据 |
| 加班申请 | `hr_overtime_request` | `hr_employee`、`hr_approval_instance` | 调休与加班费数据来源 |
| 月度统计 | `hr_attendance_month_stat` | `hr_employee`、`sys_dept` | 薪资核算读取口径 |

## 9API 设计

### 9-0 API 统一约定

*   返回体统一使用 `Result<T>`。
*   分页接口统一使用 `pageNum`、`pageSize`、`total`、`records`。
*   日期格式使用 `yyyy-MM-dd`，时间格式使用 `yyyy-MM-dd HH:mm:ss`。
*   需要审批的接口统一调用审批中心 `/approval/start`，并在业务表回填 `approval_instance_id` 与 `approval_status`。

### 9-1 考勤组与规则接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 查询考勤组分页 | `/api/v1/attendance/groups` | `GET` | 按名称、状态分页查询 |
| 获取考勤组详情 | `/api/v1/attendance/groups/{id}` | `GET` | 返回规则、成员和工作日配置摘要 |
| 创建考勤组 | `/api/v1/attendance/groups` | `POST` | 新增考勤组规则 |
| 更新考勤组 | `/api/v1/attendance/groups/{id}` | `PUT` | 更新考勤规则 |
| 启停考勤组 | `/api/v1/attendance/groups/{id}/status` | `PATCH` | 修改 `status` |
| 删除考勤组 | `/api/v1/attendance/groups/{id}` | `DELETE` | 逻辑删除 |
| 保存考勤组成员 | `/api/v1/attendance/groups/{id}/members` | `PUT` | 覆盖保存部门/职位/员工成员 |
| 保存工作日配置 | `/api/v1/attendance/groups/{id}/calendar` | `PUT` | 批量保存工作日、休息日、节假日 |

查询考勤组分页请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `keyword` | String | 否 | 考勤组名称模糊匹配 |
| `status` | Integer | 否 | `1` 启用，`0` 禁用 |
| `pageNum` | Integer | 否 | 页码，默认 1 |
| `pageSize` | Integer | 否 | 每页条数，默认 20 |

创建考勤组请求核心字段：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `groupName` | String | 是 | 考勤组名称 |
| `shiftType` | String | 是 | `FIXED` / `FLEXIBLE` / `SCHEDULED` |
| `workStartTime` | String | 是 | 上班时间，如 `09:00` |
| `workEndTime` | String | 是 | 下班时间，如 `18:00` |
| `lateThresholdMinutes` | Integer | 是 | 迟到阈值，默认 15 |
| `earlyLeaveThresholdMinutes` | Integer | 是 | 早退阈值，默认 15 |
| `memberList` | Array | 是 | 成员范围，包含 `memberType`、`memberId` |

### 9-2 打卡与补卡接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 员工打卡 | `/api/v1/attendance/clock` | `POST` | 当前登录员工网页端打卡 |
| 查询个人打卡日历 | `/api/v1/attendance/records/my-calendar` | `GET` | 查询个人每日打卡状态 |
| 查询打卡记录分页 | `/api/v1/attendance/records` | `GET` | HR/主管按权限查询 |
| 提交补卡申请 | `/api/v1/attendance/corrections` | `POST` | 创建补卡单并发起审批 |
| 查询补卡记录 | `/api/v1/attendance/corrections` | `GET` | 查询个人或权限范围内补卡记录 |

员工打卡请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `clockType` | String | 是 | `CLOCK_IN` / `CLOCK_OUT` |
| `ipAddress` | String | 是 | 客户端 IP，由网关或前端透传 |
| `gpsLocation` | String | 否 | 经纬度 |
| `deviceInfo` | String | 否 | 设备信息 |

员工打卡响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 8848,
    "clockTime": "2024-07-09 09:02:13",
    "clockStatus": "LATE",
    "statusDesc": "迟到"
  }
}
```

提交补卡申请请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `recordId` | Long | 否 | 原打卡记录 ID |
| `correctionDate` | Date | 是 | 补卡日期 |
| `correctionType` | String | 是 | `CLOCK_IN` / `CLOCK_OUT` |
| `correctionTime` | DateTime | 是 | 补卡时间 |
| `reason` | String | 是 | 补卡原因 |

### 9-3 请假与余额接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 查询请假类型 | `/api/v1/leaves/types` | `GET` | 获取启用的请假类型 |
| 查询假期余额 | `/api/v1/leaves/balances` | `GET` | 查询当前员工或指定员工余额 |
| 提交请假申请 | `/api/v1/leaves` | `POST` | 创建请假单并发起审批 |
| 查询请假记录 | `/api/v1/leaves` | `GET` | 分页查询请假记录 |
| 获取请假详情 | `/api/v1/leaves/{id}` | `GET` | 返回请假单与审批状态 |
| 撤回请假申请 | `/api/v1/leaves/{id}/withdraw` | `PATCH` | 审批完成前撤回 |

提交请假申请请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `leaveTypeId` | Long | 是 | 请假类型 ID |
| `startTime` | DateTime | 是 | 开始时间 |
| `endTime` | DateTime | 是 | 结束时间 |
| `startPeriod` | String | 是 | `AM` / `PM` |
| `endPeriod` | String | 是 | `AM` / `PM` |
| `reason` | String | 是 | 请假事由 |
| `attachmentFileId` | Long | 条件必填 | 病假、婚假、产假等证明材料，关联 `sys_file.id` |
| `handoverEmployeeId` | Long | 否 | 工作交接人员工 ID |

### 9-4 统计与可视化接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 生成月度考勤统计 | `/api/v1/attendance/stats/monthly/generate` | `POST` | 定时任务或 HR 手动触发 |
| 查询个人月度统计 | `/api/v1/attendance/stats/personal` | `GET` | 默认查询当前登录员工 |
| 查询部门月度统计 | `/api/v1/attendance/stats/departments` | `GET` | HR/主管按权限查询 |
| 查询考勤可视化数据 | `/api/v1/attendance/stats/visualization` | `GET` | 返回 AntV 数据集 |
| 薪资模块读取考勤汇总 | `/api/v1/attendance/stats/monthly/payroll-source` | `GET` | 按月份和员工范围返回锁定后的考勤统计 |

薪资模块读取考勤汇总请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `statMonth` | String | 是 | 统计月份，如 `2024-07` |
| `employeeIds` | Array<Long> | 否 | 指定员工 ID 集合 |
| `deptId` | Long | 否 | 指定部门 ID |
| `onlyLocked` | Boolean | 否 | 是否只返回已锁定统计，默认 `true` |

### 9-5 跨模块接口依赖

| 依赖场景 | 调用接口 | 方法 | 最小字段 |
| --- | --- | --- | --- |
| 获取员工简要信息 | `/employees/brief/{id}` | `GET` | `id, name, employeeNo, departmentId, departmentName, status` |
| 获取组织范围 | `/departments/tree` | `GET` | `id, name, parentId, children` |
| 请假/补卡发起审批 | `/approval/start` | `POST` | 入参 `bizType, bizId, applicantId`，返回 `taskId` |
| 附件上传后绑定证明 | 文件中心接口 | `POST` | 返回 `fileId`，写入 `attachment_file_id` |

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
*   幂等：`hr_attendance_month_stat` 表 `uk_hr_attendance_month_stat_emp_month` 唯一索引 + UPSERT，支持重跑
*   失败重试：单员工失败记录到 `hr_attendance_stat_failed_log`，人工补跑
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
salSvc -&gt; db : SELECT hr\_salary\_batch\_item + hr\_payslip
db --&gt; salSvc : 返回工资条数据
salSvc -&gt; db : UPDATE hr\_payslip SET first\_view\_time=NOW()
salSvc --&gt; fe : 返回工资条明细
fe --&gt; emp : 展示工资项目 + 趋势图
@enduml

## 8数据库设计

### 8-0 数据库设计口径

薪资模块数据库设计基于全局主数据与考勤统计结果扩展，不重复建设员工、部门、职位、系统账号、审批、附件等主表。

*   薪资账套使用 `template` 命名，兼容 `hr_employee.salary_template_id`。
*   员工薪资档案作为 `hr_employee.base_salary`、`hr_employee.salary_template_id` 的业务扩展表。
*   薪资核算读取 `hr_attendance_month_stat`，不直接扫描原始打卡明细。
*   工资条查看需要二次验证，并记录查看日志。
*   涉薪字段属于敏感字段，接口层需按角色和字段权限脱敏。

公共字段模板如下，后续表结构均默认包含：

```sql
`create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
`version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
`remark` VARCHAR(500) DEFAULT NULL COMMENT '备注'
```

### 8-1 薪资账套表 hr_salary_template

用于定义薪资计算模板，包括适用范围、生效日期和启停状态。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` VARCHAR(64) NOT NULL COMMENT '薪资账套名称',
  `scope_type` VARCHAR(32) NOT NULL COMMENT '适用范围：DEPT/POST/JOB_LEVEL/EMPLOYEE',
  `scope_value` VARCHAR(500) NOT NULL COMMENT '适用范围值',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_salary_template_status` (`status`),
  KEY `idx_hr_salary_template_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资账套表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `template_name` | `VARCHAR(64)` | 是 | 薪资账套名称 | 如标准职员工资 |
| `scope_type` | `VARCHAR(32)` | 是 | 适用范围 | `DEPT`、`POST`、`JOB_LEVEL`、`EMPLOYEE` |
| `scope_value` | `VARCHAR(500)` | 是 | 适用范围值 | 多个 ID 或编码用逗号分隔 |
| `effective_date` | `DATE` | 是 | 生效日期 | 控制账套启用时间 |
| `status` | `TINYINT` | 是 | 状态 | `1` 启用，`0` 停用 |

建议索引：`idx_hr_salary_template_status(status)`、`idx_hr_salary_template_effective_date(effective_date)`。

### 8-2 工资项目表 hr_salary_template_item

用于定义账套中的固定收入、变动收入、考勤扣款、社保、公积金、个税等工资项。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_template_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资账套ID',
  `item_code` VARCHAR(32) NOT NULL COMMENT '工资项目编码',
  `item_name` VARCHAR(64) NOT NULL COMMENT '工资项目名称',
  `item_type` VARCHAR(32) NOT NULL COMMENT '项目类型',
  `calc_rule` VARCHAR(1000) DEFAULT NULL COMMENT '计算规则或公式',
  `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必算项',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_sal_tpl_item_code` (`template_id`, `item_code`, `is_deleted`),
  KEY `idx_hr_sal_tpl_item_template` (`template_id`),
  CONSTRAINT `fk_hr_sal_tpl_item_template` FOREIGN KEY (`template_id`) REFERENCES `hr_salary_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资项目表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `template_id` | `BIGINT UNSIGNED` | 是 | 薪资账套ID | 关联 `hr_salary_template.id` |
| `item_code` | `VARCHAR(32)` | 是 | 工资项目编码 | 账套内唯一 |
| `item_name` | `VARCHAR(64)` | 是 | 工资项目名称 | 如基本工资、迟到扣款 |
| `item_type` | `VARCHAR(32)` | 是 | 项目类型 | `FIXED_INCOME`、`VARIABLE_INCOME`、`ATTENDANCE_DEDUCTION`、`SOCIAL_INSURANCE`、`HOUSING_FUND`、`TAX` |
| `calc_rule` | `VARCHAR(1000)` | 否 | 计算规则或公式 | 如 `50元 * 迟到次数` |
| `is_required` | `TINYINT(1)` | 是 | 是否必算项 | `1` 是，`0` 否 |
| `sort_no` | `INT` | 是 | 排序号 | 工资条展示顺序 |
| `status` | `TINYINT` | 是 | 状态 | `1` 启用，`0` 停用 |

建议唯一索引：`uk_hr_salary_template_item_code(template_id, item_code, is_deleted)`。

### 8-3 员工薪资档案表 hr_employee_salary_profile

用于维护每个员工独立的薪资档案，承接账套、基本工资、津贴基数、社保公积金基数、绩效基数和试用期比例。

```sql
CREATE TABLE IF NOT EXISTS `hr_employee_salary_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `template_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资账套ID',
  `base_salary` DECIMAL(12,2) NOT NULL COMMENT '基本工资',
  `allowance_base` DECIMAL(12,2) DEFAULT 0 COMMENT '津贴基数',
  `social_insurance_base` DECIMAL(12,2) NOT NULL COMMENT '社保基数',
  `housing_fund_base` DECIMAL(12,2) NOT NULL COMMENT '公积金基数',
  `performance_base` DECIMAL(12,2) DEFAULT 0 COMMENT '绩效基数',
  `probation_salary_ratio` DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '试用期薪资比例',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1生效，0失效',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_sal_profile_emp` (`employee_id`),
  KEY `idx_hr_emp_sal_profile_template` (`template_id`),
  KEY `idx_hr_emp_sal_profile_status` (`employee_id`, `status`, `is_deleted`),
  CONSTRAINT `fk_hr_emp_sal_profile_emp` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_emp_sal_profile_template` FOREIGN KEY (`template_id`) REFERENCES `hr_salary_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工薪资档案表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `template_id` | `BIGINT UNSIGNED` | 是 | 薪资账套ID | 关联 `hr_salary_template.id` |
| `base_salary` | `DECIMAL(12,2)` | 是 | 基本工资 | 与 `hr_employee.base_salary` 保持同步口径 |
| `allowance_base` | `DECIMAL(12,2)` | 否 | 津贴基数 | 默认 0 |
| `social_insurance_base` | `DECIMAL(12,2)` | 是 | 社保基数 | 社保个人扣除计算依据 |
| `housing_fund_base` | `DECIMAL(12,2)` | 是 | 公积金基数 | 公积金个人扣除计算依据 |
| `performance_base` | `DECIMAL(12,2)` | 否 | 绩效基数 | 默认 0 |
| `probation_salary_ratio` | `DECIMAL(5,2)` | 是 | 试用期薪资比例 | 如 `80.00`、`100.00` |
| `effective_date` | `DATE` | 是 | 生效日期 | 调薪生效口径 |
| `expire_date` | `DATE` | 否 | 失效日期 | 历史档案关闭时回填 |
| `status` | `TINYINT` | 是 | 状态 | `1` 生效，`0` 失效 |

建议索引：`uk_hr_employee_salary_profile_active(employee_id, status, is_deleted)`、`idx_hr_employee_salary_profile_template(template_id)`。

### 8-4 调薪历史表 hr_salary_adjustment_record

用于记录薪资档案变更历史，支撑审计与薪资较上月变动异常检测。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_adjustment_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `profile_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '薪资档案ID',
  `adjust_date` DATE NOT NULL COMMENT '调薪日期',
  `old_base_salary` DECIMAL(12,2) DEFAULT NULL COMMENT '调整前基本工资',
  `new_base_salary` DECIMAL(12,2) DEFAULT NULL COMMENT '调整后基本工资',
  `old_template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '调整前账套ID',
  `new_template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '调整后账套ID',
  `adjust_reason` VARCHAR(500) DEFAULT NULL COMMENT '调薪原因',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_adj_employee` (`employee_id`),
  KEY `idx_hr_sal_adj_profile` (`profile_id`),
  KEY `idx_hr_sal_adj_old_template` (`old_template_id`),
  KEY `idx_hr_sal_adj_new_template` (`new_template_id`),
  KEY `idx_hr_sal_adj_operator` (`operator_user_id`),
  CONSTRAINT `fk_hr_sal_adj_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_sal_adj_profile` FOREIGN KEY (`profile_id`) REFERENCES `hr_employee_salary_profile` (`id`),
  CONSTRAINT `fk_hr_sal_adj_old_template` FOREIGN KEY (`old_template_id`) REFERENCES `hr_salary_template` (`id`),
  CONSTRAINT `fk_hr_sal_adj_new_template` FOREIGN KEY (`new_template_id`) REFERENCES `hr_salary_template` (`id`),
  CONSTRAINT `fk_hr_sal_adj_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调薪历史表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `profile_id` | `BIGINT UNSIGNED` | 否 | 薪资档案ID | 关联 `hr_employee_salary_profile.id` |
| `adjust_date` | `DATE` | 是 | 调薪日期 | 薪资变动时间轴 |
| `old_base_salary` | `DECIMAL(12,2)` | 否 | 调整前基本工资 | 历史快照 |
| `new_base_salary` | `DECIMAL(12,2)` | 否 | 调整后基本工资 | 历史快照 |
| `old_template_id` | `BIGINT UNSIGNED` | 否 | 调整前账套ID | 关联旧账套 |
| `new_template_id` | `BIGINT UNSIGNED` | 否 | 调整后账套ID | 关联新账套 |
| `adjust_reason` | `VARCHAR(500)` | 否 | 调薪原因 | 调薪时必填 |
| `operator_user_id` | `BIGINT UNSIGNED` | 否 | 操作用户ID | 关联 `sys_user.id` |

建议索引：`idx_hr_salary_adjustment_employee(employee_id)`、`idx_hr_salary_adjustment_date(adjust_date)`。

### 8-5 薪资批次表 hr_salary_batch

用于管理每月薪资核算批次，状态与产品规格中的草稿、计算中、待确认、审批中、已通过、已发放、已驳回保持一致。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_batch` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` VARCHAR(64) NOT NULL COMMENT '薪资批次编号',
  `salary_month` CHAR(7) NOT NULL COMMENT '薪资月份，格式yyyy-MM',
  `scope_type` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '核算范围：ALL/DEPT/EMPLOYEE',
  `scope_value` VARCHAR(500) DEFAULT NULL COMMENT '核算范围值',
  `batch_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态',
  `approval_instance_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批实例ID',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '核算员工数',
  `total_gross_salary` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '应发总额',
  `total_net_salary` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '实发总额',
  `yellow_warning_count` INT NOT NULL DEFAULT 0 COMMENT '黄色预警数量',
  `red_warning_count` INT NOT NULL DEFAULT 0 COMMENT '红色预警数量',
  `block_count` INT NOT NULL DEFAULT 0 COMMENT '阻断异常数量',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_salary_batch_no` (`batch_no`),
  UNIQUE KEY `uk_hr_salary_batch_month_scope` (`salary_month`, `scope_type`, `scope_value`, `is_deleted`),
  KEY `idx_hr_salary_batch_approval` (`approval_instance_id`),
  CONSTRAINT `fk_hr_salary_batch_approval` FOREIGN KEY (`approval_instance_id`) REFERENCES `hr_approval_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资批次表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `batch_no` | `VARCHAR(64)` | 是 | 薪资批次编号 | 全局唯一 |
| `salary_month` | `CHAR(7)` | 是 | 薪资月份 | 格式 `yyyy-MM` |
| `scope_type` | `VARCHAR(32)` | 是 | 核算范围 | `ALL`、`DEPT`、`EMPLOYEE` |
| `scope_value` | `VARCHAR(500)` | 否 | 核算范围值 | 部门或员工范围 |
| `batch_status` | `VARCHAR(32)` | 是 | 批次状态 | `DRAFT`、`CALCULATING`、`PENDING_CONFIRM`、`APPROVING`、`APPROVED`、`PAID`、`REJECTED` |
| `approval_instance_id` | `BIGINT UNSIGNED` | 否 | 审批实例ID | 关联 `hr_approval_instance.id` |
| `total_count` | `INT` | 是 | 核算员工数 | 汇总字段 |
| `total_gross_salary` | `DECIMAL(15,2)` | 是 | 应发总额 | 汇总字段 |
| `total_net_salary` | `DECIMAL(15,2)` | 是 | 实发总额 | 汇总字段 |
| `yellow_warning_count` | `INT` | 是 | 黄色预警数量 | 异常摘要 |
| `red_warning_count` | `INT` | 是 | 红色预警数量 | 异常摘要 |
| `block_count` | `INT` | 是 | 阻断异常数量 | 阻断时不可提交审批 |

建议唯一索引：`uk_hr_salary_batch_no(batch_no)`、`uk_hr_salary_batch_month_scope(salary_month, scope_type, scope_value, is_deleted)`。

### 8-6 薪资明细表 hr_salary_batch_item

用于记录每位员工在指定批次下的薪资明细和考勤扣款结果。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_batch_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资批次ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '部门ID',
  `attendance_stat_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '考勤月度统计ID',
  `base_salary` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
  `allowance_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '津贴金额',
  `performance_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '绩效奖金',
  `overtime_pay` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '加班费',
  `late_deduction` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '迟到扣款',
  `leave_deduction` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '请假扣款',
  `social_insurance_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '社保个人扣除',
  `housing_fund_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '公积金个人扣除',
  `income_tax_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '个人所得税',
  `gross_salary` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应发工资',
  `deduction_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应扣合计',
  `net_salary` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实发工资',
  `manual_adjust_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '手动调整金额',
  `manual_adjust_reason` VARCHAR(500) DEFAULT NULL COMMENT '手动调整原因',
  `exception_level` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '异常级别',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_salary_batch_item_emp` (`batch_id`, `employee_id`, `is_deleted`),
  KEY `idx_hr_salary_item_employee` (`employee_id`),
  KEY `idx_hr_salary_item_dept` (`dept_id`),
  KEY `idx_hr_salary_item_att_stat` (`attendance_stat_id`),
  CONSTRAINT `fk_hr_salary_item_batch` FOREIGN KEY (`batch_id`) REFERENCES `hr_salary_batch` (`id`),
  CONSTRAINT `fk_hr_salary_item_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_salary_item_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_hr_salary_item_att_stat` FOREIGN KEY (`attendance_stat_id`) REFERENCES `hr_attendance_month_stat` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资明细表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `batch_id` | `BIGINT UNSIGNED` | 是 | 薪资批次ID | 关联 `hr_salary_batch.id` |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `dept_id` | `BIGINT UNSIGNED` | 是 | 部门ID | 关联 `sys_dept.id`，保留核算快照 |
| `attendance_stat_id` | `BIGINT UNSIGNED` | 否 | 考勤月度统计ID | 关联 `hr_attendance_month_stat.id` |
| `base_salary` | `DECIMAL(12,2)` | 是 | 基本工资 | 收入项 |
| `allowance_amount` | `DECIMAL(12,2)` | 是 | 津贴金额 | 收入项 |
| `performance_amount` | `DECIMAL(12,2)` | 是 | 绩效奖金 | 收入项 |
| `overtime_pay` | `DECIMAL(12,2)` | 是 | 加班费 | 由考勤加班时长计算 |
| `late_deduction` | `DECIMAL(12,2)` | 是 | 迟到扣款 | 由考勤迟到次数计算 |
| `leave_deduction` | `DECIMAL(12,2)` | 是 | 请假扣款 | 由请假天数计算 |
| `social_insurance_amount` | `DECIMAL(12,2)` | 是 | 社保个人扣除 | 扣除项 |
| `housing_fund_amount` | `DECIMAL(12,2)` | 是 | 公积金个人扣除 | 扣除项 |
| `income_tax_amount` | `DECIMAL(12,2)` | 是 | 个人所得税 | 扣除项 |
| `gross_salary` | `DECIMAL(12,2)` | 是 | 应发工资 | 收入合计 |
| `deduction_amount` | `DECIMAL(12,2)` | 是 | 应扣合计 | 扣除合计 |
| `net_salary` | `DECIMAL(12,2)` | 是 | 实发工资 | 应发 - 应扣 |
| `manual_adjust_amount` | `DECIMAL(12,2)` | 是 | 手动调整金额 | 需要记录原因 |
| `manual_adjust_reason` | `VARCHAR(500)` | 否 | 手动调整原因 | 调整时必填 |
| `exception_level` | `VARCHAR(32)` | 是 | 异常级别 | `NORMAL`、`YELLOW`、`RED`、`BLOCK` |

建议唯一索引：`uk_hr_salary_batch_item_emp(batch_id, employee_id, is_deleted)`。

### 8-7 薪资异常检测表 hr_salary_exception

用于记录薪资核算过程中识别出的黄标预警、红色预警和阻断异常。

```sql
CREATE TABLE IF NOT EXISTS `hr_salary_exception` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资批次ID',
  `batch_item_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '薪资明细ID',
  `employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '员工ID',
  `exception_code` VARCHAR(64) NOT NULL COMMENT '异常编码',
  `exception_level` VARCHAR(32) NOT NULL COMMENT '异常级别',
  `exception_message` VARCHAR(500) NOT NULL COMMENT '异常说明',
  `resolved_status` VARCHAR(32) NOT NULL DEFAULT 'UNRESOLVED' COMMENT '处理状态',
  `resolved_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人用户ID',
  `resolved_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_salary_exception_batch` (`batch_id`),
  KEY `idx_hr_salary_exception_item` (`batch_item_id`),
  KEY `idx_hr_salary_exception_emp` (`employee_id`),
  KEY `idx_hr_salary_exception_resolved` (`resolved_by`),
  CONSTRAINT `fk_hr_salary_exception_batch` FOREIGN KEY (`batch_id`) REFERENCES `hr_salary_batch` (`id`),
  CONSTRAINT `fk_hr_salary_exception_item` FOREIGN KEY (`batch_item_id`) REFERENCES `hr_salary_batch_item` (`id`),
  CONSTRAINT `fk_hr_salary_exception_emp` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`),
  CONSTRAINT `fk_hr_salary_exception_resolved` FOREIGN KEY (`resolved_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资异常检测表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `batch_id` | `BIGINT UNSIGNED` | 是 | 薪资批次ID | 关联 `hr_salary_batch.id` |
| `batch_item_id` | `BIGINT UNSIGNED` | 否 | 薪资明细ID | 关联 `hr_salary_batch_item.id` |
| `employee_id` | `BIGINT UNSIGNED` | 否 | 员工ID | 关联 `hr_employee.id` |
| `exception_code` | `VARCHAR(64)` | 是 | 异常编码 | 如 `LEAVE_OVER_15` |
| `exception_level` | `VARCHAR(32)` | 是 | 异常级别 | `YELLOW`、`RED`、`BLOCK` |
| `exception_message` | `VARCHAR(500)` | 是 | 异常说明 | 前端展示文案 |
| `resolved_status` | `VARCHAR(32)` | 是 | 处理状态 | `UNRESOLVED`、`CONFIRMED`、`IGNORED`、`FIXED` |
| `resolved_by` | `BIGINT UNSIGNED` | 否 | 处理人用户ID | 关联 `sys_user.id` |
| `resolved_time` | `DATETIME` | 否 | 处理时间 | 审计字段 |

建议索引：`idx_hr_salary_exception_batch(batch_id)`、`idx_hr_salary_exception_level(exception_level)`。

### 8-8 工资条表 hr_payslip

用于存储审批通过后员工可查看的工资条摘要。

```sql
CREATE TABLE IF NOT EXISTS `hr_payslip` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资批次ID',
  `batch_item_id` BIGINT UNSIGNED NOT NULL COMMENT '薪资明细ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `salary_month` CHAR(7) NOT NULL COMMENT '薪资月份，格式yyyy-MM',
  `gross_salary` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应发工资',
  `deduction_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应扣合计',
  `net_salary` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实发工资',
  `visible_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否可见：1可见，0不可见',
  `first_view_time` DATETIME DEFAULT NULL COMMENT '首次查看时间',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_payslip_item` (`batch_item_id`),
  UNIQUE KEY `uk_hr_payslip_emp_month` (`employee_id`, `salary_month`, `is_deleted`),
  KEY `idx_hr_payslip_batch` (`batch_id`),
  CONSTRAINT `fk_hr_payslip_batch` FOREIGN KEY (`batch_id`) REFERENCES `hr_salary_batch` (`id`),
  CONSTRAINT `fk_hr_payslip_item` FOREIGN KEY (`batch_item_id`) REFERENCES `hr_salary_batch_item` (`id`),
  CONSTRAINT `fk_hr_payslip_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资条表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `batch_id` | `BIGINT UNSIGNED` | 是 | 薪资批次ID | 关联 `hr_salary_batch.id` |
| `batch_item_id` | `BIGINT UNSIGNED` | 是 | 薪资明细ID | 关联 `hr_salary_batch_item.id` |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `salary_month` | `CHAR(7)` | 是 | 薪资月份 | 格式 `yyyy-MM` |
| `gross_salary` | `DECIMAL(12,2)` | 是 | 应发工资 | 工资条摘要 |
| `deduction_amount` | `DECIMAL(12,2)` | 是 | 应扣合计 | 工资条摘要 |
| `net_salary` | `DECIMAL(12,2)` | 是 | 实发工资 | 工资条摘要 |
| `visible_status` | `TINYINT` | 是 | 是否可见 | 审批通过后为 `1` |
| `first_view_time` | `DATETIME` | 否 | 首次查看时间 | 首次查看后回填 |

建议唯一索引：`uk_hr_payslip_emp_month(employee_id, salary_month, is_deleted)`。

### 8-9 工资条查看日志表 hr_payslip_view_log

用于记录工资条二次验证、首次查看与后续查看审计。

```sql
CREATE TABLE IF NOT EXISTS `hr_payslip_view_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payslip_id` BIGINT UNSIGNED NOT NULL COMMENT '工资条ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `verify_type` VARCHAR(32) NOT NULL COMMENT '二次验证方式：SMS/PASSWORD',
  `verify_status` TINYINT NOT NULL DEFAULT 1 COMMENT '验证状态：1成功，0失败',
  `view_ip` VARCHAR(64) DEFAULT NULL COMMENT '查看IP',
  `view_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_payslip_log_payslip` (`payslip_id`),
  KEY `idx_hr_payslip_log_employee` (`employee_id`, `view_time`),
  CONSTRAINT `fk_hr_payslip_log_payslip` FOREIGN KEY (`payslip_id`) REFERENCES `hr_payslip` (`id`),
  CONSTRAINT `fk_hr_payslip_log_employee` FOREIGN KEY (`employee_id`) REFERENCES `hr_employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资条查看日志表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 主键ID | 自增主键 |
| `payslip_id` | `BIGINT UNSIGNED` | 是 | 工资条ID | 关联 `hr_payslip.id` |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `verify_type` | `VARCHAR(32)` | 是 | 二次验证方式 | `SMS`、`PASSWORD` |
| `verify_status` | `TINYINT` | 是 | 验证状态 | `1` 成功，`0` 失败 |
| `view_ip` | `VARCHAR(64)` | 否 | 查看 IP | 安全审计 |
| `view_time` | `DATETIME` | 是 | 查看时间 | 默认当前时间 |

建议索引：`idx_hr_payslip_view_log_payslip(payslip_id)`、`idx_hr_payslip_view_log_employee(employee_id, view_time)`。

### 8-10 主要表关系

| 业务对象 | 表名 | 关联主数据/上游表 | 说明 |
| --- | --- | --- | --- |
| 薪资账套 | `hr_salary_template` | `sys_dept` / `sys_post` / `hr_employee.job_level` | 薪资计算模板 |
| 工资项目 | `hr_salary_template_item` | `hr_salary_template` | 定义薪资构成和计算规则 |
| 员工薪资档案 | `hr_employee_salary_profile` | `hr_employee`、`hr_salary_template` | 员工个性化薪资配置 |
| 调薪历史 | `hr_salary_adjustment_record` | `hr_employee`、`sys_user` | 涉薪变更审计 |
| 薪资批次 | `hr_salary_batch` | `hr_approval_instance` | 月度核算主单据 |
| 薪资明细 | `hr_salary_batch_item` | `hr_salary_batch`、`hr_employee`、`hr_attendance_month_stat` | 员工级核算结果 |
| 薪资异常 | `hr_salary_exception` | `hr_salary_batch_item` | 异常检测与处理记录 |
| 工资条 | `hr_payslip` | `hr_salary_batch_item` | 员工可见工资条 |
| 查看日志 | `hr_payslip_view_log` | `hr_payslip`、`hr_employee` | 二次验证和查看审计 |

## 9API 设计

### 9-0 API 统一约定

*   返回体统一使用 `Result<T>`。
*   分页接口统一使用 `pageNum`、`pageSize`、`total`、`records`。
*   金额字段使用 `BigDecimal`，数据库使用 `DECIMAL`，不得使用浮点类型。
*   涉薪接口按角色控制字段可见性；普通员工只能查看本人已可见工资条。
*   薪资批次提交审批统一调用 `/approval/start`，审批通过后工资条才可见。

### 9-1 薪资账套接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 查询薪资账套分页 | `/api/v1/salary/templates` | `GET` | 按名称、状态、适用范围查询 |
| 获取薪资账套详情 | `/api/v1/salary/templates/{id}` | `GET` | 返回账套和工资项目 |
| 创建薪资账套 | `/api/v1/salary/templates` | `POST` | 新增薪资账套 |
| 更新薪资账套 | `/api/v1/salary/templates/{id}` | `PUT` | 更新账套基础信息 |
| 启停薪资账套 | `/api/v1/salary/templates/{id}/status` | `PATCH` | 修改 `status` |
| 删除薪资账套 | `/api/v1/salary/templates/{id}` | `DELETE` | 逻辑删除 |
| 保存工资项目 | `/api/v1/salary/templates/{id}/items` | `PUT` | 批量保存工资项目 |

创建薪资账套请求核心字段：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `templateName` | String | 是 | 账套名称 |
| `scopeType` | String | 是 | `DEPT` / `POST` / `JOB_LEVEL` / `EMPLOYEE` |
| `scopeValue` | String | 是 | 范围值，多个用逗号分隔 |
| `effectiveDate` | Date | 是 | 生效日期 |
| `items` | Array | 是 | 工资项目列表 |

### 9-2 员工薪资档案与调薪接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 查询员工薪资档案 | `/api/v1/salary/employees/{employeeId}/profile` | `GET` | HR/财务按权限查询 |
| 设置员工薪资档案 | `/api/v1/salary/employees/{employeeId}/profile` | `PUT` | 新增或更新薪资档案 |
| 查询调薪历史 | `/api/v1/salary/employees/{employeeId}/adjustments` | `GET` | 查询调薪审计记录 |
| 查询未设置薪资档案员工 | `/api/v1/salary/employees/missing-profile` | `GET` | 薪资核算前阻断检查 |

设置员工薪资档案请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `templateId` | Long | 是 | 薪资账套 ID |
| `baseSalary` | BigDecimal | 是 | 基本工资 |
| `allowanceBase` | BigDecimal | 否 | 津贴基数 |
| `socialInsuranceBase` | BigDecimal | 是 | 社保基数 |
| `housingFundBase` | BigDecimal | 是 | 公积金基数 |
| `performanceBase` | BigDecimal | 否 | 绩效基数 |
| `probationSalaryRatio` | BigDecimal | 否 | 试用期薪资比例，如 `80.00` |
| `effectiveDate` | Date | 是 | 生效日期 |
| `adjustReason` | String | 条件必填 | 涉及调薪时必填 |

### 9-3 薪资批次与核算接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 创建薪资批次 | `/api/v1/salary/batches` | `POST` | 选择月份和范围创建批次 |
| 查询薪资批次分页 | `/api/v1/salary/batches` | `GET` | 按月份、状态查询 |
| 获取薪资批次详情 | `/api/v1/salary/batches/{id}` | `GET` | 返回批次摘要和异常汇总 |
| 触发薪资核算 | `/api/v1/salary/batches/{id}/calculate` | `POST` | 读取薪资档案、考勤统计并计算 |
| 查询薪资预览 | `/api/v1/salary/batches/{id}/preview` | `GET` | 返回员工级薪资明细和异常标记 |
| 手动调整薪资明细 | `/api/v1/salary/batches/{id}/items/{itemId}/adjust` | `PATCH` | 调整金额并重新汇总 |
| 提交财务审批 | `/api/v1/salary/batches/{id}/submit` | `POST` | 调用审批中心发起审批 |
| 确认发放 | `/api/v1/salary/batches/{id}/pay` | `PATCH` | 实际发放后标记已发放 |
| 查询薪资异常 | `/api/v1/salary/batches/{id}/exceptions` | `GET` | 查询异常列表 |
| 处理薪资异常 | `/api/v1/salary/exceptions/{id}/resolve` | `PATCH` | 确认、忽略或修复异常 |

创建薪资批次请求参数：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `salaryMonth` | String | 是 | 薪资月份，如 `2024-07` |
| `scopeType` | String | 是 | `ALL` / `DEPT` / `EMPLOYEE` |
| `scopeValue` | String | 否 | 范围值 |

触发薪资核算响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "batchId": 2024,
    "batchNo": "SAL-202407-001",
    "status": "PENDING_CONFIRM",
    "totalCount": 156,
    "totalGrossSalary": 2856421.50,
    "totalNetSalary": 2218436.78,
    "exceptionSummary": {
      "yellowWarningCount": 8,
      "redWarningCount": 2,
      "blockCount": 1
    }
  }
}
```

异常检测规则：

| 规则 | 异常级别 | 处理要求 |
| --- | --- | --- |
| 当月请假天数 > 15 天 | `YELLOW` | HR 确认后可继续 |
| 当月加班时长 > 50 小时 | `YELLOW` | HR 确认后可继续 |
| 薪资较上月变动 > 30% | `RED` | 需确认原因 |
| 新入职员工未设置薪资档案 | `BLOCK` | 阻断核算，补齐档案后重算 |

### 9-4 薪资可视化与工资条接口

| 接口名 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 查询薪资可视化数据 | `/api/v1/salary/batches/{id}/visualization` | `GET` | 返回 AntV 数据集 |
| 二次验证工资条 | `/api/v1/salary/payslips/{month}/verify` | `POST` | 短信验证码或密码验证 |
| 查看工资条 | `/api/v1/salary/payslips/{month}` | `GET` | 员工仅可查看本人已可见工资条 |
| 查询工资条查看日志 | `/api/v1/salary/payslips/{id}/view-logs` | `GET` | HR/审计按权限查询 |

工资条二次验证请求头和参数：

| 名称 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `Authorization` | Header | 是 | 登录 Token |
| `verifyType` | String | 是 | `SMS` / `PASSWORD` |
| `verifyCode` | String | 是 | 验证码或密码校验凭据 |

工资条响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "salaryMonth": "2024-07",
    "employeeName": "张三",
    "employeeNo": "202401005",
    "departmentName": "技术部",
    "incomeItems": [
      { "name": "基本工资", "amount": 10000.00 },
      { "name": "岗位津贴", "amount": 2000.00 },
      { "name": "绩效奖金", "amount": 3600.00 }
    ],
    "deductionItems": [
      { "name": "事假扣款", "amount": 500.00 },
      { "name": "养老保险", "amount": 640.00 },
      { "name": "个人所得税", "amount": 320.00 }
    ],
    "grossSalary": 15600.00,
    "deductionAmount": 2620.00,
    "netSalary": 12980.00
  }
}
```

### 9-5 跨模块接口依赖

| 依赖场景 | 调用接口 | 方法 | 最小字段 |
| --- | --- | --- | --- |
| 获取员工简要信息 | `/employees/brief/{id}` | `GET` | `id, name, employeeNo, departmentId, departmentName, status` |
| 获取组织范围 | `/departments/tree` | `GET` | `id, name, parentId, children` |
| 发起薪资批次审批 | `/approval/start` | `POST` | 入参 `bizType, bizId, applicantId`，返回 `taskId` |
| 读取考勤月度汇总 | `/api/v1/attendance/stats/monthly/payroll-source` | `GET` | `employeeId, statMonth, lateCount, leaveDays, overtimeHours` |

## 10关键技术设计

### 10-1 薪资计算引擎方案

采用规则引擎 + 公式表达式分离设计：

*   固定收入项：直接取 `hr_employee_salary_profile` 表对应字段值
*   变动收入项：使用 Aviator 表达式引擎，公式示例：`performanceBase * performanceCoeff`、`hourlyRate * multiplier * overtimeHours`
*   考勤扣款项：从 `hr_attendance_month_stat` 拉取数据按规则计算：`50 * lateCount`、`dailyWage * leaveDays`
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
