# HRMS 统一全局开发规范

## 1. 文档目标

本文档用于统一 HRMS 人资管理系统在产品设计、技术实现、数据建模、接口契约、权限控制、开发协作与数据库建设方面的全局规范，作为项目长期演进的基础约束文档。

本文档主要依据以下资料进行整合：

- 00-全局技术底座与核心契约.md
- HRMS\README.md
- HRMS\backend\协同开发文档.md
- 人资管理系统（HRMS）详细产品规格说明书.pdf

其中，业务规则、角色权限、流程状态、主数据口径以《人资管理系统（HRMS）详细产品规格说明书》为优先依据；技术结构与工程协作以现有项目代码结构为落地基准。

## 2. 适用范围

- 适用于 `HRMS` 整个仓库
- 适用于前端、后端、数据库、接口联调、测试验收与后续迭代
- 适用于系统管理、组织架构、员工档案、入转调离、审批、考勤、薪资、个人中心等业务模块

## 3. 项目总体架构与技术底座

### 3.1 总体架构

项目采用前后端分离架构：

- 前端：React 18 + TypeScript 5
- 前端工程化：Node `24.18.0` + pnpm `11.10.0`
- 后端：Spring Boot `3.5.16`
- Java 版本：Java `17`
- 构建方式：Maven 多模块
- 数据库建议：MySQL `8.0+`

### 3.2 当前项目目录基线

```text
HRMS
├─ README.md
├─ backend
│  ├─ hrms-common
│  ├─ hrms-system
│  ├─ hrms-business
│  ├─ hrms-server
│  └─ 协同开发文档.md
└─ frontend
   └─ hrms-frontend-umi
```

### 3.3 后端模块职责

#### `hrms-common`

公共基础模块，负责沉淀全项目复用能力：

- 通用返回体
- 枚举与错误码
- 全局异常处理
- 工具类与基础常量
- 通用配置基座
- 脱敏、加密、审计、权限拦截等公共能力

#### `hrms-system`

系统管理域，负责系统级基础能力：

- 用户与账号
- 角色与菜单权限
- 部门与岗位基础配置
- 字典与参数配置
- 登录日志、操作日志、附件管理

#### `hrms-business`

业务域，负责人资核心业务：

- 组织架构管理
- 员工档案管理
- 入职管理
- 转正管理
- 调岗管理
- 离职管理
- 审批中心
- 考勤管理
- 薪资管理
- 个人中心

#### `hrms-server`

启动与装配模块，仅负责：

- Spring Boot 应用启动
- 配置装配
- 模块扫描与依赖聚合

约束：`hrms-server` 不承载具体业务逻辑、Mapper、Service 实现和领域实体。

## 4. 分层设计与包结构规范

### 4.1 推荐分层

后端业务实现统一按照以下分层组织：

- `controller`：接口控制层
- `service`：业务服务接口
- `service.impl`：业务服务实现
- `mapper`：数据访问层
- `entity`：数据库实体
- `dto`：请求或过程数据传输对象
- `vo`：返回视图对象
- `convert`：对象转换器
- `enums`：模块内业务枚举
- `domain`：领域对象与聚合根
- `config`：模块配置
- `exception`：模块异常
- `util`：模块工具类

### 4.2 包名规范

统一包名前缀如下：

- `com.hrms.common`
- `com.hrms.system`
- `com.hrms.business`
- `com.hrms.server`

### 4.3 类命名规范

- 实体类：`XxxDO` 或 `XxxEntity`，项目内统一后不得混用
- DTO：`XxxRequestDTO`、`XxxQueryDTO`、`XxxCommandDTO`
- VO：`XxxVO`、`XxxPageVO`
- Mapper：`XxxMapper`
- Service：`XxxService`
- Service 实现：`XxxServiceImpl`
- Controller：`XxxController`
- 转换器：`XxxConvert`
- 枚举：`XxxEnum`

建议优先采用更易识别的命名方式：

- 数据库实体：`XxxDO`
- 接口入参：`XxxRequestDTO`、`XxxQueryDTO`
- 接口出参：`XxxVO`

## 5. 统一开发原则

### 5.1 单一职责原则

- `system` 模块只处理系统底座与主数据配置
- `business` 模块只处理人资业务规则
- 公共能力必须沉淀到 `common`，避免重复实现

### 5.2 先建契约后写实现

所有模块开发顺序建议为：

1. 明确业务规则与状态流转
2. 确定数据库模型
3. 定义接口契约
4. 编写 DTO、VO、枚举、错误码
5. 编写 Service 与业务实现
6. 编写 Controller
7. 补充日志、权限、校验、测试

### 5.3 审计优先

所有核心业务表、审批表、日志表默认具备审计字段、逻辑删除字段与版本号字段，确保后续追踪、审计与并发控制可扩展。

## 6. 统一接口契约

### 6.1 返回体规范

项目统一使用 `com.hrms.common.model.Result<T>` 作为返回体，统一字段如下：

- `code`：业务响应码
- `message`：响应消息
- `data`：业务数据

统一要求：

- Controller 不直接返回裸对象
- 成功统一返回 `Result.success()` 或 `Result.success(data)`
- 失败统一抛出业务异常，由全局异常处理器转换为标准结构

### 6.2 HTTP 语义规范

- `GET`：查询
- `POST`：新增、流程发起、复杂条件查询
- `PUT`：全量更新
- `PATCH`：部分更新、状态变更
- `DELETE`：逻辑删除

### 6.3 分页接口规范

分页查询至少包含：

- `pageNum`
- `pageSize`
- `total`
- `records`

如后续引入统一分页对象，应放入 `hrms-common` 统一管理。

### 6.4 查询条件规范

列表查询统一支持以下设计原则：

- 精确条件与模糊条件分离
- 时间查询统一使用开始时间与结束时间
- 多选项统一使用数组
- 状态、类型、级别等统一使用枚举编码或字典编码

### 6.5 跨模块接口规范参考

以下表格作为 HRMS 跨模块调用时的最小接口契约参考。具体业务字段可以按模块场景扩展，但最小必填入参与最小必有返回字段不得缺失，以保证模块间联调、替换与复用的一致性。

| 序号 | 接口名 | 路径 | 请求方式 | 最小必填入参 | 最小必有返回字段 | 提供方(模块) | 调用方(模块) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 获取员工简要信息 | `/employees/brief/{id}` | `GET` | `id (Path)` | `id, name, employeeNo, departmentId, departmentName, status` | `4(档案)` | `6,7,8,9` |
| 2 | 生成工号 | `/employees/gen-no` | `POST` | `departmentId` | `employeeNo` | `4(档案)` | `5(入职)` |
| 3 | 获取部门树 | `/departments/tree` | `GET` | 无（基于 Token 自动过滤） | `[{id, name, parentId, children}]` | `3(组织)` | `4,5,6,7,8` |
| 4 | 发起审批任务 | `/approval/start` | `POST` | `bizType, bizId, applicantId` | `taskId` | `8(审批)` | `5,6` |

接口协同时还应补充以下约束：

- 请求头统一携带认证 Token、租户标识、追踪 ID 等公共上下文
- 对外暴露的跨模块接口应明确幂等要求、超时策略与错误码约定
- 返回字段命名统一使用驼峰风格，布尔字段建议采用 `isXxx` 或语义明确的状态字段
- 重要主数据接口应保证版本兼容，新增字段只能向后兼容扩展，不能随意删除既有字段

### 6.6 统一响应 JSON 示例

以下示例用于补充统一返回体规范，便于前后端联调、接口文档编写与测试数据构造。

获取员工简要信息响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "name": "张三",
    "employeeNo": "202401005",
    "departmentId": 10,
    "departmentName": "技术部",
    "status": 2
  }
}
```

发起审批任务响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": 30001
  }
}
```

获取部门树响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 10,
      "name": "总部",
      "parentId": 0,
      "children": [
        {
          "id": 11,
          "name": "技术部",
          "parentId": 10,
          "children": []
        }
      ]
    }
  ]
}
```

## 7. 核心主数据模型规范

### 7.1 全局核心主数据

HRMS 全局核心主数据包括：

- 员工
- 部门
- 职位
- 系统用户
- 角色
- 菜单
- 数据字典

其中：

- 员工是业务主实体
- 系统用户是登录与认证主实体
- 员工与系统用户建议一对一关联
- 部门与职位共同决定员工组织归属与业务流向

### 7.2 员工主数据口径

员工档案以规格说明书为准，至少覆盖以下信息域：

#### 基础身份信息

- 员工姓名
- 性别
- 手机号
- 邮箱
- 身份证号
- 生日
- 户籍地址
- 现居住地址

#### 工作信息

- 工号
- 所属部门
- 职位
- 职级
- 直接汇报人
- 工作地点
- 入职类型
- 入职日期
- 在职状态

#### 合同与薪资信息

- 合同类型
- 合同到期日
- 试用期月数
- 试用期待遇比例
- 薪资账套
- 基本工资
- 银行账号
- 开户行

### 7.3 员工工号生成规则

根据规格说明书，工号规则统一为：

`4位年份 + 2位部门编码 + 3位流水序号`

示例：

`202401005`

约束：

- 年份取员工正式生成工号当年的四位值
- 部门编码必须可配置且唯一
- 同部门同年流水号不可重复

### 7.4 部门主数据规范

部门模型至少包含：

- 部门名称
- 部门编码
- 上级部门
- 部门负责人
- 排序序号
- 部门描述

业务约束：

- 最大层级深度为 5 级
- 子部门数量不限
- 部门合并前必须先完成员工迁移
- 需支持统计本部门及下属部门在职员工数

员工统计口径统一为：

- 在职状态为“试用期”或“正式”的员工

### 7.5 职位主数据规范

职位模型至少包含：

- 职位名称
- 职位序列
- 所属部门
- 职级范围
- 默认试用期
- 职位描述

业务约束：

- 所属部门可为空，为空表示公司通用职位
- 职位序列统一支持 `M`、`P`、`S`
- 职级范围建议按字典维护

建议职级范围：

- `M1` - `M5`
- `P1` - `P10`
- `S1` - `S5`

## 8. 权限与安全规范

### 8.1 角色划分

系统默认角色至少包括：

- 系统管理员
- HR 专员
- 部门主管
- 普通员工
- 财务专员

### 8.2 数据权限口径

依据规格说明书，数据权限范围建议统一为：

- 系统管理员：全平台数据
- HR 专员：全部员工数据
- 部门主管：本部门及下属部门员工数据
- 财务专员：薪资相关数据
- 普通员工：仅本人数据

### 8.3 字段级权限

员工档案中以下字段默认视为敏感字段：

- 身份证号
- 银行账号
- 基本工资
- 紧急联系人
- 开户行

字段可见性建议：

- HR 专员：可见敏感字段
- 财务专员：可见薪资与银行卡相关字段
- 部门主管：默认不可见身份证号、银行卡、薪资等敏感字段
- 普通员工：仅可查看本人敏感字段

### 8.4 敏感数据处理

以下数据建议加密存储或至少脱敏展示：

- 身份证号
- 银行账号
- 手机号
- 邮箱

展示规则建议：

- 身份证号：仅显示前 6 位与后 4 位
- 银行账号：仅显示后 4 位
- 手机号：中间 4 位脱敏

### 8.5 安全基线

- 密码必须加密存储，禁止明文存储
- 初始密码必须首次登录强制修改
- 关键操作必须记录操作日志
- 登录行为必须记录登录日志
- 审批动作必须保留审批轨迹

## 9. 核心业务状态与流程规范

### 9.1 员工在职状态

员工在职状态统一枚举建议：

- `PROBATION`：试用期
- `FORMAL`：正式
- `PENDING_LEAVE`：待离职
- `LEFT`：已离职

### 9.2 入职流程

入职流程状态统一建议：

- `DRAFT`：草稿
- `PENDING_APPROVAL`：审批中
- `APPROVED_PENDING_ENTRY`：已批准待入职
- `REJECTED`：已拒绝
- `ENTERED`：已入职

入职申请至少包含：

- 姓名
- 性别
- 手机号
- 邮箱
- 身份证号
- 预计入职日期
- 所属部门
- 职位
- 录用类型
- 试用期月数
- 试用期薪资比例
- 直接汇报人

入职审批规则：

- 一级审批：部门负责人
- 二级审批：HR 负责人
- 可支持开关控制二级审批是否启用
- 非标准职位或薪资超出职级范围时必须触发二审
- 每级审批时效为 48 小时

入职通过后的系统动作：

- 自动生成工号
- 自动创建系统账号
- 首次登录强制修改密码
- 发送欢迎通知
- 通知 HR 与部门负责人

### 9.3 转正流程

转正业务规则：

- 系统应支持每日扫描试用期员工
- 到期提醒规则：`入职日期 + 试用期月数 - 7 天`
- 转正结果：
  - 通过
  - 延长试用
  - 不通过

转正通过后：

- 员工状态更新为“正式”
- 可同步调整转正后薪资

### 9.4 调岗流程

调岗发起条件：

- 员工在职状态必须为“试用期”或“正式”

调岗可变更项：

- 所属部门
- 职位
- 职级
- 直接汇报人
- 薪资调整

核心约束：

- 所属部门必须发生变化
- 工号不变
- 调岗生效后同步更新员工主档
- 必须记录调岗历史

调岗审批流：

- 原部门负责人
- 新部门负责人
- HR 负责人备案

### 9.5 离职流程

离职发起规则：

- 仅 HR 可发起
- 员工当前状态必须为“试用期”或“正式”
- 离职日期不得早于当天

离职审批流：

- 部门负责人
- HR 负责人

离职通过后：

- 先变更为“待离职”
- 到达离职日期后变更为“已离职”
- 系统账号禁用
- 员工档案保留

## 10. 前后端协同契约

### 10.1 字典优先

以下字段优先采用字典配置，不在前端写死：

- 性别
- 在职状态
- 合同类型
- 入职类型
- 审批状态
- 职位序列
- 职级

### 10.2 枚举与显示文案分离

后端返回编码值，前端通过字典或枚举映射显示文案，避免页面直接依赖中文状态值。

### 10.3 时间与金额规范

- 时间统一使用 `yyyy-MM-dd HH:mm:ss`
- 日期字段统一使用 `yyyy-MM-dd`
- 金额统一使用十进制定点类型，不使用浮点类型存储

### 10.4 文件与附件规范

员工档案、合同、审批附件统一接入文件中心，文件元数据统一沉淀在公共文件表中，不允许业务表直接保存大量二进制内容。

## 11. 统一错误码规范

建议错误码分段如下：

- `0000`：成功
- `1000-1999`：通用参数与校验异常
- `2000-2999`：认证与授权异常
- `3000-3999`：系统管理模块异常
- `4000-4999`：员工档案与组织业务异常
- `5000-5999`：审批流程异常
- `6000-6999`：考勤异常
- `7000-7999`：薪资异常
- `9000-9999`：系统内部异常

示例：

- `4001`：员工不存在
- `4002`：工号重复
- `4003`：部门不存在
- `4004`：职位不存在
- `5001`：审批单不存在
- `5002`：当前节点无审批权限

## 12. 命名与字段规范

### 12.1 数据库命名规范

- 表名统一小写下划线风格
- 系统表以 `sys_` 开头
- 人资业务表以 `hr_` 开头
- 中间表使用 `relation` 或双实体组合命名

### 12.2 通用字段规范

所有核心业务表统一建议包含如下字段：

- `id`：主键
- `create_by`：创建人
- `create_time`：创建时间
- `update_by`：更新人
- `update_time`：更新时间
- `is_deleted`：逻辑删除标记
- `version`：乐观锁版本号
- `remark`：备注

### 12.3 状态字段规范

- 状态字段统一命名为 `status`
- 启停字段统一命名为 `status` 或 `enabled`，项目内需统一选型
- 审批状态字段统一命名为 `approval_status`

建议：

- 通用启停使用 `status`
- 业务流程单独使用 `approval_status`、`employment_status` 等具备明确语义的字段

## 13. 开发协作规范

### 13.1 模块边界

- `system` 不直接耦合业务流程细节
- `business` 可依赖 `system` 的主数据能力
- 所有模块均可依赖 `common`
- 禁止 `common` 反向依赖业务模块

### 13.2 代码提交规范

建议采用中文 Conventional Commits 风格，例如：

```text
feat(employee): 新增员工档案创建与详情查询能力

- 新增员工主档实体与查询接口
- 接入部门、职位与汇报人关联校验
- 补充员工工号生成规则与数据校验
```

文档类提交建议：

```text
docs(spec): 新增HRMS统一全局规范文档

- 融合全局技术底座、协同开发文档与产品规格书形成统一规范
- 明确模块职责、分层约定、接口与数据规范
- 补充基础公用SQL建表语法与公共审计字段约定
```

### 13.3 文档变更原则

- 业务规则变化优先更新本规范
- 字段口径变化必须同步更新 SQL 设计
- 流程变化必须同步更新状态枚举、审批规则与接口文档

## 14. 基础公用 SQL 建表语法

以下 SQL 作为 HRMS 项目基础公共表与核心主数据表的推荐建表基线，默认数据库为 MySQL 8.0，字符集为 `utf8mb4`。

### 14.1 建库建议

```sql
CREATE DATABASE IF NOT EXISTS `hrms`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

### 14.2 系统用户表

```sql
CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联员工ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `need_change_password` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否首次登录强制修改密码',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  KEY `idx_sys_user_employee_id` (`employee_id`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 系统用户主键 | 自增主键，供角色、日志、员工账号关联引用 |
| `username` | `VARCHAR(64)` | 是 | 登录账号 | 全局唯一，建议与员工账号策略保持一致 |
| `password` | `VARCHAR(255)` | 是 | 登录密码密文 | 必须加密存储，禁止明文保存 |
| `nickname` | `VARCHAR(64)` | 否 | 用户昵称 | 用于页面展示与轻量识别 |
| `real_name` | `VARCHAR(64)` | 否 | 用户真实姓名 | 建议与员工姓名保持同步 |
| `phone` | `VARCHAR(20)` | 是 | 手机号 | 全局唯一，可作为登录/找回密码凭证 |
| `email` | `VARCHAR(128)` | 否 | 邮箱 | 用于通知、找回密码与消息触达 |
| `avatar_url` | `VARCHAR(255)` | 否 | 头像地址 | 存储头像访问链接 |
| `employee_id` | `BIGINT UNSIGNED` | 否 | 关联员工主档ID | 用于账号与员工档案一一映射 |
| `status` | `TINYINT` | 是 | 账号启用状态 | `1` 启用，`0` 禁用 |
| `last_login_time` | `DATETIME` | 否 | 最后登录时间 | 便于安全审计与活跃度统计 |
| `last_login_ip` | `VARCHAR(64)` | 否 | 最后登录IP | 用于异常登录分析 |
| `need_change_password` | `TINYINT(1)` | 是 | 是否首次强制改密 | `1` 表示首次登录需修改密码 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段，记录创建操作人 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段，默认当前时间 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段，记录最后修改人 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段，更新时自动刷新 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发更新控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 补充账号说明信息 |

### 14.3 角色表

```sql
CREATE TABLE `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `data_scope` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_role_code` (`role_code`),
  KEY `idx_sys_role_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 角色主键 | 自增主键 |
| `role_name` | `VARCHAR(64)` | 是 | 角色名称 | 如系统管理员、HR 专员 |
| `role_code` | `VARCHAR(64)` | 是 | 角色编码 | 全局唯一，供权限判断与程序识别 |
| `data_scope` | `VARCHAR(32)` | 是 | 数据权限范围 | 如 `ALL`、`DEPT_AND_CHILD`、`SELF` |
| `status` | `TINYINT` | 是 | 角色状态 | `1` 启用，`0` 禁用 |
| `sort_no` | `INT` | 是 | 排序号 | 控制角色展示顺序 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 补充角色适用说明 |

### 14.4 用户角色关联表

```sql
CREATE TABLE `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 关联关系主键 | 自增主键 |
| `user_id` | `BIGINT UNSIGNED` | 是 | 用户ID | 关联 `sys_user.id` |
| `role_id` | `BIGINT UNSIGNED` | 是 | 角色ID | 关联 `sys_role.id` |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录特殊授权说明 |

### 14.5 菜单表

```sql
CREATE TABLE `sys_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级菜单ID',
  `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `menu_type` VARCHAR(16) NOT NULL COMMENT '类型：DIR/MENU/BUTTON',
  `path` VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `permission` VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 菜单主键 | 自增主键 |
| `parent_id` | `BIGINT UNSIGNED` | 是 | 父级菜单ID | `0` 表示顶级菜单 |
| `menu_name` | `VARCHAR(64)` | 是 | 菜单名称 | 用于前后端统一展示 |
| `menu_type` | `VARCHAR(16)` | 是 | 菜单类型 | `DIR`、`MENU`、`BUTTON` |
| `path` | `VARCHAR(255)` | 否 | 路由路径 | 目录或菜单型节点使用 |
| `component` | `VARCHAR(255)` | 否 | 前端组件路径 | 页面菜单时需配置 |
| `permission` | `VARCHAR(128)` | 否 | 权限标识 | 按钮与接口鉴权使用 |
| `icon` | `VARCHAR(64)` | 否 | 图标标识 | 用于前端侧边栏展示 |
| `sort_no` | `INT` | 是 | 排序号 | 控制同级菜单顺序 |
| `visible` | `TINYINT(1)` | 是 | 是否可见 | `1` 可见，`0` 隐藏 |
| `status` | `TINYINT` | 是 | 菜单状态 | `1` 启用，`0` 禁用 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录菜单补充说明 |

### 14.6 角色菜单关联表

```sql
CREATE TABLE `sys_role_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 关联关系主键 | 自增主键 |
| `role_id` | `BIGINT UNSIGNED` | 是 | 角色ID | 关联 `sys_role.id` |
| `menu_id` | `BIGINT UNSIGNED` | 是 | 菜单ID | 关联 `sys_menu.id` |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录角色菜单授权说明 |

### 14.7 部门表

```sql
CREATE TABLE `sys_dept` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级部门ID',
  `dept_name` VARCHAR(64) NOT NULL COMMENT '部门名称',
  `dept_code` VARCHAR(16) NOT NULL COMMENT '部门编码',
  `leader_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '部门负责人用户ID',
  `leader_employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '部门负责人员工ID',
  `ancestors` VARCHAR(500) DEFAULT NULL COMMENT '祖级路径',
  `dept_level` INT NOT NULL DEFAULT 1 COMMENT '部门层级',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '部门描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `employee_count` INT NOT NULL DEFAULT 0 COMMENT '在职员工数缓存',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dept_dept_code` (`dept_code`),
  KEY `idx_sys_dept_parent_id` (`parent_id`),
  KEY `idx_sys_dept_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 部门主键 | 自增主键 |
| `parent_id` | `BIGINT UNSIGNED` | 是 | 上级部门ID | `0` 表示顶级部门 |
| `dept_name` | `VARCHAR(64)` | 是 | 部门名称 | 用于组织架构展示 |
| `dept_code` | `VARCHAR(16)` | 是 | 部门编码 | 全局唯一，用于业务识别 |
| `leader_user_id` | `BIGINT UNSIGNED` | 否 | 部门负责人用户ID | 关联 `sys_user.id` |
| `leader_employee_id` | `BIGINT UNSIGNED` | 否 | 部门负责人员工ID | 关联 `hr_employee.id` |
| `ancestors` | `VARCHAR(500)` | 否 | 祖级路径 | 便于树形查询与层级回溯 |
| `dept_level` | `INT` | 是 | 部门层级 | 根节点通常为 `1` |
| `sort_no` | `INT` | 是 | 排序号 | 控制同级部门顺序 |
| `description` | `VARCHAR(500)` | 否 | 部门描述 | 记录部门职责说明 |
| `status` | `TINYINT` | 是 | 部门状态 | `1` 启用，`0` 禁用 |
| `employee_count` | `INT` | 是 | 在职员工数缓存 | 用于提升统计查询性能 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录部门补充说明 |

### 14.8 职位表

```sql
CREATE TABLE `sys_post` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_name` VARCHAR(64) NOT NULL COMMENT '职位名称',
  `post_code` VARCHAR(64) NOT NULL COMMENT '职位编码',
  `sequence_code` VARCHAR(16) NOT NULL COMMENT '职位序列：M/P/S',
  `dept_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属部门ID，为空表示通用职位',
  `job_level_min` VARCHAR(16) DEFAULT NULL COMMENT '职级下限',
  `job_level_max` VARCHAR(16) DEFAULT NULL COMMENT '职级上限',
  `default_probation_month` INT NOT NULL DEFAULT 3 COMMENT '默认试用期（月）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '职位描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_post_post_code` (`post_code`),
  KEY `idx_sys_post_dept_id` (`dept_id`),
  KEY `idx_sys_post_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 职位主键 | 自增主键 |
| `post_name` | `VARCHAR(64)` | 是 | 职位名称 | 用于岗位展示 |
| `post_code` | `VARCHAR(64)` | 是 | 职位编码 | 全局唯一 |
| `sequence_code` | `VARCHAR(16)` | 是 | 职位序列 | 如 `M`、`P`、`S` |
| `dept_id` | `BIGINT UNSIGNED` | 否 | 所属部门ID | 为空表示通用职位 |
| `job_level_min` | `VARCHAR(16)` | 否 | 职级下限 | 用于限制岗位适配职级 |
| `job_level_max` | `VARCHAR(16)` | 否 | 职级上限 | 与下限组合控制范围 |
| `default_probation_month` | `INT` | 是 | 默认试用期 | 单位为月 |
| `description` | `VARCHAR(500)` | 否 | 职位描述 | 记录岗位职责 |
| `status` | `TINYINT` | 是 | 职位状态 | `1` 启用，`0` 禁用 |
| `sort_no` | `INT` | 是 | 排序号 | 控制职位列表顺序 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录职位补充说明 |

### 14.9 字典类型表

```sql
CREATE TABLE `sys_dict_type` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name` VARCHAR(64) NOT NULL COMMENT '字典名称',
  `dict_type` VARCHAR(64) NOT NULL COMMENT '字典类型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type_type` (`dict_type`),
  KEY `idx_sys_dict_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 字典类型主键 | 自增主键 |
| `dict_name` | `VARCHAR(64)` | 是 | 字典名称 | 用于后台展示 |
| `dict_type` | `VARCHAR(64)` | 是 | 字典类型编码 | 全局唯一，供程序引用 |
| `status` | `TINYINT` | 是 | 字典状态 | `1` 启用，`0` 禁用 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录字典用途说明 |

### 14.10 字典数据表

```sql
CREATE TABLE `sys_dict_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` VARCHAR(64) NOT NULL COMMENT '字典类型',
  `label` VARCHAR(64) NOT NULL COMMENT '字典标签',
  `value` VARCHAR(64) NOT NULL COMMENT '字典值',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `tag_type` VARCHAR(32) DEFAULT NULL COMMENT '标签样式',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_data_type_value` (`dict_type`, `value`),
  KEY `idx_sys_dict_data_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 字典数据主键 | 自增主键 |
| `dict_type` | `VARCHAR(64)` | 是 | 字典类型编码 | 对应 `sys_dict_type.dict_type` |
| `label` | `VARCHAR(64)` | 是 | 字典标签 | 页面展示文本 |
| `value` | `VARCHAR(64)` | 是 | 字典值 | 同一类型下唯一 |
| `sort_no` | `INT` | 是 | 排序号 | 控制字典值顺序 |
| `tag_type` | `VARCHAR(32)` | 否 | 标签样式 | 前端状态色或样式标识 |
| `status` | `TINYINT` | 是 | 字典状态 | `1` 启用，`0` 禁用 |
| `is_default` | `TINYINT(1)` | 是 | 是否默认 | `1` 默认值，`0` 非默认 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录字典值补充说明 |

### 14.11 文件表

```sql
CREATE TABLE `sys_file` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '业务主键ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问地址',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `file_ext` VARCHAR(32) DEFAULT NULL COMMENT '文件后缀',
  `storage_type` VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 文件主键 | 自增主键 |
| `biz_type` | `VARCHAR(64)` | 否 | 业务类型 | 标识归属模块，如员工档案、合同等 |
| `biz_id` | `BIGINT UNSIGNED` | 否 | 业务主键ID | 与 `biz_type` 组合关联业务对象 |
| `file_name` | `VARCHAR(255)` | 是 | 原始文件名 | 保存上传时文件名 |
| `file_url` | `VARCHAR(500)` | 是 | 文件访问地址 | 可为本地路径或对象存储 URL |
| `file_size` | `BIGINT` | 是 | 文件大小 | 单位为字节 |
| `file_ext` | `VARCHAR(32)` | 否 | 文件后缀 | 如 `pdf`、`jpg` |
| `storage_type` | `VARCHAR(32)` | 是 | 存储类型 | 默认 `LOCAL`，可扩展 OSS/MinIO |
| `status` | `TINYINT` | 是 | 文件状态 | `1` 有效，`0` 禁用 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录文件补充说明 |

### 14.12 操作日志表

```sql
CREATE TABLE `sys_operate_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_name` VARCHAR(64) NOT NULL COMMENT '模块名称',
  `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
  `biz_id` VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
  `operate_type` VARCHAR(32) NOT NULL COMMENT '操作类型',
  `request_uri` VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
  `request_method` VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `request_param` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_data` TEXT DEFAULT NULL COMMENT '响应数据',
  `result_code` VARCHAR(32) DEFAULT NULL COMMENT '结果码',
  `result_message` VARCHAR(255) DEFAULT NULL COMMENT '结果信息',
  `success_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_operate_log_module` (`module_name`),
  KEY `idx_sys_operate_log_operator` (`operator_user_id`),
  KEY `idx_sys_operate_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 日志主键 | 自增主键 |
| `module_name` | `VARCHAR(64)` | 是 | 模块名称 | 标识操作所属业务模块 |
| `biz_type` | `VARCHAR(64)` | 否 | 业务类型 | 细分业务类别 |
| `biz_id` | `VARCHAR(64)` | 否 | 业务ID | 记录被操作的数据主键或单号 |
| `operate_type` | `VARCHAR(32)` | 是 | 操作类型 | 如新增、修改、删除、审批 |
| `request_uri` | `VARCHAR(255)` | 否 | 请求 URI | 记录访问接口路径 |
| `request_method` | `VARCHAR(16)` | 否 | 请求方法 | 如 `GET`、`POST` |
| `operator_user_id` | `BIGINT UNSIGNED` | 否 | 操作人用户ID | 关联 `sys_user.id` |
| `operator_name` | `VARCHAR(64)` | 否 | 操作人姓名 | 冗余记录，便于追溯 |
| `request_ip` | `VARCHAR(64)` | 否 | 请求 IP | 支持安全审计 |
| `request_param` | `TEXT` | 否 | 请求参数 | 建议脱敏后落库 |
| `response_data` | `TEXT` | 否 | 响应数据 | 建议控制长度并做敏感信息过滤 |
| `result_code` | `VARCHAR(32)` | 否 | 结果码 | 记录接口响应编码 |
| `result_message` | `VARCHAR(255)` | 否 | 结果信息 | 记录执行结果说明 |
| `success_flag` | `TINYINT(1)` | 是 | 是否成功 | `1` 成功，`0` 失败 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段，同时为主查询维度 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录异常补充说明 |

### 14.13 登录日志表

```sql
CREATE TABLE `sys_login_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
  `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  `login_location` VARCHAR(128) DEFAULT NULL COMMENT '登录地点',
  `browser` VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
  `login_status` TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '提示消息',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_login_log_username` (`username`),
  KEY `idx_sys_login_log_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 登录日志主键 | 自增主键 |
| `username` | `VARCHAR(64)` | 是 | 登录账号 | 记录尝试登录的账号 |
| `user_id` | `BIGINT UNSIGNED` | 否 | 用户ID | 登录成功时关联 `sys_user.id` |
| `login_ip` | `VARCHAR(64)` | 否 | 登录 IP | 支持安全审计与异地分析 |
| `login_location` | `VARCHAR(128)` | 否 | 登录地点 | 根据 IP 解析得到 |
| `browser` | `VARCHAR(128)` | 否 | 浏览器信息 | 终端环境识别 |
| `os` | `VARCHAR(128)` | 否 | 操作系统信息 | 终端环境识别 |
| `login_status` | `TINYINT` | 是 | 登录状态 | `1` 成功，`0` 失败 |
| `message` | `VARCHAR(255)` | 否 | 提示消息 | 记录失败原因或提示 |
| `login_time` | `DATETIME` | 是 | 登录时间 | 作为登录轨迹时间轴 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录登录事件补充说明 |

### 14.14 员工主档表

```sql
CREATE TABLE `hr_employee` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_no` VARCHAR(32) NOT NULL COMMENT '工号',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联系统用户ID',
  `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '职位ID',
  `leader_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '直接汇报人员工ID',
  `employee_name` VARCHAR(64) NOT NULL COMMENT '员工姓名',
  `gender` VARCHAR(16) DEFAULT NULL COMMENT '性别',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `id_card_no` VARCHAR(128) DEFAULT NULL COMMENT '身份证号（建议加密存储）',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `domicile_address` VARCHAR(255) DEFAULT NULL COMMENT '户籍地址',
  `current_address` VARCHAR(255) DEFAULT NULL COMMENT '现居住地址',
  `job_level` VARCHAR(16) DEFAULT NULL COMMENT '职级',
  `work_location` VARCHAR(128) DEFAULT NULL COMMENT '工作地点',
  `hire_type` VARCHAR(32) DEFAULT NULL COMMENT '入职类型',
  `employment_status` VARCHAR(32) NOT NULL COMMENT '在职状态',
  `hire_date` DATE NOT NULL COMMENT '入职日期',
  `probation_month` INT DEFAULT 3 COMMENT '试用期（月）',
  `probation_salary_ratio` DECIMAL(5,2) DEFAULT 100.00 COMMENT '试用期薪资比例',
  `contract_type` VARCHAR(32) DEFAULT NULL COMMENT '合同类型',
  `contract_expire_date` DATE DEFAULT NULL COMMENT '合同到期日',
  `salary_template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '薪资账套ID',
  `base_salary` DECIMAL(12,2) DEFAULT NULL COMMENT '基本工资',
  `bank_account` VARCHAR(128) DEFAULT NULL COMMENT '银行账号（建议加密存储）',
  `bank_name` VARCHAR(128) DEFAULT NULL COMMENT '开户行',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '记录状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_employee_employee_no` (`employee_no`),
  UNIQUE KEY `uk_hr_employee_phone` (`phone`),
  KEY `idx_hr_employee_user_id` (`user_id`),
  KEY `idx_hr_employee_dept_id` (`dept_id`),
  KEY `idx_hr_employee_post_id` (`post_id`),
  KEY `idx_hr_employee_leader_id` (`leader_id`),
  KEY `idx_hr_employee_employment_status` (`employment_status`),
  KEY `idx_hr_employee_hire_date` (`hire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工主档表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 员工主档主键 | 自增主键 |
| `employee_no` | `VARCHAR(32)` | 是 | 工号 | 全局唯一，员工核心标识 |
| `user_id` | `BIGINT UNSIGNED` | 否 | 关联系统用户ID | 关联 `sys_user.id`，支持账号映射 |
| `dept_id` | `BIGINT UNSIGNED` | 是 | 所属部门ID | 关联 `sys_dept.id` |
| `post_id` | `BIGINT UNSIGNED` | 否 | 职位ID | 关联 `sys_post.id` |
| `leader_id` | `BIGINT UNSIGNED` | 否 | 直接汇报人员工ID | 关联 `hr_employee.id` |
| `employee_name` | `VARCHAR(64)` | 是 | 员工姓名 | 人事主数据核心字段 |
| `gender` | `VARCHAR(16)` | 否 | 性别 | 建议引用 `gender` 字典 |
| `phone` | `VARCHAR(20)` | 是 | 手机号 | 全局唯一，常用于通知与登录找回 |
| `email` | `VARCHAR(128)` | 否 | 邮箱 | 建议校验格式 |
| `id_card_no` | `VARCHAR(128)` | 否 | 身份证号 | 敏感字段，建议加密存储 |
| `birthday` | `DATE` | 否 | 生日 | 用于员工关怀与统计 |
| `domicile_address` | `VARCHAR(255)` | 否 | 户籍地址 | 属于敏感个人信息 |
| `current_address` | `VARCHAR(255)` | 否 | 现居住地址 | 属于敏感个人信息 |
| `job_level` | `VARCHAR(16)` | 否 | 职级 | 建议引用 `job_level` 字典 |
| `work_location` | `VARCHAR(128)` | 否 | 工作地点 | 支持多办公区管理 |
| `hire_type` | `VARCHAR(32)` | 否 | 入职类型 | 建议引用 `hire_type` 字典 |
| `employment_status` | `VARCHAR(32)` | 是 | 在职状态 | 建议引用 `employment_status` 字典 |
| `hire_date` | `DATE` | 是 | 入职日期 | 人事流程关键时间节点 |
| `probation_month` | `INT` | 否 | 试用期月数 | 默认 `3` 个月 |
| `probation_salary_ratio` | `DECIMAL(5,2)` | 否 | 试用期薪资比例 | 例如 `80.00`、`100.00` |
| `contract_type` | `VARCHAR(32)` | 否 | 合同类型 | 建议引用 `contract_type` 字典 |
| `contract_expire_date` | `DATE` | 否 | 合同到期日 | 用于合同到期预警 |
| `salary_template_id` | `BIGINT UNSIGNED` | 否 | 薪资账套ID | 关联薪酬模块主数据 |
| `base_salary` | `DECIMAL(12,2)` | 否 | 基本工资 | 建议配合权限脱敏展示 |
| `bank_account` | `VARCHAR(128)` | 否 | 银行账号 | 敏感字段，建议加密存储 |
| `bank_name` | `VARCHAR(128)` | 否 | 开户行 | 发薪打款使用 |
| `status` | `TINYINT` | 是 | 记录状态 | `1` 有效，`0` 禁用 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录员工档案补充说明 |

### 14.15 员工合同表

```sql
CREATE TABLE `hr_employee_contract` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `contract_no` VARCHAR(64) DEFAULT NULL COMMENT '合同编号',
  `contract_type` VARCHAR(32) NOT NULL COMMENT '合同类型',
  `start_date` DATE DEFAULT NULL COMMENT '合同开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '合同结束日期',
  `probation_month` INT DEFAULT NULL COMMENT '试用期（月）',
  `probation_salary_ratio` DECIMAL(5,2) DEFAULT NULL COMMENT '试用期薪资比例',
  `attachment_file_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '附件文件ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_employee_contract_employee_id` (`employee_id`),
  KEY `idx_hr_employee_contract_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工合同表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 合同主键 | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `contract_no` | `VARCHAR(64)` | 否 | 合同编号 | 建议按业务规则生成并保持唯一 |
| `contract_type` | `VARCHAR(32)` | 是 | 合同类型 | 建议引用 `contract_type` 字典 |
| `start_date` | `DATE` | 否 | 合同开始日期 | 用于合同周期计算 |
| `end_date` | `DATE` | 否 | 合同结束日期 | 用于到期预警 |
| `probation_month` | `INT` | 否 | 试用期月数 | 与员工主档可保持一致 |
| `probation_salary_ratio` | `DECIMAL(5,2)` | 否 | 试用期薪资比例 | 记录合同约定比例 |
| `attachment_file_id` | `BIGINT UNSIGNED` | 否 | 附件文件ID | 关联 `sys_file.id` |
| `status` | `TINYINT` | 是 | 合同状态 | `1` 有效，`0` 失效/停用 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录合同补充说明 |

### 14.16 员工调岗记录表

```sql
CREATE TABLE `hr_employee_transfer_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
  `from_dept_id` BIGINT UNSIGNED NOT NULL COMMENT '原部门ID',
  `to_dept_id` BIGINT UNSIGNED NOT NULL COMMENT '新部门ID',
  `from_post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '原职位ID',
  `to_post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '新职位ID',
  `from_job_level` VARCHAR(16) DEFAULT NULL COMMENT '原职级',
  `to_job_level` VARCHAR(16) DEFAULT NULL COMMENT '新职级',
  `from_leader_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '原汇报人ID',
  `to_leader_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '新汇报人ID',
  `salary_before` DECIMAL(12,2) DEFAULT NULL COMMENT '调整前薪资',
  `salary_after` DECIMAL(12,2) DEFAULT NULL COMMENT '调整后薪资',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '调岗原因',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_employee_transfer_record_employee_id` (`employee_id`),
  KEY `idx_hr_employee_transfer_record_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工调岗记录表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 调岗记录主键 | 自增主键 |
| `employee_id` | `BIGINT UNSIGNED` | 是 | 员工ID | 关联 `hr_employee.id` |
| `from_dept_id` | `BIGINT UNSIGNED` | 是 | 原部门ID | 关联 `sys_dept.id` |
| `to_dept_id` | `BIGINT UNSIGNED` | 是 | 新部门ID | 关联 `sys_dept.id` |
| `from_post_id` | `BIGINT UNSIGNED` | 否 | 原职位ID | 关联 `sys_post.id` |
| `to_post_id` | `BIGINT UNSIGNED` | 否 | 新职位ID | 关联 `sys_post.id` |
| `from_job_level` | `VARCHAR(16)` | 否 | 原职级 | 记录调整前职级 |
| `to_job_level` | `VARCHAR(16)` | 否 | 新职级 | 记录调整后职级 |
| `from_leader_id` | `BIGINT UNSIGNED` | 否 | 原汇报人ID | 关联 `hr_employee.id` |
| `to_leader_id` | `BIGINT UNSIGNED` | 否 | 新汇报人ID | 关联 `hr_employee.id` |
| `salary_before` | `DECIMAL(12,2)` | 否 | 调整前薪资 | 便于薪酬变动追踪 |
| `salary_after` | `DECIMAL(12,2)` | 否 | 调整后薪资 | 便于薪酬变动追踪 |
| `effective_date` | `DATE` | 是 | 生效日期 | 调岗正式生效时间 |
| `reason` | `VARCHAR(500)` | 否 | 调岗原因 | 记录调岗背景说明 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录调岗补充说明 |

### 14.17 审批实例表

```sql
CREATE TABLE `hr_approval_instance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_no` VARCHAR(64) NOT NULL COMMENT '审批单号',
  `approval_type` VARCHAR(32) NOT NULL COMMENT '审批类型：ENTRY/REGULAR/TRANSFER/LEAVE',
  `biz_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '业务主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '审批标题',
  `applicant_user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人用户ID',
  `applicant_employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '申请人员工ID',
  `current_node_name` VARCHAR(64) DEFAULT NULL COMMENT '当前节点名称',
  `approval_status` VARCHAR(32) NOT NULL COMMENT '审批状态',
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `form_json` JSON DEFAULT NULL COMMENT '表单快照',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_approval_instance_approval_no` (`approval_no`),
  KEY `idx_hr_approval_instance_type` (`approval_type`),
  KEY `idx_hr_approval_instance_status` (`approval_status`),
  KEY `idx_hr_approval_instance_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批实例表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 审批实例主键 | 自增主键 |
| `approval_no` | `VARCHAR(64)` | 是 | 审批单号 | 全局唯一，审批流核心编号 |
| `approval_type` | `VARCHAR(32)` | 是 | 审批类型 | 如 `ENTRY`、`REGULAR`、`TRANSFER`、`LEAVE` |
| `biz_id` | `BIGINT UNSIGNED` | 否 | 业务主键ID | 关联具体业务单据或记录 |
| `title` | `VARCHAR(255)` | 是 | 审批标题 | 用于待办和历史列表展示 |
| `applicant_user_id` | `BIGINT UNSIGNED` | 是 | 申请人用户ID | 关联 `sys_user.id` |
| `applicant_employee_id` | `BIGINT UNSIGNED` | 否 | 申请人员工ID | 关联 `hr_employee.id` |
| `current_node_name` | `VARCHAR(64)` | 否 | 当前节点名称 | 审批中用于展示当前进度 |
| `approval_status` | `VARCHAR(32)` | 是 | 审批状态 | 建议引用 `approval_status` 字典 |
| `apply_time` | `DATETIME` | 是 | 申请时间 | 审批发起时间 |
| `finish_time` | `DATETIME` | 否 | 完成时间 | 审批结束时回填 |
| `form_json` | `JSON` | 否 | 表单快照 | 保存审批发起时的业务数据快照 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录审批实例补充说明 |

### 14.18 审批任务表

```sql
CREATE TABLE `hr_approval_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` BIGINT UNSIGNED NOT NULL COMMENT '审批实例ID',
  `node_code` VARCHAR(64) NOT NULL COMMENT '节点编码',
  `node_name` VARCHAR(64) NOT NULL COMMENT '节点名称',
  `approver_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人用户ID',
  `approver_employee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人员工ID',
  `task_status` VARCHAR(32) NOT NULL COMMENT '任务状态',
  `approve_result` VARCHAR(32) DEFAULT NULL COMMENT '审批结果',
  `approve_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `receive_time` DATETIME DEFAULT NULL COMMENT '接收时间',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `deadline_time` DATETIME DEFAULT NULL COMMENT '截止时间',
  `sort_no` INT NOT NULL DEFAULT 1 COMMENT '节点顺序',
  `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_hr_approval_task_instance_id` (`instance_id`),
  KEY `idx_hr_approval_task_approver_user_id` (`approver_user_id`),
  KEY `idx_hr_approval_task_task_status` (`task_status`),
  KEY `idx_hr_approval_task_deadline_time` (`deadline_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批任务表';
```

| 字段名 | 类型 | 必填 | 字段作用 | 约束与说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 是 | 审批任务主键 | 自增主键 |
| `instance_id` | `BIGINT UNSIGNED` | 是 | 审批实例ID | 关联 `hr_approval_instance.id` |
| `node_code` | `VARCHAR(64)` | 是 | 节点编码 | 流程定义中的节点唯一标识 |
| `node_name` | `VARCHAR(64)` | 是 | 节点名称 | 用于待办展示 |
| `approver_user_id` | `BIGINT UNSIGNED` | 否 | 审批人用户ID | 关联 `sys_user.id` |
| `approver_employee_id` | `BIGINT UNSIGNED` | 否 | 审批人员工ID | 关联 `hr_employee.id` |
| `task_status` | `VARCHAR(32)` | 是 | 任务状态 | 如待处理、已处理、已转交 |
| `approve_result` | `VARCHAR(32)` | 否 | 审批结果 | 如通过、驳回、撤回 |
| `approve_comment` | `VARCHAR(500)` | 否 | 审批意见 | 记录审批说明 |
| `receive_time` | `DATETIME` | 否 | 接收时间 | 任务进入待办的时间 |
| `approve_time` | `DATETIME` | 否 | 审批时间 | 实际处理完成时间 |
| `deadline_time` | `DATETIME` | 否 | 截止时间 | 用于超时提醒 |
| `sort_no` | `INT` | 是 | 节点顺序 | 控制串并行节点顺序 |
| `create_by` | `BIGINT UNSIGNED` | 否 | 创建人 | 公共审计字段 |
| `create_time` | `DATETIME` | 是 | 创建时间 | 公共审计字段 |
| `update_by` | `BIGINT UNSIGNED` | 否 | 更新人 | 公共审计字段 |
| `update_time` | `DATETIME` | 是 | 更新时间 | 公共审计字段 |
| `is_deleted` | `TINYINT(1)` | 是 | 逻辑删除标记 | `0` 未删除，`1` 已删除 |
| `version` | `INT` | 是 | 乐观锁版本号 | 用于并发控制 |
| `remark` | `VARCHAR(500)` | 否 | 备注 | 记录审批任务补充说明 |

## 15. 推荐初始化字典项

建议在系统初始化阶段预置以下字典类型：

- `gender`
- `employment_status`
- `contract_type`
- `hire_type`
- `approval_status`
- `post_sequence`
- `job_level`
- `data_scope`

建议预置关键字典值：

- `employment_status`：`PROBATION`、`FORMAL`、`PENDING_LEAVE`、`LEFT`
- `contract_type`：`FIXED_TERM`、`NON_FIXED_TERM`、`LABOR`
- `post_sequence`：`M`、`P`、`S`

## 16. 版本记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.0 | 2026-07-09 | 首次整合全局技术底座、协同开发文档、项目 README 与 HRMS 产品规格说明书，形成统一开发规范与基础 SQL 基线 |
