---

# 《智愈先锋》后端系统详细设计说明书
| 文档信息 | 内容 |
| --- | --- |
| 文档名称 | 智愈先锋 后端系统详细设计说明书 |
| 项目名称 | 智愈先锋 —— AI驱动的全链路医疗健康平台 |
| 编写日期 | 2026-07-30 |
| 文档版本 | V1.0 |
| 文档状态 | 初稿 |
| 编写人 | 李世豪、高四军、王璐飞、李少博 |
| 参考文档 | Agent 模块系分 V2.0、C 端后端系分 V1.3、B 端后端系分 V1.0、SPHP 产品说明书 V1.10、项目计划 V1.3 |


## 变更记录
| 版本 | 日期 | 修订内容 | 修订人 |
| --- | --- | --- | --- |
| V1.0 | 2026-07-30 | 整合版初稿：将 C 端后端、B 端后端、AI Agent 三份子系分按统一标准模板合并；统一数据库 DDL（共享表 + C 端独有表，补全 c_user）、统一状态机、统一安全与幂等设计、统一接口总览与契约、统一排期甘特图 | 李世豪、高四军、王璐飞、李少博 |
| V1.1 | 2026-07-31 | 架构收敛：C/B 后端合并为单一 Spring Boot 应用（统一 :8080，路径前缀区分业务域）；新增 B 端账号表 b_user（ADMIN/DEPT_HEAD/DOCTOR）并基于 b_user 简化数据权限拦截器；删除 slot_snapshot.order_id 冗余字段；补充就诊人数量上限校验、处方审核权限校验、候补通知队列、支付回调预留；L3/L4 统一表述为"不注册工具" | 李世豪、高四军、王璐飞、李少博 |
| V1.2 | 2026-07-31 | 评审修复：P0 补全 Agent L2 确认协议（card 带 session_id、confirm 响应与错误码）、B 端 token/refresh 接口与 b_refresh_token 表、B 端管理接口统一 /api/b/admin/*、数据权限 NPE 防护；P1 补全通用错误码、幂等键统一、处方返回规则判定、库存锁定明细与解锁接口、工具 Schema 示例、支付回调占位、号源 scheduleStatus；P2 统一 L1-L4 边界、停用前置校验、药房预留字段、Agent 记忆策略、处方模板科室关联 | 李世豪、高四军、王璐飞、李少博 |


---

## 1. 项目背景
### 1.1 相关资料
+ SPHP_产品说明书（《智愈先锋 —— AI 驱动的全链路医疗健康平台 产品需求文档 PRD V1.10》）

### 1.2 参与人
| 角色 | 姓名 | 职责范围 |
| --- | --- | --- |
| 后端兼 AI 工程 | 李世豪 | AI Agent 服务（Python）、MCP Server、RAG、对话编排、与 Java 后端协作契约 |
| 后端兼前端 | 高四军 | B 端后端（医院/排班/诊疗/处方/库存）+ B 端管理后台前端 |
| 后端兼前端 | 王璐飞 | C 端后端（认证/挂号/问诊/购药/健康）+ C 端 H5 前端 |
| 后端兼质量测试 | 李少博 | 全流程测试、接口契约验证、防超卖压测、验收 |


### 1.3 项目目标与 MVP 范围
平台由 **C 端 H5 + B 端医院管理后台 + AI Agent 服务** 三端联动。AI 不是独立聊天机器人，而是在用户授权与人工确认前提下，通过 MCP 协议调用挂号、预约、订单、通知、随访等业务工具，将就医服务组织成闭环。

医疗安全声明：所有 AI 生成的导诊、报告解读、用药提醒、健康建议均须在界面显著标注"AI 建议仅供参考，不替代医生诊断"；急危重症、处方、诊断、关键治疗决策必须由执业医生确认。

| 端 | MVP 范围（08/12 前） | 后续版本 |
| --- | --- | --- |
| C 端 | 认证、就诊人、科室/医生/号源查询、导诊、挂号锁号、防超卖、模拟支付、挂号订单；问诊/购药/档案/提醒/随访按完整闭环设计 | — |
| B 端 | 医院/科室/医生管理、排班与号源配置、锁定号源看板、接诊、处方提交与审核、药品库存、患者管理、统计报表、AI 辅助面板 | V1.1~V1.3 |
| Agent | 导诊→挂号全链路工具调用、L2 确认、RAG 科普检索；B 端接诊辅助/处方审核/报告解读工具 | 情感化、知识图谱 |


Agent 三条边界：能查不能断、能建议不能决定、能代办不能代确认。四条治理原则：辅助而非替代、透明可追溯、最小权限、人工兜底。

### 1.4 术语表
| 术语 | 定义 |
| --- | --- |
| sphp-core | 核心共享 Maven 模块，含全局 Entity、Mapper、核心 Service、共享表 DDL |
| MCP | Model Context Protocol，AI 工具调用开放标准，JSON-RPC 通信；Agent 经 `tools/list` 发现工具、`tools/call` 执行工具 |
| MCP Client / Server | Client 为 Agent 编排层；Server 为 Agent 内嵌 Python 服务，封装 Java REST API |
| 防超卖 | Redis 原子扣减 + 数据库状态机 + 超时释放，确保号源/库存不超售 |
| 号源锁定 | 用户挂号后预扣号源，进入 15 分钟支付倒计时 |
| L1-L4 | 安全验证分级：L1 只读查询（导诊、科室医生查询、处方/报告列表等）；L2 需确认操作（创建/修改/取消业务操作，需用户确认）；L3 资金操作（支付、退款，Agent 不注册工具，前端仅跳转支付页）；L4 禁止操作（诊断、开具处方、删除病历等，Agent 不注册工具，禁止调用） |
| SSE | Server-Sent Events，前端与 Agent 间流式文本/卡片推送协议 |
| DataScope | 数据权限拦截器，按医院/科室/医生动态拼接 SQL 过滤条件 |
| confirm_token | L2 操作的一次性确认令牌，Redis 存储，5 分钟 TTL |


---

## 2. 系统架构与领域模型
### 2.1 整体部署视图
系统由两个独立应用进程 + 共享基础设施组成。Java 后端为**单一 Spring Boot 应用**（:8080），统一提供全部 REST API，按路径前缀区分业务域：C 端 `/api/c/v1/*`、B 端 `/api/b/*`；Agent Python（:8081，对前端直接暴露）。B/C 共用同一后端容器与端口，不再各自起独立进程；跨端数据通过共享 PostgreSQL 与 RabbitMQ 事件协作。

```mermaid
flowchart TB
    subgraph FE["前端"]
        C_APP["C端 H5 :8001<br/>手动页面 + AI导诊对话页"]
        B_WEB["B端 Web后台 :8000<br/>医生工作台 + AI辅助面板"]
    end
    subgraph AGENT["Agent :8081 (Python FastAPI + LangGraph)"]
        API["API接入层 对话端点+MCP适配"]
        ORCH["编排层 意图路由+对话子图"]
        ENG["引擎层 LLM+FunctionCalling+RAG+记忆"]
        MCPIN["MCP Server 内嵌 工具封装+Java REST客户端"]
        API --> ORCH --> ENG --> MCPIN
    end
    subgraph JAVA["Java 后端 :8080 (Spring Boot 单应用，C/B 共用)"]
        CAUTH["C端鉴权 CJwtInterceptor + token/parse"]
        BAUTH["B端鉴权 JWT独立密钥 + token/parse"]
        CAPI["C端业务API 认证/挂号/问诊/购药/健康"]
        BAPI["B端业务API 医院/排班/诊疗/处方/库存/统计"]
        CORE["sphp-core 共享Service+Mapper+数据权限拦截器"]
        CAPI --> CORE
        BAPI --> CORE
    end
    subgraph INFRA["基础设施"]
        PG[("PostgreSQL 16 + pgvector")]
        RD[("Redis 7")]
        MQ["RabbitMQ"]
    end
    C_APP -->|"POST /api/chat/stream Bearer JWT"| API
    B_WEB -->|"POST /api/chat/stream Bearer JWT"| API
    C_APP -->|"REST /api/c/v1/*"| CAPI
    B_WEB -->|"REST /api/b/*"| BAPI
    API -->|"JWT校验 方案A"| CAUTH
    API -->|"JWT校验 方案A"| BAUTH
    MCPIN -->|"HTTP REST X-User-Id"| CAPI
    MCPIN -->|"HTTP REST X-User-Id"| BAPI
    CORE --> PG
    CORE --> RD
    CORE --> MQ
    ENG --> PG
    ORCH --> RD
```

调用链路：前端 → Agent :8081（JWT 鉴权 → LLM 推理）。Agent 需业务数据时，编排层经 MCP Client 调内嵌 MCP Server（`tools/call`），MCP Server 向 Java :8080 发 REST 请求，Java 查库返回。Agent 不直连业务数据库，所有业务数据读写经 Java REST API。

**Agent 记忆与上下文管理：**

+ 会话上下文包含：`patientId`、`hospitalId`、`scope`、`userId`。
+ 会话过期时间：30 分钟无活动后自动失效。
+ 跨轮记忆策略：保留最近 10 轮对话，超出后使用摘要压缩。

### 2.2 后端内部分层与目录结构
```latex
sphp-project/
├── docs/                                # 文档（含本整合系分）
├── sphp-agent/                          # AI Agent (Python, :8081)
│   └── app/
│       ├── main.py                      # FastAPI + MCP Server 启动 + lifespan
│       ├── api/                         # ① 接入层 routes/middleware/schemas
│       ├── orchestrator/                # ② 编排层 graphs/nodes/state
│       ├── engine/                      # ③ 引擎层 llm/tools/rag/memory
│       ├── mcp_server/                  # MCP Server 内嵌 stdio + tools 封装
│       └── infrastructure/              # ④ 基础设施 audit/cache/java_client/config
└── sphp-server/                         # Java Maven 多模块
    ├── sphp-bootstrap/                  # 唯一启动模块 统一 :8080 /api
    ├── sphp-core/                       # B/C 共享：返回体/异常/JWT公共/共享Entity+Mapper+数据权限
    └── sphp-web/                        # 唯一业务模块 C/B 共用同一后端容器
        ├── com.sphp.b/                  # B端业务 (auth/security/admin/schedule/consult/prescription/drug/statistics)
        └── com.sphp.patient/            # C端业务 (auth/family/triage/registration/consultation/order/health/notification/filter/support)
```

| 层 / 模块 | 职责 | 禁止事项 |
| --- | --- | --- |
| sphp-core | 统一返回体、异常、共享实体、Mapper、数据权限拦截器、防超卖/号源/处方核心 Service | 禁止依赖 C/B 业务模块；禁止写业务 Controller |
| sphp-web com.sphp.b Controller | 参数校验、B 端 JWT 鉴权、响应包装、调 core Service | 不写核心业务逻辑 |
| sphp-web com.sphp.patient Controller | 参数校验、C 端 JWT 上下文、幂等、调 core/本模块 Service | 不写核心业务逻辑 |
| sphp-web com.sphp.patient support | 幂等、Redis Lua 锁库存、超时释放、支付回调、异步任务 | 禁止暴露 Controller |
| Agent 四层 | 接入层/编排层/引擎层/MCP Server/基础设施层，单向依赖 | 引擎层与 MCP Server 可独立测试，不互相依赖 |


Agent 内部分层规则：API 接入层 → 编排层 → 引擎层/MCP Server → 基础设施层；改动一个 graph 不触及 LLM 工厂，换 Java API 路径只改 `mcp_server/tools/`。

### 2.3 应用依赖关系
```mermaid
flowchart LR
    subgraph BE["Java 后端 :8080 (Spring Boot 单应用，C/B 共用)"]
        H["医院/科室/医生 API"]
        S["排班/号源 API"]
        A["挂号/防超卖 API"]
        Q["问诊/处方/购药/健康 API"]
    end
    subgraph AG["AI Agent (Agent 开发)"]
        PY["Python 编排+LLM"]
        MCP["MCP Server 工具封装"]
        TOOL["Function Calling Schema C30+B9"]
    end
    subgraph FR["前端"]
        BF["B端 管理页面"]
        CF["C端 导诊挂号页"]
    end
    H --> S --> A --> Q
    A --> MCP
    Q --> MCP
    TOOL --> MCP
    MCP --> PY
    BF --> H
    BF --> S
    CF --> A
    CF -.Agent模式.-> PY
```

依赖链：DB schema 定稿 → 医院/科室/医生 API → 排班/号源 API → 挂号+防超卖 API → MCP 工具封装 → Agent 对话链路。Agent 可先搭骨架 + Mock 工具响应，后端 API 就绪后逐个替换。

### 2.4 领域模型（UML 类图）
```mermaid
classDiagram
    class CUser { +id +account +passwordHash +status }
    class BUser { +id +account +passwordHash +role +hospitalId +doctorId +status }
    class Patient { +id +name +idCardCiphertext +phoneCiphertext +gender +dateOfBirth }
    class PatientUserRelation { +userId +patientId +relationship +isDefault +deletedAt }
    class Hospital { +id +name +level +status }
    class Department { +id +hospitalId +name +headDoctorId +status }
    class Doctor { +id +hospitalId +deptId +name +title +registrationFeeCent +status }
    class Schedule { +id +doctorId +scheduleDate +shift +totalSlots +status }
    class Slot { +id +scheduleId +startTime +endTime +totalCount +remainCount }
    class SlotSnapshot { +id +slotId +patientId +status +lockedAt +soldAt }
    class Appointment { +id +slotSnapshotId +patientId +doctorId +status +amountCent +expireAt }
    class ConsultRecord { +id +appointmentId +doctorId +patientId +status +aiSummary +doctorNote }
    class Prescription { +id +consultId +doctorId +patientId +status +rejectReason }
    class PrescriptionItem { +id +prescriptionId +drugId +dosage +frequency +usageMethod +days +quantity }
    class Drug { +id +hospitalId +name +specification +indication +contraindication +sideEffect +status }
    class Pharmacy { +id +hospitalId +name +isDefault +status }
    class PharmacyDrugStock { +id +pharmacyId +drugId +availableCount +lockedCount +safetyStock +unitPriceCent }
    class TriageAssessment { +id +patientId +hospitalId +symptomInput +urgency +recommendedDepartments }
    class AppointmentWaitlist { +id +patientId +slotId +queueNo +status }
    class ConsultationMessage { +id +consultId +senderType +content }
    class DrugOrder { +id +patientId +prescriptionId +pharmacyId +status +logisticsStatus +amountCent +expireAt }
    class PaymentOrder { +id +appointmentId +drugOrderId +payerUserId +amountCent +status +expireAt }
    class PatientReport { +id +patientId +reportName +reportDate +interpretationStatus +interpretation }
    class MedicationPlan { +id +patientId +prescriptionItemId +status +nextRemindAt }
    class FollowUpPlan { +id +patientId +consultId +dueAt +remindAt +status }
    class Notification { +id +userId +patientId +type +title +content +readAt }

    CUser "1" -- "*" PatientUserRelation : 绑定
    Patient "1" -- "*" PatientUserRelation : 被管理
    Hospital "1" -- "*" Department : has
    Department "1" -- "*" Doctor : contains
    Doctor "1" -- "*" Schedule : has
    Doctor "0..1" -- "0..1" BUser : 账号
    Schedule "1" -- "*" Slot : has
    Slot "1" -- "*" SlotSnapshot : tracks
    SlotSnapshot "1" -- "0..1" Appointment : 对应
    Patient "1" -- "*" Appointment : 发起
    Appointment "1" -- "0..1" ConsultRecord : 生成
    ConsultRecord "1" -- "0..*" Prescription : 开具
    Prescription "1" -- "*" PrescriptionItem : contains
    Drug "1" -- "*" PrescriptionItem : 引用
    Drug "1" -- "*" PharmacyDrugStock : 库存
    Pharmacy "1" -- "*" PharmacyDrugStock : 含
    Patient "1" -- "*" DrugOrder : 购药
    Appointment "1" -- "1" PaymentOrder : 支付
    DrugOrder "1" -- "1" PaymentOrder : 支付
    Patient "1" -- "*" TriageAssessment : 导诊
    Patient "1" -- "*" AppointmentWaitlist : 候补
    ConsultRecord "1" -- "*" ConsultationMessage : 消息
    Patient "1" -- "*" PatientReport : 报告
    Patient "1" -- "*" MedicationPlan : 用药
    Patient "1" -- "*" FollowUpPlan : 随访
    CUser "1" -- "*" Notification : 通知
```

### 2.5 模块职责边界
| 边界 | 说明 |
| --- | --- |
| B 端 vs C 端 | 同一 Spring Boot 应用内两个业务域，共用同一后端容器，不直接互调；跨端数据通过共享 PostgreSQL；RabbitMQ 仅事件通知（如停诊通知 C 端清缓存/推患者），不做跨端数据同步 |
| B/C vs sphp-core | Controller 只做接入层逻辑，核心业务（防超卖、号源状态机、处方流转、数据权限）在 core |
| 后端 vs Agent | Agent 经 MCP 调 Java REST 获取数据/执行操作；Java 不参与 AI 推理、工具编排、对话管理 |
| C 端账号 vs B 端账号 | B/C 拥有各自用户表（c_user / b_user），共用同一后端容器；JWT 双密钥隔离，令牌互不通用 |


---

## 3. 功能模块
### 3.1 功能模块树
```latex
智愈先锋 后端
├── 共享核心 (sphp-core)
│   ├── 统一返回体/全局异常/业务码
│   ├── 共享实体与 Mapper
│   ├── 数据权限拦截器 (DataScope, 医院/科室/医生隔离)
│   └── 核心 Service (号源/挂号/问诊/处方/库存/患者)
├── C 端业务 (com.sphp.patient)
│   ├── 认证与就诊人 (MVP)
│   │   ├── 图形验证码/注册/登录/刷新/退出
│   │   ├── 本人资料查询与更新/修改密码
│   │   ├── 家庭成员与就诊人管理 (最多5人, 软删除)
│   │   └── 只读 JWT 解析 (token/parse, 供 Agent)
│   ├── 导诊与挂号资源 (MVP)
│   │   ├── 医院/科室/医生/号源时段查询 (hospitalId 链路校验)
│   │   └── 症状导诊评估 (非诊断结论)
│   ├── 挂号订单与模拟支付 (MVP)
│   │   ├── 创建挂号锁号 (Redis 原子预扣+防超卖)
│   │   ├── 挂号订单列表/详情/取消
│   │   ├── 候补登记
│   │   └── 模拟支付/支付状态/支付回调预留
│   ├── 在线问诊 (扩展)
│   │   ├── 预问诊草稿/提交
│   │   ├── 问诊记录列表/详情/文字消息
│   │   └── 处方列表/详情/解读
│   ├── 处方购药 (扩展)
│   │   ├── 院内药房库存查询 (按处方医生医院, 默认药房优先)
│   │   ├── 购药订单草稿/列表/详情/取消/确认收货
│   │   └── 购药模拟支付 (支付后生成用药计划)
│   ├── 健康管理 (扩展)
│   │   ├── 健康档案/过敏史/既往史维护
│   │   ├── 检查报告录入/列表/详情/解读
│   │   ├── 用药计划查询/暂停/恢复/完成
│   │   └── 随访计划查询/确认
│   ├── 通知 (扩展) 列表/标记已读 (按 patientId 可选)
│   └── 支撑 support 幂等/Redis Lua 锁库存/超时释放/支付回调/RabbitMQ 异步
├── B 端业务 (com.sphp.b)
│   ├── 医院管理 医院信息/科室 CRUD+启停/医生 CRUD+启停
│   ├── 排班与号源 排班 CRUD/号源时段配置/发布与撤销/锁定看板/手动释放
│   ├── 诊疗管理 待接诊列表(风险置顶)/患者详情/开始与结束接诊/保存病历
│   ├── 处方管理 提交处方(风险拦截)/列表/详情/待审核/审核/模板
│   ├── 药品库存 药品目录/库存列表/更新库存/低库存预警
│   ├── 患者管理 列表/详情/就诊记录/历史处方/当前用药与随访
│   ├── 统计报表 运营总览/按科室/按日期
│   ├── AI 辅助 生成病历草稿（Agent直连，不经Java代理）
│   ├── 安全 JWT(独立密钥) 拦截器+RBAC+数据权限+token/parse
│   └── 处方风险拦截器 过敏/禁忌/重复用药 (ERROR 拦截, WARNING 提示)
└── AI Agent (sphp-agent)
    ├── 对话接入 POST /api/chat/stream (SSE) + /api/chat/confirm
    ├── 鉴权中间件 调 Java token/parse 换 userId (方案A)
    ├── 意图路由 主图 + 4 业务子图 (导诊/挂号/问诊/购药) + qa/chitchat
    ├── 工具引擎 C端30 + B端9 Function Calling Schema 注册中心
    ├── MCP Server 内嵌 stdio, 封装 Java REST (一个工具一个文件)
    ├── 安全校验 L1 直通 / L2 confirm_token / L3L4 不注册工具
    ├── RAG 知识库 pgvector 检索 (search_medical_knowledge/interpret_report 本地工具)
    ├── 对话记忆 buffer+summary, session 生命周期
    └── 知识库管理 ingest + search 接口
```

### 3.2 功能模块说明（节选关键规则）
1. **导诊评估**：采集症状/持续时间/体温/既往史/过敏史，返回紧急程度与至多 3 个推荐科室及理由，附免责声明；急危重症识别后引导线下就医，不下诊断结论。
2. **挂号锁号**：校验就诊人归属与 hospitalId 链路 → Redis Lua 原子预扣 → 事务创建 UNPAID 订单 + PENDING 支付单 → 发 15 分钟延迟消息；预扣失败返回号源不足并推荐候补。
3. **防超卖**：扣减走 Redis DECR（<5ms，1000+ TPS），异步写 slot_snapshot=LOCKED，乐观锁 CAS 兜底，超时/取消/手动释放走 INCR 归还。
4. **多就诊人适配**：除 `GET/PUT /profile` 外，患者相关接口可携带 `patientId`；未传取 `is_default=true` 本人；服务端统一以 `patient_user_relation.user_id=currentUserId and patient_id=? and deleted_at is null` 校验归属；详情/更新/取消沿资源 `patient_id` 反查校验。
5. **院内药房规则**：经 `prescription→consult_record→doctor.hospital_id` 确定供药医院，仅返回该院启用且库存满足的药房，`is_default=true` 排首位；不接受经纬度，配送固定 COURIER。
6. **处方风险拦截**：ERROR 级（如过敏强匹配）返回 3004 不入库；WARNING 级（如同成分重复）处方生效但弹窗提示；命中审核级规则 `auditRequired=true` 进入人工审核。
7. **Agent 工具分级**：L1 只读查询直通 MCP；L2 创建/修改/取消等业务操作生成 confirm_token 推确认卡片，用户确认后一次性消费执行；L3 资金操作（支付/退款）与 L4 禁止操作（诊断/开具处方/删除病历）不在 MCP 工具清单注册，Agent 无法调用，因此无需额外拦截。

---

## 4. 核心业务流程图
### 4.1 患者全链路主流程
```mermaid
flowchart TD
    A["账号密码登录"] --> B["查询科室/医生/号源"]
    B --> C{"选择入口"}
    C -->|手动| D["选科室→选医生→选时段→选就诊人"]
    C -->|Agent| E["描述症状→AI追问→AI推荐科室医生→用户确认"]
    D --> F["创建挂号锁号 Redis原子预扣"]
    E --> F
    F -->|预扣成功| G["15分钟内输入密码模拟支付"]
    F -->|余量不足| H["推荐其他号源/登记候补"]
    G -->|支付成功| I["挂号成功 号源SOLD"]
    G -->|超时或取消| J["释放号源 RELEASED"]
    I --> K["预问诊与文字问诊"]
    K --> L["医生开具处方 风险拦截"]
    L --> M["查询院内药房库存 创建购药草稿"]
    M --> N["模拟支付 生成用药计划与随访"]
```

### 4.2 号源抢占与防超卖流程（双入口共用）
```mermaid
flowchart LR
    A["用户/Agent 发起挂号"] --> B["Redis DECR slot remain"]
    B --> C{"remain >= 0 ?"}
    C -->|是| D["事务写 slot_snapshot=LOCKED + UNPAID订单 + PENDING支付单"]
    C -->|否| E["返回 B0201 号源已满 推荐候补"]
    D --> F["返回成功 进入支付 15分钟倒计时"]
    D --> G["RabbitMQ 延迟队列 15min"]
    G --> H{"是否已支付"}
    H -->|否| I["CANCELLED + RELEASED + INCR归还 + 通知候补"]
    H -->|是| J["确认 SOLD 清理锁定标记"]
```

### 4.3 排班与号源管理流程
```mermaid
flowchart TD
    A["创建排班 DRAFT"] --> B["配置号源时段 写slot+初始化Redis remain"]
    B --> C["发布排班 PUBLISHED 发SchedulePublishedEvent"]
    C --> D["C端可见号源"]
    D --> E["锁定号源看板 展示LOCKED+剩余超时"]
    E --> F{"异常锁定?"}
    F -->|是| G["管理员手动释放 分布式锁+CAS+INCR"]
    F -->|否| H["正常超时自动释放"]
```

### 4.4 处方提交与风险拦截流程
```mermaid
flowchart TD
    A["医生选药填用法 提交并签名"] --> B["风险拦截器"]
    B --> C{"命中规则级别"}
    C -->|ERROR 红线| D["返回3004 不入库 提示修改"]
    C -->|WARNING 提示| E["处方APPROVED 弹窗WARNING"]
    C -->|无命中| F["处方APPROVED 自动审核"]
    B --> G{"需人工审核?"}
    G -->|是| H["进入待审核列表 科室负责人/管理员审核"]
```

### 4.5 Agent 对话与 L2 确认流程
```mermaid
flowchart TD
    A["前端发起对话 content+scope+JWT"] --> B["调Java token/parse 换userId"]
    B --> C["意图识别 路由子图"]
    C --> D["LLM 决策工具+参数"]
    D --> E{"安全等级"}
    E -->|L3/L4| F["不注册工具 不产生tool_call"]
    E -->|L1| G["MCP tools/call 同步返回"]
    E -->|L2| H["生成confirm_token 推确认卡片 挂起"]
    H --> I["用户点击确认 回调confirm"]
    I --> J["Lua一次性消费token 校验userId+session"]
    J --> G
    G --> K["LLM 融合结果 流式回复 + 免责声明"]
```

---

## 5. 时序图
### 5.1 患者导诊挂号全流程（含 Agent + MCP）
```mermaid
sequenceDiagram
    participant U as 患者
    participant F as 前端 :8001
    participant A as Agent :8081
    participant M as MCP Server
    participant J as Java :8080
    participant P as PostgreSQL
    participant R as Redis
    U->>F: 我头疼发烧三天了
    F->>A: POST /api/chat/stream Bearer JWT + scope=c_end
    A->>J: GET /api/c/v1/auth/token/parse
    J-->>A: userId + account + tokenExpiresAt
    A->>A: 意图=导诊 追问体温
    A-->>F: SSE message 请问发烧多少度
    U->>F: 38.5度
    F->>A: POST /api/chat/stream
    A->>M: tools/call query_departments
    M->>J: GET /departments X-User-Id
    J->>P: 查启用科室
    J-->>M: 科室数据
    M-->>A: 结果
    A->>M: tools/call query_doctors
    M->>J: GET /doctors X-User-Id
    J-->>M: 医生+号源
    M-->>A: 结果
    A-->>F: SSE message 建议呼吸内科 3位医生有号
    U->>F: 选王医生 确认挂号
    A->>A: create_appointment 为L2 生成confirm_token
    A->>R: SET confirm token TTL 5min
    A-->>F: SSE card 确认卡片
    U->>F: 点击确认
    F->>A: POST /api/chat/confirm
    A->>R: GET+DEL confirm token 校验通过
    A->>M: tools/call create_appointment
    M->>J: POST /appointments X-User-Id X-Idempotency-Key
    J->>R: Lua 原子预扣
    J->>P: 事务 UNPAID订单+PENDING支付单
    J-->>M: appointmentId+paymentId
    M-->>A: 结果
    A-->>F: SSE message 挂号成功 15分钟内支付
```

### 5.2 排班发布与号源初始化
```mermaid
sequenceDiagram
    actor Admin as 管理员
    participant FE as B端前端
    participant BC as B端Controller
    participant Core as sphp-core
    participant DB as PostgreSQL
    participant R as Redis
    participant MQ as RabbitMQ
    Admin->>FE: 配置号源 总数30 分6时段
    FE->>BC: PUT /api/b/admin/schedules/{id}/slots
    BC->>Core: SlotConfigService.configure()
    Core->>DB: BATCH INSERT slot AVAILABLE
    Core->>R: 初始化 slot remain
    Admin->>FE: 发布排班
    FE->>BC: PUT /api/b/admin/schedules/{id}/publish
    BC->>Core: ScheduleService.publish()
    Core->>DB: UPDATE schedule PUBLISHED
    Core->>MQ: SchedulePublishedEvent
    Core-->>BC: 发布成功
```

### 5.3 接诊与 AI 辅助
```mermaid
sequenceDiagram
    actor Doctor as 医生
    participant FE as B端前端
    participant BC as B端Controller
    participant Core as sphp-core
    participant AG as Agent :8081
    Doctor->>FE: 查看待接诊列表
    FE->>BC: GET /api/b/doctor/queue
    BC->>Core: queryPendingList 风险置顶
    Core-->>BC: 列表 含AI预问诊摘要
    Doctor->>FE: 接诊
    FE->>BC: POST /api/b/doctor/consult/{id}/start
    BC->>Core: startConsult 校验无其他IN_PROGRESS
    Doctor->>FE: AI面板 查患者既往史
    FE->>AG: POST /api/chat/stream scope=b_end patientId
    AG->>BC: GET /api/b/auth/token/parse
    BC-->>AG: userId+roles
    AG->>BC: 业务API X-User-Id 聚合患者数据
    BC-->>AG: 数据
    AG-->>FE: SSE 解读
    Doctor->>FE: 结束问诊
    FE->>BC: POST /api/b/doctor/consult/{id}/end
```

### 5.4 处方提交风险拦截
```mermaid
sequenceDiagram
    actor Doctor as 医生
    participant FE as B端前端
    participant BC as B端Controller
    participant RI as 风险拦截器
    participant Core as sphp-core
    participant DB as PostgreSQL
    Doctor->>FE: 提交处方并签名
    FE->>BC: POST /api/b/prescriptions
    BC->>RI: filter 过敏+禁忌+重复用药
    alt 命中ERROR
        RI-->>BC: 红线拦截
        BC-->>FE: 3004 不入库
    else 命中WARNING或无命中
        BC->>Core: createPrescription
        Core->>DB: INSERT APPROVED
        Core-->>BC: 成功 含riskWarnings
        BC-->>FE: 处方生效 弹窗WARNING
    end
```

### 5.5 B 端鉴权协作（Agent ↔ Java）
```mermaid
sequenceDiagram
    participant FE as B端前端
    participant AG as Agent
    participant B as B端 token/parse
    participant Biz as B端业务API
    FE->>AG: POST /api/chat/stream scope=b_end JWT
    AG->>B: GET /api/b/auth/token/parse JWT
    B->>B: JWT 校验 提取userId/roles/hospitalId
    B-->>AG: userId+roles
    AG->>AG: 写AgentState 注册B端工具集
    AG->>Biz: 业务调用 X-User-Id
    Biz->>Biz: DataScope 按userId过滤
    Biz-->>AG: 业务数据
    AG-->>FE: SSE 流式返回
```



## 6. 数据库设计
### 6.1 设计规范
| 规范项 | 说明 |
| --- | --- |
| 主键 | `bigint GENERATED BY DEFAULT AS IDENTITY` |
| 时间 | `timestamptz`（带时区） |
| 金额 | `integer` 单位分（`amount_cent`/`unit_price_cent`） |
| 状态 | `varchar` + `CHECK` 约束，大写下划线枚举 |
| JSON | `jsonb` |
| 敏感字段 | 字段名 `xxx_ciphertext`；演示环境 patient 手机号/身份证按明文存储兼容旧字段名，**响应/日志强制脱敏**，密码仅存 jBCrypt `password_hash` |
| 软删除 | 业务表统一 `deleted_at timestamptz`（NULL 未删除） |
| 排序 | 不用 sort_order，按 created_at 或业务字段 |
| 命名 | 外键 `fk_{表}_{字段}`、CHECK `ck_{表}_{语义}`、唯一 `uk_{表}_{字段}`、索引 `idx_{表}_{字段}` |


共享表（含 `c_user`）由 B 端 Flyway 创建；C 端不重复建共享表，仅建独有表。

### 6.2 ER 图（共享 + B 端）
```mermaid
erDiagram
    Hospital ||--o{ Department : has
    Department ||--o{ Doctor : contains
    Doctor ||--o{ Schedule : has
    Schedule ||--o{ Slot : has
    Slot ||--o{ SlotSnapshot : tracks
    SlotSnapshot ||--o{ Appointment : has
    Appointment ||--|| ConsultRecord : becomes
    ConsultRecord ||--o{ Prescription : generates
    Prescription ||--o{ PrescriptionItem : contains
    Drug ||--o{ PrescriptionItem : referenced_by
    Drug ||--o{ PharmacyDrugStock : stocked_in
    Hospital ||--o{ Pharmacy : has
    Pharmacy ||--o{ PharmacyDrugStock : contains
    Patient ||--o{ Appointment : has
    Patient ||--o{ ConsultRecord : has
    Patient ||--o{ Prescription : has
    Patient ||--o{ PatientAllergy : has_allergies
    Patient ||--o{ PatientMedicalHistory : has_history
    CUser ||--o{ PatientUserRelation : binds
    Patient ||--o{ PatientUserRelation : managed
    Hospital ||--o{ BUser : 账号
    Doctor |o--o| BUser : 关联
    Hospital { bigint id PK varchar name varchar level varchar status }
    Department { bigint id PK bigint hospital_id FK varchar name varchar status }
    Doctor { bigint id PK bigint hospital_id FK bigint dept_id FK varchar name integer registration_fee_cent varchar status }
    Schedule { bigint id PK bigint doctor_id FK date schedule_date varchar shift integer total_slots varchar status }
    Slot { bigint id PK bigint schedule_id FK time start_time time end_time integer total_count integer remain_count }
    SlotSnapshot { bigint id PK bigint slot_id FK bigint patient_id FK varchar status timestamptz locked_at }
    Appointment { bigint id PK bigint slot_snapshot_id FK bigint patient_id FK varchar status integer amount_cent timestamptz expire_at }
    ConsultRecord { bigint id PK bigint appointment_id FK bigint doctor_id FK varchar status jsonb ai_summary text doctor_note }
    Prescription { bigint id PK bigint consult_id FK bigint doctor_id FK varchar status varchar reject_reason }
    PrescriptionItem { bigint id PK bigint prescription_id FK bigint drug_id FK varchar dosage smallint days smallint quantity }
    Drug { bigint id PK bigint hospital_id FK varchar name varchar specification text contraindication varchar status }
    Pharmacy { bigint id PK bigint hospital_id FK varchar name boolean is_default varchar status }
    PharmacyDrugStock { bigint id PK bigint pharmacy_id FK bigint drug_id FK integer available_count integer locked_count integer unit_price_cent }
    Patient { bigint id PK varchar name varchar id_card_ciphertext varchar phone_ciphertext varchar gender }
    PatientAllergy { bigint id PK bigint patient_id FK varchar allergen varchar severity }
    PatientMedicalHistory { bigint id PK bigint patient_id FK varchar content date occurred_at }
    CUser { bigint id PK varchar account varchar password_hash varchar status }
    BUser { bigint id PK varchar account varchar password_hash varchar role bigint hospital_id FK bigint doctor_id FK varchar status }
    PatientUserRelation { bigint id PK bigint user_id FK bigint patient_id FK varchar relationship boolean is_default }
```

### 6.3 ER 图（C 端独有）
```mermaid
erDiagram
    Patient ||--o{ TriageAssessment : triage
    Patient ||--o{ AppointmentWaitlist : waitlist
    ConsultRecord ||--o{ ConsultationMessage : messages
    Patient ||--o{ DrugOrder : orders
    DrugOrder ||--o{ DrugOrderItem : items
    DrugOrder ||--o{ DrugOrderLogisticsTrace : traces
    Appointment ||--o| PaymentOrder : pay
    DrugOrder ||--o| PaymentOrder : pay
    Patient ||--o{ PatientReport : reports
    PatientReport ||--o{ PatientReportIndicator : indicators
    Patient ||--o{ MedicationPlan : meds
    Patient ||--o{ FollowUpPlan : followups
    CUser ||--o{ Notification : notify
    CUser ||--o{ CRefreshToken : tokens
```

### 6.4 共享表（sphp-core）
#### 6.4.1 医院与科室
```sql
-- 医院表
CREATE TABLE hospital (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name            varchar(100) NOT NULL,
    level           varchar(20),
    description     varchar(500),
    address         varchar(200),
    contact         varchar(100),
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_hospital_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- 科室表
CREATE TABLE department (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hospital_id     bigint NOT NULL,
    name            varchar(64) NOT NULL,
    head_doctor_id  bigint,
    description     varchar(500),
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_department_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT ck_department_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- 医生表
CREATE TABLE doctor (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hospital_id          bigint NOT NULL,
    dept_id              bigint NOT NULL,
    name                 varchar(64) NOT NULL,
    title                varchar(64),
    specialty            varchar(1000),
    introduction         varchar(4000),
    license_no           varchar(64),
    phone                varchar(32),
    registration_fee_cent integer NOT NULL DEFAULT 0,
    status               varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT fk_doctor_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_doctor_dept FOREIGN KEY (dept_id) REFERENCES department(id),
    CONSTRAINT ck_doctor_status CHECK (status IN ('ENABLED', 'DISABLED', 'SUSPENDED'))
);
CREATE INDEX idx_doctor_dept_status ON doctor(dept_id, status) WHERE deleted_at IS NULL;
```

#### 6.4.2 排班与号源
```sql
-- 排班表
CREATE TABLE schedule (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    doctor_id       bigint NOT NULL,
    schedule_date   date NOT NULL,
    shift           varchar(10) NOT NULL,
    total_slots     integer NOT NULL DEFAULT 0,
    status          varchar(20) NOT NULL DEFAULT 'DRAFT',
    published_at    timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT uk_schedule_doctor_date_shift UNIQUE (doctor_id, schedule_date, shift),
    CONSTRAINT ck_schedule_shift CHECK (shift IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_schedule_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

-- 号源时段表
CREATE TABLE slot (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    schedule_id     bigint NOT NULL,
    start_time      time NOT NULL,
    end_time        time NOT NULL,
    total_count     integer NOT NULL,
    remain_count    integer NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_slot_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    CONSTRAINT ck_slot_time CHECK (end_time > start_time),
    CONSTRAINT ck_slot_count CHECK (total_count >= 0 AND remain_count BETWEEN 0 AND total_count)
);

-- 号源快照表（每个号源独立记录）
CREATE TABLE slot_snapshot (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    slot_id         bigint NOT NULL,
    patient_id      bigint,
    status          varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_at       timestamptz,
    sold_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_slot_snapshot_slot FOREIGN KEY (slot_id) REFERENCES slot(id),
    CONSTRAINT fk_slot_snapshot_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT ck_slot_snapshot_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'SOLD', 'EXPIRED', 'RELEASED'))
);
CREATE INDEX idx_slot_snapshot_slot_status ON slot_snapshot(slot_id, status) WHERE deleted_at IS NULL;
```

> slot表的相关说明：`remain_count`：该时段剩余号源数，API 响应字段名为 `availableCount`。
>

#### 6.4.3 患者与用户
```sql
-- C端用户表
CREATE TABLE c_user (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account         varchar(32) NOT NULL UNIQUE,
    password_hash   varchar(100) NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_c_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- B端用户表（医生/科室负责人/管理员账号，与 doctor 表解耦）
-- role: ADMIN 医院管理员（无需 doctor_id）；DEPT_HEAD 科室负责人（经 doctor_id 取科室）；DOCTOR 普通医生（经 doctor_id 关联医生）
CREATE TABLE b_user (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account         varchar(32) NOT NULL UNIQUE,
    password_hash   varchar(100) NOT NULL,
    role            varchar(20) NOT NULL,          -- ADMIN / DEPT_HEAD / DOCTOR
    hospital_id     bigint NOT NULL,               -- 所属医院（数据隔离）
    doctor_id       bigint,                        -- role=DOCTOR/DEPT_HEAD 时关联医生表
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_b_user_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_b_user_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT ck_b_user_role CHECK (role IN ('ADMIN', 'DEPT_HEAD', 'DOCTOR')),
    CONSTRAINT ck_b_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
);
CREATE INDEX idx_b_user_hospital_role ON b_user(hospital_id, role) WHERE deleted_at IS NULL;

**业务规则：**
- 创建医生记录（doctor）时，可选同步创建 b_user 账号；若不创建，由管理员后续手动开通。
- 停用医生（doctor.status=DISABLED）时，若存在关联 b_user，建议同步停用（或由管理员决定）。
- `ADMIN` 角色无 `doctor_id`，数据权限仅按 `hospital_id` 过滤。
- `DEPT_HEAD` 的科室归属通过 `b_user.doctor_id → doctor.dept_id` 获取。
- `DOCTOR` 角色的数据权限为本人（`hospital_id + doctor_id`）。

-- B端刷新令牌（对应 b_user，参考 c_refresh_token）
CREATE TABLE b_refresh_token (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id         bigint NOT NULL REFERENCES b_user(id),
    token_hash      varchar(128) NOT NULL,
    expired_at      timestamptz NOT NULL,
    revoked_at      timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_b_refresh_token_hash UNIQUE (token_hash)
);

-- 患者表（就诊人）
CREATE TABLE patient (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name                 varchar(64) NOT NULL,
    id_card_ciphertext   varchar(512),
    phone_ciphertext     varchar(512),
    gender               varchar(10),
    date_of_birth        date,
    emergency_contact    varchar(256),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT ck_patient_gender CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN'))
);

-- 患者用户关联表（家庭成员关系）
CREATE TABLE patient_user_relation (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id         bigint NOT NULL,
    patient_id      bigint NOT NULL,
    relationship    varchar(20) NOT NULL,
    is_default      boolean NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_patient_user_relation_user FOREIGN KEY (user_id) REFERENCES c_user(id),
    CONSTRAINT fk_patient_user_relation_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT ck_patient_user_relation_relationship CHECK (relationship IN ('SELF', 'SPOUSE', 'PARENT', 'CHILD', 'OTHER'))
);
CREATE INDEX idx_patient_user_relation_user ON patient_user_relation(user_id) WHERE deleted_at IS NULL;

-- 患者过敏史
CREATE TABLE patient_allergy (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL,
    allergen        varchar(128) NOT NULL,
    reaction        varchar(512),
    severity        varchar(20),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_patient_allergy_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT ck_patient_allergy_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE'))
);

-- 患者既往史
CREATE TABLE patient_medical_history (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL,
    content         varchar(2000) NOT NULL,
    occurred_at     date,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_patient_medical_history_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
);
```

#### 6.4.4 挂号与问诊
```sql
-- 挂号订单表
CREATE TABLE appointment (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    slot_snapshot_id     bigint NOT NULL,
    patient_id           bigint NOT NULL,
    doctor_id            bigint NOT NULL,
    status               varchar(20) NOT NULL DEFAULT 'UNPAID',
    amount_cent          integer NOT NULL,
    expire_at            timestamptz,
    paid_at              timestamptz,
    cancelled_at         timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT fk_appointment_slot_snapshot FOREIGN KEY (slot_snapshot_id) REFERENCES slot_snapshot(id),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT ck_appointment_status CHECK (status IN ('UNPAID', 'PAID', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_appointment_amount CHECK (amount_cent >= 0)
);
CREATE INDEX idx_appointment_patient_created ON appointment(patient_id, created_at DESC) WHERE deleted_at IS NULL;

-- 问诊记录表
CREATE TABLE consult_record (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    appointment_id       bigint NOT NULL,
    doctor_id            bigint NOT NULL,
    patient_id           bigint NOT NULL,
    status               varchar(20) NOT NULL DEFAULT 'PENDING',
    ai_summary           jsonb,
    doctor_note          text,
    started_at           timestamptz,
    ended_at             timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT fk_consult_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id),
    CONSTRAINT fk_consult_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT fk_consult_record_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT uk_consult_record_appointment UNIQUE (appointment_id),
    CONSTRAINT ck_consult_record_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW'))
);
CREATE INDEX idx_consult_record_doctor_status ON consult_record(doctor_id, status) WHERE deleted_at IS NULL;
```

#### 6.4.5 处方与药品
```sql
-- 处方表
CREATE TABLE prescription (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    consult_id           bigint NOT NULL,
    doctor_id            bigint NOT NULL,
    patient_id           bigint NOT NULL,
    status               varchar(20) NOT NULL DEFAULT 'DRAFT',
    reject_reason        varchar(1000),
    issued_at            timestamptz,
    audited_at           timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT fk_prescription_consult FOREIGN KEY (consult_id) REFERENCES consult_record(id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT ck_prescription_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

-- 处方明细
CREATE TABLE prescription_item (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    prescription_id bigint NOT NULL,
    drug_id         bigint NOT NULL,
    dosage          varchar(50) NOT NULL,
    frequency       varchar(50),
    usage_method    varchar(50) NOT NULL,
    days            smallint NOT NULL,
    quantity        smallint NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    CONSTRAINT fk_prescription_item_drug FOREIGN KEY (drug_id) REFERENCES drug(id)
);

-- 药品目录
CREATE TABLE drug (
    id                   bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hospital_id          bigint NOT NULL,
    name                 varchar(200) NOT NULL,
    specification        varchar(100) NOT NULL,
    manufacturer         varchar(200),
    approval_number      varchar(50),
    unit                 varchar(32) NOT NULL DEFAULT '盒',
    indication           text,
    contraindication     text,
    side_effect          text,
    status               varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    deleted_at           timestamptz,
    CONSTRAINT fk_drug_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT uk_drug_hospital_approval UNIQUE (hospital_id, approval_number),
    CONSTRAINT ck_drug_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- 药房
CREATE TABLE pharmacy (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hospital_id     bigint NOT NULL,
    name            varchar(128) NOT NULL,
    address         varchar(500),
    phone           varchar(32),
    is_default      boolean NOT NULL DEFAULT false,
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_pharmacy_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT uk_pharmacy_hospital_name UNIQUE (hospital_id, name),
    CONSTRAINT ck_pharmacy_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- 药房药品库存
CREATE TABLE pharmacy_drug_stock (
    id                  bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    pharmacy_id         bigint NOT NULL,
    drug_id             bigint NOT NULL,
    available_count     integer NOT NULL DEFAULT 0,
    locked_count        integer NOT NULL DEFAULT 0,
    safety_stock        integer NOT NULL DEFAULT 0,
    unit_price_cent     integer NOT NULL,
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_pharmacy_drug_stock_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacy(id),
    CONSTRAINT fk_pharmacy_drug_stock_drug FOREIGN KEY (drug_id) REFERENCES drug(id),
    CONSTRAINT uk_pharmacy_drug_stock UNIQUE (pharmacy_id, drug_id),
    CONSTRAINT ck_stock_count CHECK (available_count >= 0 AND locked_count >= 0 AND safety_stock >= 0)
);

-- 处方模板表
CREATE TABLE prescription_template (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hospital_id     bigint NOT NULL,               -- 所属医院
    dept_id         bigint,                        -- 关联科室，NULL 表示全院通用
    name            varchar(128) NOT NULL,         -- 模板名称
    doctor_id       bigint NOT NULL,               -- 创建人
    items           jsonb NOT NULL,                -- 药品明细 JSON，结构与处方明细一致
    status          varchar(20) NOT NULL DEFAULT 'ENABLED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT fk_template_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id),
    CONSTRAINT fk_template_dept FOREIGN KEY (dept_id) REFERENCES department(id),
    CONSTRAINT fk_template_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT ck_template_status CHECK (status IN ('ENABLED', 'DISABLED'))
);
```

### 6.5 C 端独有表
```sql
-- C端刷新令牌
CREATE TABLE c_refresh_token (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id         bigint NOT NULL REFERENCES c_user(id),
    token_hash      varchar(128) NOT NULL,
    expired_at      timestamptz NOT NULL,
    revoked_at      timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_c_refresh_token_hash UNIQUE (token_hash)
);

-- 导诊评估
CREATE TABLE triage_assessment (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    hospital_id     bigint NOT NULL REFERENCES hospital(id),
    symptom_input   jsonb NOT NULL,
    urgency         varchar(20) NOT NULL,
    recommended_departments jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_triage_assessment_urgency CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- 挂号候补
CREATE TABLE appointment_waitlist (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    slot_id         bigint NOT NULL REFERENCES slot(id),
    queue_no        integer NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'WAITING',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT uk_waitlist_patient_slot UNIQUE (patient_id, slot_id),
    CONSTRAINT ck_waitlist_status CHECK (status IN ('WAITING', 'NOTIFIED', 'CANCELLED', 'EXPIRED'))
);

-- 支付订单
CREATE TABLE payment_order (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    appointment_id  bigint REFERENCES appointment(id),
    drug_order_id   bigint REFERENCES drug_order(id),
    payer_user_id   bigint NOT NULL REFERENCES c_user(id),
    amount_cent     integer NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'PENDING',
    expire_at       timestamptz NOT NULL,
    paid_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_business CHECK (num_nonnulls(appointment_id, drug_order_id) = 1),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CLOSED'))
);

-- 问诊消息
CREATE TABLE consultation_message (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    consult_id      bigint NOT NULL REFERENCES consult_record(id),
    sender_type     varchar(20) NOT NULL,
    content         varchar(2000) NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_consultation_message_sender CHECK (sender_type IN ('PATIENT', 'DOCTOR', 'SYSTEM'))
);

-- 购药订单
CREATE TABLE drug_order (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    prescription_id bigint NOT NULL REFERENCES prescription(id),
    pharmacy_id     bigint NOT NULL REFERENCES pharmacy(id),
    delivery_method varchar(20) NOT NULL DEFAULT 'COURIER',
    delivery_address varchar(500) NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    logistics_status varchar(30) NOT NULL DEFAULT 'PENDING_SHIPMENT',
    amount_cent     integer NOT NULL,
    expire_at       timestamptz NOT NULL,
    logistics_company varchar(128),
    tracking_no     varchar(64),
    shipped_at      timestamptz,
    delivered_at    timestamptz,
    received_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_drug_order_status CHECK (status IN ('DRAFT', 'PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_drug_order_logistics_status CHECK (logistics_status IN ('PENDING_SHIPMENT', 'SHIPPED', 'IN_TRANSIT', 'TO_RECEIVE', 'RECEIVED'))
);

-- 检查报告
CREATE TABLE patient_report (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    report_name     varchar(256) NOT NULL,
    report_date     date NOT NULL,
    interpretation_status varchar(20) NOT NULL DEFAULT 'PENDING',
    interpretation  jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_report_interpretation_status CHECK (interpretation_status IN ('PENDING', 'READY', 'FAILED'))
);

-- 用药计划
CREATE TABLE medication_plan (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    prescription_item_id bigint NOT NULL REFERENCES prescription_item(id),
    drug_name_snapshot varchar(128) NOT NULL,
    frequency       varchar(128) NOT NULL,
    usage_method    varchar(128),
    next_remind_at  timestamptz,
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_medication_plan_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED'))
);

-- 随访计划
CREATE TABLE follow_up_plan (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    patient_id      bigint NOT NULL REFERENCES patient(id),
    appointment_id  bigint REFERENCES appointment(id),
    consult_id      bigint REFERENCES consult_record(id),
    due_at          timestamptz NOT NULL,
    remind_at       timestamptz,
    status          varchar(20) NOT NULL DEFAULT 'PENDING_CONFIRM',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    CONSTRAINT ck_follow_up_status CHECK (status IN ('PENDING_CONFIRM', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);

-- 通知
CREATE TABLE notification (
    id              bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id         bigint NOT NULL REFERENCES c_user(id),
    patient_id      bigint REFERENCES patient(id),
    patient_name_snapshot varchar(64),
    type            varchar(64) NOT NULL,
    title           varchar(256) NOT NULL,
    content         text NOT NULL,
    payload         jsonb NOT NULL DEFAULT '{}'::jsonb,
    read_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz
);
```

### 6.6 状态机设计
#### 6.6.1 号源状态机
```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : 排班发布
    AVAILABLE --> LOCKED : 用户挂号（预扣）
    LOCKED --> SOLD : 支付成功
    LOCKED --> AVAILABLE : 超时/用户取消/管理员释放
    AVAILABLE --> EXPIRED : 排班结束日
    LOCKED --> EXPIRED : 排班结束日
    SOLD --> [*]
    EXPIRED --> [*]
```

#### 6.6.2 挂号订单状态机
```mermaid
stateDiagram-v2
    [*] --> UNPAID : 创建订单
    UNPAID --> PAID : 支付成功
    UNPAID --> CANCELLED : 超时/用户取消
    PAID --> COMPLETED : 就诊结束
    PAID --> CANCELLED : 管理员取消
    COMPLETED --> [*]
    CANCELLED --> [*]
```

#### 6.6.3 处方状态机
```mermaid
stateDiagram-v2
    [*] --> DRAFT : 医生开方
    DRAFT --> SUBMITTED : 签名提交
    SUBMITTED --> APPROVED : 自动/人工审核通过
    SUBMITTED --> REJECTED : 审核驳回
    REJECTED --> SUBMITTED : 修改后重提
    APPROVED --> CANCELLED : 作废
    APPROVED --> [*]
    CANCELLED --> [*]
```



## 7. API 设计
### 7.1 全局约定
| 项目 | 约定 |
| --- | --- |
| C 端基础路径 | `/api/c/v1` |
| B 端基础路径 | `/api/b` |
| 鉴权方式 | JWT Bearer Token（C/B 端独立签发，密钥隔离） |
| 幂等键 | 所有 POST/PUT/DELETE 状态变更接口均须在 Header 携带 `X-Idempotency-Key`，Redis 存 24h。创建型接口和取消类接口必填；查询类接口不需要 |
| 时间格式 | ISO-8601，东八区，如 `2026-07-30T10:00:00+08:00` |
| 金额 | 整数分 `amountCent` |
| TraceId | 全链路追踪 ID，响应中返回 |


#### 7.1.1 统一响应格式
```json
{
    "code": "00000",
    "message": "操作成功",
    "data": { ... },
    "traceId": "01J7X..."
}
```

#### 7.1.2 通用错误码
| 错误码 | HTTP 状态 | 说明 | 所属模块 |
| --- | --- | --- | --- |
| 00000 | 200 | 成功 | — |
| A0301 | 401 | 未授权/Token 无效 | 全局 |
| A0400 | 400 | 参数校验失败 | 全局 |
| A0402 | 404 | 资源不存在 | 全局 |
| A0443 | 409 | 状态冲突/不可操作 | 全局 |
| A0506 | 409 | 重复请求（幂等冲突） | 全局 |
| A0111 | 409 | 账号已存在 | C端认证 |
| A0120 | 400 | 密码校验失败 | C端认证/支付 |
| A0240 | 400 | 验证码错误/过期 | C端认证 |
| A0501 | 429 | 请求频率超限 | 全局 |
| B0001 | 500 | 系统内部异常 | 全局 |
| B0201 | 409 | 号源库存竞争失败 | C端挂号 |
| B0202 | 409 | 业务状态冲突 | 全局 |
| B0300 | 409 | 药品库存不足 | C端购药 |
| 3001 | 404 | 患者不存在 | B端处方 |
| 3002 | 409 | 问诊记录不存在或不可开方 | B端处方 |
| 3003 | 404 | 药品不存在或已停用 | B端处方 |
| 3004 | 409 | 红线规则拦截（禁止提交） | B端处方 |
| 3010 | 403 | 无权查看该患者 | B端接诊 |
| 3011 | 409 | 问诊记录不存在或状态不可接诊 | B端接诊 |
| 3012 | 409 | 医生当前存在未结束的接诊记录 | B端接诊 |
| 3013 | 409 | 问诊状态不是 IN_PROGRESS | B端接诊 |
| 3014 | 409 | 存在未签名的处方草稿 | B端接诊 |
| 3020 | 403 | 无权查看该处方 | B端处方 |
| 3021 | 403 | 当前角色无处方审核权限 | B端处方审核 |
| 3022 | 409 | 处方状态不是 SUBMITTED | B端处方审核 |
| 3023 | 400 | 驳回时必须填写驳回原因 | B端处方审核 |
| 4001 | 409 | 科室下存在启用医生，无法停用 | B端科室管理 |
| 4002 | 409 | 科室下存在已发布排班，无法停用 | B端科室管理 |
| 4003 | 409 | 科室下存在进行中问诊，无法停用 | B端科室管理 |


### 7.2 C 端 API 设计
#### 7.2.1 认证模块
**（1）GET **`/auth/captcha`** — 获取图形验证码**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| 无 | — | — | — | — |


**响应示例：**

```json
{
    "code": "00000",
    "message": "获取验证码成功",
    "data": {
        "challengeId": "cap_01J7X8A2",
        "imageBase64": "data:image/svg+xml;base64,PHN2Zy...",
        "expireSeconds": 120
    }
}
```

**（2）POST **`/auth/register`** — 注册**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| account | Body | String | 是 | 登录账号，4-32 位 |
| password | Body | String | 是 | 密码，8-64 位 |
| challengeId | Body | String | 是 | 验证码挑战 ID |
| captchaCode | Body | String | 是 | 图形验证码 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "注册成功",
    "data": { "userId": 10001, "account": "patient_zhangsan" }
}
```

| 错误码 | 说明 |
| --- | --- |
| A0240 | 验证码无效 |
| A0111 | 账号已存在 |


**（3）POST **`/auth/login`** — 登录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| account | Body | String | 是 | 登录账号 |
| password | Body | String | 是 | 登录密码 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "登录成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "rt_01J7X8D5...",
        "expiresIn": 7200,
        "user": { "id": 10001, "account": "patient_zhangsan" }
    }
}
```

**（4）GET **`/auth/token/parse`** — 解析 JWT（供 Agent 调用）**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer C 端 Access Token |


**响应示例：**

```json
{
    "code": "00000",
    "message": "令牌解析成功",
    "data": {
        "userId": 10001,
        "account": "zhangsan",
        "tokenExpiresAt": "2026-07-30T12:00:00+08:00"
    }
}
```

#### 7.2.2 挂号模块
**（1）GET **`/departments`** — 查询科室**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| hospitalId | Query | Long | 是 | 医院 ID |
| keyword | Query | String | 否 | 模糊搜索 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": [
        { "id": 301, "name": "呼吸内科", "description": "呼吸系统疾病诊疗" }
    ]
}
```

**（2）GET **`/doctors`** — 查询医生**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| hospitalId | Query | Long | 是 | 医院 ID |
| departmentId | Query | Long | 是 | 科室 ID |
| date | Query | Date | 否 | 出诊日期 |
| pageNo | Query | Integer | 否 | 页码，默认 1 |
| pageSize | Query | Integer | 否 | 每页条数，默认 20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": {
        "pageNo": 1,
        "pageSize": 20,
        "total": 1,
        "records": [
            {
                "id": 401,
                "name": "王医生",
                "title": "主任医师",
                "specialty": "哮喘、慢阻肺",
                "registrationFeeCent": 5000,
                "availableCount": 6
            }
        ]
    }
}
```

**（3）GET **`/doctors/{doctorId}/slots`** — 查询时段**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| doctorId | Path | Long | 是 | 医生 ID |
| hospitalId | Query | Long | 是 | 医院 ID |
| date | Query | Date | 是 | 查询日期 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": [
        {
            "slotId": 501,
            "startTime": "2026-07-30T09:00:00+08:00",
            "endTime": "2026-07-30T09:30:00+08:00",
            "feeCent": 5000,
            "availableCount": 6,
            "scheduleStatus": "PUBLISHED"     // PUBLISHED / CANCELLED / DRAFT
        }
    ]
}
```

> `availableCount`：对应数据库 `slot.remain_count`，当前可预约号源数量，表示当前剩余可预约号源数。
>

**（4）POST **`/appointments`** — 创建挂号订单**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |
| patientId | Body | Long | 否 | 就诊人 ID，默认本人 |
| hospitalId | Body | Long | 是 | 医院 ID |
| slotId | Body | Long | 是 | 号源时段 ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "号源锁定成功，请在15分钟内完成支付",
    "data": {
        "appointmentId": 7001,
        "status": "UNPAID",
        "amountCent": 5000,
        "expireAt": "2026-07-28T10:15:00+08:00",
        "paymentId": 8001
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| B0201 | 号源已约满 |
| A0506 | 重复请求或已有冲突挂号 |


**（5）GET **`/appointments`** — 查询挂号订单列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| patientId | Query | Long | 否 | 就诊人 ID，不传默认本人 |
| status | Query | String | 否 | UNPAID/PAID/COMPLETED/CANCELLED |
| pageNo | Query | Integer | 否 | 页码，默认 1 |
| pageSize | Query | Integer | 否 | 每页条数，默认 20 |


**请求示例：**

```http
GET /api/c/v1/appointments?patientId=20002&status=PAID&pageNo=1&pageSize=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": {
        "pageNo": 1,
        "pageSize": 10,
        "total": 1,
        "records": [
            {
                "id": 7001,
                "doctorName": "王医生",
                "departmentName": "呼吸内科",
                "startTime": "2026-07-30T09:00:00+08:00",
                "status": "PAID",
                "amountCent": 5000,
                "expireAt": null
            }
        ]
    }
}
```

**（6）GET **`/appointments/{appointmentId}`** — 查询挂号订单详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| appointmentId | Path | Long | 是 | 订单 ID |


**请求示例：**

```http
GET /api/c/v1/appointments/7001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": {
        "id": 7001,
        "status": "UNPAID",
        "doctor": {
            "id": 401,
            "name": "王医生",
            "departmentName": "呼吸内科"
        },
        "slot": {
            "id": 501,
            "startTime": "2026-07-30T09:00:00+08:00",
            "endTime": "2026-07-30T09:30:00+08:00"
        },
        "amountCent": 5000,
        "expireAt": "2026-07-28T10:15:00+08:00",
        "payment": {
            "id": 8001,
            "status": "PENDING"
        }
    }
}
```

**（7）POST **`/appointments/{appointmentId}/cancel`** — 取消未支付订单**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |
| appointmentId | Path | Long | 是 | 订单 ID |


**请求示例：**

```http
POST /api/c/v1/appointments/7001/cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
X-Idempotency-Key: uuid-5678
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "挂号订单已取消",
    "data": {
        "appointmentId": 7001,
        "status": "CANCELLED"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| A0443 | 订单状态不是 UNPAID，不可取消 |
| A0301 | 订单不属于当前用户 |


**（8）POST **`/waitlists`** — 创建候补登记**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |
| slotId | Body | Long | 是 | 已约满的号源时段 ID |
| patientId | Body | Long | 否 | 就诊人 ID，默认本人 |


**请求示例：**

```http
POST /api/c/v1/waitlists
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
X-Idempotency-Key: uuid-9012

{
    "slotId": 501,
    "patientId": 20002
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "候补登记成功",
    "data": {
        "waitlistId": 9001,
        "slotId": 501,
        "status": "WAITING",
        "queueNo": 3
    }
}
```

**（9）GET **`/payments/{paymentId}`** — 查询支付状态**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| paymentId | Path | Long | 是 | 支付单 ID |


**请求示例：**

```http
GET /api/c/v1/payments/8001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": {
        "id": 8001,
        "appointmentOrderId": 7001,
        "drugOrderId": null,
        "amountCent": 5000,
        "status": "SUCCESS",
        "paidAt": "2026-07-28T10:02:00+08:00",
        "expireAt": "2026-07-28T10:15:00+08:00"
    }
}
```

**（10）POST **`/payments/{paymentId}/simulate-pay`** — 模拟支付**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| paymentId | Path | Long | 是 | 支付单 ID |
| loginPassword | Body | String | 是 | 登录密码 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "支付成功",
    "data": {
        "paymentId": 8001,
        "status": "SUCCESS",
        "paidAt": "2026-07-29T10:17:00+08:00"
    }
}
```

**（11）POST **`/payments/callback`** — 支付回调（预留，当前版本不实现）**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| transactionId | Body | String | 是 | 第三方支付交易号 |
| paymentId | Body | Long | 是 | 内部支付单 ID |
| status | Body | String | 是 | SUCCESS / FAILED |
| sign | Body | String | 是 | 渠道签名 |


> 预留路由，当前版本仅定义占位结构，不实现业务逻辑。MVP 仅提供 `simulate-pay` 模拟支付；后续接入真实支付渠道时完善签名校验和状态同步（校验渠道签名、按 transactionId + paymentId 去重、更新支付单与订单状态）。
>

#### 7.2.3 就诊人管理
**（1）GET **`/family-members`** — 查询家庭成员列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| includeInactive | Query | Boolean | 否 | 是否包含已解绑成员，默认 false |


**请求示例：**

```http
GET /api/c/v1/family-members
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "查询成功",
    "data": [
        {
            "patientId": 20001,
            "name": "张三",
            "relation": "SELF",
            "relationName": "本人",
            "gender": "MALE",
            "birthday": "1990-05-20",
            "phone": "138****8000",
            "isDefault": true
        },
        {
            "patientId": 20002,
            "name": "张小明",
            "relation": "CHILD",
            "relationName": "子女",
            "gender": "MALE",
            "birthday": "2018-06-01",
            "phone": null,
            "isDefault": false
        }
    ]
}
```

**（2）POST **`/family-members`** — 新增家庭成员**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |
| name | Body | String | 是 | 姓名 |
| relation | Body | String | 是 | SPOUSE/PARENT/CHILD/OTHER（不允许 SELF） |
| gender | Body | String | 否 | MALE/FEMALE/UNKNOWN |
| birthday | Body | Date | 否 | 出生日期 |
| phone | Body | String | 否 | 联系电话 |
| idCardNo | Body | String | 否 | 身份证号（演示明文存储） |
| emergencyContact | Body | String | 否 | 紧急联系人 |


**请求示例：**

```http
POST /api/c/v1/family-members
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
X-Idempotency-Key: uuid-3456

{
    "name": "张小明",
    "relation": "CHILD",
    "gender": "MALE",
    "birthday": "2018-06-01",
    "phone": "13800138001",
    "idCardNo": "110101201806011234"
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "家庭成员已添加",
    "data": {
        "patientId": 20002,
        "name": "张小明",
        "relation": "CHILD",
        "isDefault": false,
        "createdAt": "2026-07-29T11:00:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| A0506 | 重复绑定或成员数量超限（最多 5 人） |
| A0443 | 不允许添加 SELF 关系 |


> **数量校验**：同一用户下有效（`deleted_at IS NULL`）家庭成员关联记录不超过 5 条；超限返回 `A0506`，message 明确为"家庭成员数量已达上限（最多5人）"。
>

**（3）PUT **`/family-members/{patientId}`** — 更新家庭成员**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| patientId | Path | Long | 是 | 成员 ID |
| name | Body | String | 是 | 姓名 |
| relation | Body | String | 是 | 关系 |
| gender | Body | String | 否 | 性别 |
| birthday | Body | Date | 否 | 出生日期 |
| phone | Body | String | 否 | 联系电话 |


**请求示例：**

```http
PUT /api/c/v1/family-members/20002
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
    "name": "张小明",
    "relation": "CHILD",
    "phone": "13800138002"
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "家庭成员已更新",
    "data": {
        "patientId": 20002,
        "name": "张小明",
        "relation": "CHILD",
        "updatedAt": "2026-07-29T11:05:00+08:00"
    }
}
```

**（4）DELETE **`/family-members/{patientId}`** — 解绑家庭成员**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| patientId | Path | Long | 是 | 成员 ID |


**请求示例：**

```http
DELETE /api/c/v1/family-members/20002
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "家庭成员已解绑，历史医疗记录已保留",
    "data": {
        "patientId": 20002,
        "unbound": true,
        "unboundAt": "2026-07-29T11:10:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| A0443 | 尝试解绑本人（SELF） |


#### 7.2.4 导诊评估
**（1）POST **`/api/c/v1/triage/assessments`** — 提交症状导诊**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID，默认本人 |
| hospitalId | Body | Long | 是 | 医院ID |
| symptom | Body | String | 是 | 主要症状描述 |
| duration | Body | String | 否 | 持续时间 |
| temperature | Body | Double | 否 | 体温（℃） |
| medicalHistory | Body | String | 否 | 既往史 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {
        "assessmentId": 8001,
        "urgency": "MEDIUM",
        "recommendedDepartments": [
            {"id": 1, "name": "呼吸内科", "reason": "症状与呼吸道相关"}
        ],
        "disclaimer": "AI建议仅供参考，不替代医生诊断"
    },
    "traceId": "01J7X-TRIAGE-001"
}
```

#### 7.2.5 在线问诊
**（1）POST **`/api/c/v1/consultations/pre-consultations`** — 创建或保存预问诊**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID |
| appointmentId | Body | Long | 是 | 挂号订单ID |
| chiefComplaint | Body | String | 是 | 主诉 |
| historyOfPresentIllness | Body | String | 否 | 现病史 |
| attachments | Body | Array | 否 | 附件 |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"consultationId": 5001, "status": "PRE_CONSULTATION", "savedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-CONSULT-001"
}
```

**（2）GET **`/api/c/v1/consultations`** — 查询问诊记录列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| status | Query | String | 否 | 状态过滤 |
| pageNo | Query | Integer | 否 | 默认1 |
| pageSize | Query | Integer | 否 | 默认20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"total": 1, "records": [{"id": 5001, "appointmentId": 7001, "doctorName": "王医生", "status": "PENDING", "updatedAt": "2026-07-30T10:00:00+08:00"}]},
    "traceId": "01J7X-CONSULT-002"
}
```

**（3）GET **`/api/c/v1/consultations/{consultationId}`** — 查询问诊详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| consultationId | Path | Long | 是 | 问诊记录ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 5001, "status": "IN_PROGRESS", "doctor": {"id": 101, "name": "王医生"}, "preConsultation": {"chiefComplaint": "咳嗽3天"}, "messages": [{"id": 1, "senderType": "PATIENT", "content": "您好", "createdAt": "2026-07-30T10:05:00+08:00"}]},
    "traceId": "01J7X-CONSULT-003"
}
```

**（4）POST **`/api/c/v1/consultations/{consultationId}/messages`** — 发送问诊消息**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| consultationId | Path | Long | 是 | 问诊记录ID |
| content | Body | String | 是 | 消息内容 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"messageId": 1, "consultationId": 5001, "senderType": "PATIENT", "content": "您好", "createdAt": "2026-07-30T10:05:00+08:00"},
    "traceId": "01J7X-CONSULT-004"
}
```

#### 7.2.6 处方与解读
**（1）GET **`/api/c/v1/prescriptions`** — 查询处方列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| pageNo | Query | Integer | 否 | 默认1 |
| pageSize | Query | Integer | 否 | 默认20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"total": 1, "records": [{"id": 6001, "consultationId": 5001, "doctorName": "王医生", "status": "APPROVED", "issuedAt": "2026-07-30T10:30:00+08:00"}]},
    "traceId": "01J7X-PRESC-001"
}
```

**（2）GET **`/api/c/v1/prescriptions/{prescriptionId}`** — 查询处方详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| prescriptionId | Path | Long | 是 | 处方ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 6001, "status": "APPROVED", "doctorName": "王医生", "items": [{"drugId": 201, "drugName": "阿莫西林胶囊", "specification": "0.25g×24粒", "dosage": "0.5g", "frequency": "TID", "usage": "口服", "durationDays": 7}]},
    "traceId": "01J7X-PRESC-002"
}
```

**（3）GET **`/api/c/v1/prescriptions/{prescriptionId}/interpretation`** — 查询处方解读**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| prescriptionId | Path | Long | 是 | 处方ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"prescriptionId": 6001, "content": "阿莫西林为青霉素类抗生素，用于细菌感染...", "disclaimer": "AI建议仅供参考，不替代医生诊断", "generatedAt": "2026-07-30T10:35:00+08:00"},
    "traceId": "01J7X-PRESC-003"
}
```

#### 7.2.7 购药模块
**（1）GET **`/api/c/v1/pharmacies/inventory`** — 查询院内药房库存**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| prescriptionId | Query | Long | 是 | 处方ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {
        "pharmacyId": 301,
        "name": "本院药房",
        "hospitalId": 1,
        "isDefault": true,
        "deliveryMethod": "COURIER",
        "estimatedDeliveryDays": 3,
        "distanceKm": null,
        "items": [
            {"drugId": 201, "availableCount": 100, "unitPriceCent": 1500}
        ]
    },
    "traceId": "01J7X-PHARM-001"
}
```

**（2）POST **`/api/c/v1/drug-orders`** — 创建购药订单**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID |
| prescriptionId | Body | Long | 是 | 处方ID |
| pharmacyId | Body | Long | 是 | 药房ID |
| deliveryAddress | Body | String | 是 | 配送地址 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"drugOrderId": 9001, "status": "PENDING_PAYMENT", "deliveryMethod": "COURIER", "amountCent": 15000, "expireAt": "2026-07-30T10:15:00+08:00", "paymentId": 7002, "items": [{"drugId": 201, "quantity": 2, "unitPriceCent": 1500}]},
    "traceId": "01J7X-DRUG-001"
}
```

**（3）GET **`/api/c/v1/drug-orders`** — 查询购药订单列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| status | Query | String | 否 | 订单状态 |
| logisticsStatus | Query | String | 否 | 物流状态(PENDING_SHIPMENT/SHIPPED/IN_TRANSIT/TO_RECEIVE/RECEIVED) |
| pageNo | Query | Integer | 否 | 默认1 |
| pageSize | Query | Integer | 否 | 默认20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"total": 1, "records": [{"id": 9001, "pharmacyName": "本院药房", "status": "PAID", "logisticsStatus": "IN_TRANSIT", "latestLogisticsNode": "已到达分拣中心", "amountCent": 15000, "expireAt": "2026-07-30T10:15:00+08:00"}]},
    "traceId": "01J7X-DRUG-002"
}
```

**（4）GET **`/api/c/v1/drug-orders/{drugOrderId}`** — 查询购药订单详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| drugOrderId | Path | Long | 是 | 购药订单ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 9001, "status": "PAID", "pharmacy": {"id": 301, "name": "本院药房"}, "delivery": {"method": "COURIER", "address": "xx路xx号", "company": "顺丰", "trackingNo": "SF1234567890", "logisticsStatus": "IN_TRANSIT", "traces": [{"time": "2026-07-30T11:00:00+08:00", "desc": "已发货"}]}, "items": [{"drugId": 201, "drugName": "阿莫西林胶囊", "quantity": 2, "unitPriceCent": 1500}], "amountCent": 15000, "payment": {"id": 7002, "status": "SUCCESS"}},
    "traceId": "01J7X-DRUG-003"
}
```

**（5）POST **`/api/c/v1/drug-orders/{drugOrderId}/cancel`** — 取消购药订单**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| drugOrderId | Path | Long | 是 | 购药订单ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"drugOrderId": 9001, "status": "CANCELLED", "cancelledAt": "2026-07-30T10:10:00+08:00"},
    "traceId": "01J7X-DRUG-004"
}
```

**（6）POST **`/api/c/v1/drug-orders/{drugOrderId}/confirm-receipt`** — 确认收货**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| drugOrderId | Path | Long | 是 | 购药订单ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"drugOrderId": 9001, "logisticsStatus": "RECEIVED", "receivedAt": "2026-07-31T14:00:00+08:00"},
    "traceId": "01J7X-DRUG-005"
}
```

#### 7.2.8 健康管理
**（1）GET **`/api/c/v1/health-record`** — 查询健康档案**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"profile": {"name": "张三", "gender": "MALE", "birthday": "1990-01-01"}, "allergies": [{"id": 1, "allergen": "青霉素", "reaction": "皮疹"}], "medicalHistories": [{"id": 1, "content": "高血压3年", "occurredAt": "2022-01-01"}], "summary": "高血压患者，青霉素过敏"},
    "traceId": "01J7X-HEALTH-001"
}
```

**（2）POST **`/api/c/v1/health-record/allergies`** — 新增过敏史**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID |
| allergen | Body | String | 是 | 过敏原 |
| reaction | Body | String | 否 | 反应描述 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 2, "allergen": "阿司匹林", "reaction": "哮喘"},
    "traceId": "01J7X-HEALTH-002"
}
```

**（3）PUT **`/api/c/v1/health-record/allergies/{allergyId}`** — 更新过敏史**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| allergyId | Path | Long | 是 | 过敏史ID |
| allergen | Body | String | 是 | 过敏原 |
| reaction | Body | String | 否 | 反应描述 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 2, "allergen": "阿司匹林", "reaction": "哮喘加重", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-HEALTH-003"
}
```

**（4）POST **`/api/c/v1/health-record/histories`** — 新增既往史**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID |
| content | Body | String | 是 | 病史内容 |
| occurredAt | Body | String | 否 | 发生日期 |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 2, "content": "糖尿病2年", "occurredAt": "2024-01-01"},
    "traceId": "01J7X-HEALTH-004"
}
```

**（5）PUT **`/api/c/v1/health-record/histories/{historyId}`** — 更新既往史**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| historyId | Path | Long | 是 | 既往史ID |
| content | Body | String | 是 | 病史内容 |
| occurredAt | Body | String | 否 | 发生日期 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 2, "content": "糖尿病2年", "occurredAt": "2024-01-01", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-HEALTH-005"
}
```

#### 7.2.9 检查报告
**（1）POST **`/api/c/v1/reports`** — 录入检查报告**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Body | Long | 否 | 就诊人ID |
| reportName | Body | String | 是 | 报告名称 |
| reportDate | Body | String | 是 | 报告日期 |
| indicators | Body | Array | 是 | 指标列表，每项含name/value/unit/referenceRange |
| X-Idempotency-Key | Header | String | 是 | 幂等键 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"reportId": 10001, "status": "RECORDED"},
    "traceId": "01J7X-REPORT-001"
}
```

**（2）GET **`/api/c/v1/reports`** — 查询报告列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| pageNo | Query | Integer | 否 | 默认1 |
| pageSize | Query | Integer | 否 | 默认20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"total": 1, "records": [{"id": 10001, "reportName": "血常规", "reportDate": "2026-07-30", "indicatorCount": 5}]},
    "traceId": "01J7X-REPORT-002"
}
```

**（3）GET **`/api/c/v1/reports/{reportId}`** — 查询报告详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| reportId | Path | Long | 是 | 报告ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 10001, "reportName": "血常规", "reportDate": "2026-07-30", "indicators": [{"name": "白细胞", "value": "6.5", "unit": "10^9/L", "referenceRange": "4.0-10.0"}]},
    "traceId": "01J7X-REPORT-003"
}
```

**（4）GET **`/api/c/v1/reports/{reportId}/interpretation`** — 查询报告解读**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| reportId | Path | Long | 是 | 报告ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"reportId": 10001, "indicatorExplanations": [{"name": "白细胞", "value": "6.5", "status": "正常", "explanation": "白细胞计数在正常范围内"}], "suggestedDepartmentId": null, "disclaimer": "AI建议仅供参考，不替代医生诊断"},
    "traceId": "01J7X-REPORT-004"
}
```

#### 7.2.10 用药与随访
**（1）GET **`/api/c/v1/medication-plans`** — 查询用药计划**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| status | Query | String | 否 | 状态(ACTIVE/PAUSED/COMPLETED) |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": [{"id": 1, "drugName": "阿莫西林胶囊", "dosage": "0.5g", "frequency": "TID", "nextReminderAt": "2026-07-31T08:00:00+08:00", "status": "ACTIVE"}],
    "traceId": "01J7X-MED-001"
}
```

**（2）PATCH **`/api/c/v1/medication-plans/{planId}`** — 更新用药计划**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| planId | Path | Long | 是 | 用药计划ID |
| action | Body | String | 是 | 动作: PAUSE / RESUME / COMPLETE |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 1, "status": "PAUSED", "nextReminderAt": null},
    "traceId": "01J7X-MED-002"
}
```

**（3）GET **`/api/c/v1/follow-ups`** — 查询随访计划**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| status | Query | String | 否 | 状态(PENDING_CONFIRM/CONFIRMED/COMPLETED/CANCELLED) |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": [{"id": 1, "type": "复诊提醒", "dueAt": "2026-08-05T10:00:00+08:00", "content": "请于一周后复诊", "status": "PENDING_CONFIRM"}],
    "traceId": "01J7X-FOLLOW-001"
}
```

**（4）POST **`/api/c/v1/follow-ups/{followUpId}/confirm`** — 确认随访计划**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| followUpId | Path | Long | 是 | 随访计划ID |
| remindAt | Body | String | 否 | 提醒时间(ISO-8601) |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 1, "status": "CONFIRMED", "remindAt": "2026-08-04T09:00:00+08:00"},
    "traceId": "01J7X-FOLLOW-002"
}
```

#### 7.2.11 通知
**（1）GET **`/api/c/v1/notifications`** — 查询通知列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| patientId | Query | Long | 否 | 就诊人ID |
| read | Query | Boolean | 否 | 是否已读 |
| pageNo | Query | Integer | 否 | 默认1 |
| pageSize | Query | Integer | 否 | 默认20 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"total": 1, "records": [{"id": 1, "type": "APPOINTMENT_REMINDER", "patientId": 20001, "patientName": "张三", "title": "挂号提醒", "content": "您有一个待支付挂号订单", "read": false, "createdAt": "2026-07-30T10:00:00+08:00"}]},
    "traceId": "01J7X-NOTIF-001"
}
```

**（2）POST **`/api/c/v1/notifications/{notificationId}/read`** — 标记通知已读**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| notificationId | Path | Long | 是 | 通知ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 1, "read": true, "readAt": "2026-07-30T10:05:00+08:00"},
    "traceId": "01J7X-NOTIF-002"
}
```

#### 7.2.12 医院查询
**（1）GET **`/api/c/v1/hospitals`** — 查询可用医院**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| — | Header | Authorization | 是 | Bearer JWT |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": [{"hospitalId": 1, "name": "示范市第一人民医院", "level": "三甲", "address": "示范市XX路1号", "contact": "0571-12345678"}],
    "traceId": "01J7X-HOSP-001"
}
```

#### 7.2.13 个人资料
**（1）GET **`/api/c/v1/profile`** — 查询本人资料**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| — | Header | Authorization | 是 | Bearer JWT |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 20001, "name": "张三", "gender": "MALE", "birthday": "1990-01-01", "phone": "138****8000", "emergencyContact": "139****9000"},
    "traceId": "01J7X-PROFILE-001"
}
```

**（2）PUT **`/api/c/v1/profile`** — 更新本人资料**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Body | String | 是 | 姓名 |
| gender | Body | String | 否 | 性别(MALE/FEMALE/UNKNOWN) |
| birthday | Body | String | 否 | 生日 |
| phone | Body | String | 否 | 手机号 |
| emergencyContact | Body | String | 否 | 紧急联系人 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"id": 20001, "name": "张三", "phone": "138****8000", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-PROFILE-002"
}
```

**（3）POST **`/api/c/v1/auth/token/refresh`** — 刷新令牌**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| refreshToken | Body | String | 是 | 刷新令牌 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 3600},
    "traceId": "01J7X-AUTH-005"
}
```

**（4）POST **`/api/c/v1/auth/logout`** — 退出登录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| refreshToken | Body | String | 是 | 刷新令牌 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"loggedOut": true},
    "traceId": "01J7X-AUTH-006"
}
```

**（5）PUT **`/api/c/v1/auth/password`** — 修改密码**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| oldPassword | Body | String | 是 | 旧密码 |
| newPassword | Body | String | 是 | 新密码(8-64位) |


**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {"passwordChanged": true},
    "traceId": "01J7X-AUTH-007"
}
```



### 7.3 B 端 API 设计
#### 7.3.0 B端认证
> **账号体系**：B 端账号基于 `b_user` 表（见 §6.4.3），登录校验 `account + password_hash`（jBCrypt），JWT 独立密钥签发。token 携带 `role / hospitalId / doctorId`：`DEPT_HEAD`、`DOCTOR` 经 `doctor_id` 关联医生并取其所属科室；`ADMIN` 无 `doctor_id`。
>

**（1）POST **`/api/b/auth/login`** — B端登录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| username | Body | String | 是 | 用户名 |
| password | Body | String | 是 | 密码 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzUxMiJ9...",
        "userInfo": {
            "id": 1001,
            "name": "张医生",
            "roles": ["DOCTOR"],
            "deptId": 10,
            "doctorId": 101,
            "hospitalId": 1
        }
    },
    "traceId": "01J7X-BAUTH-001"
}
```

**（2）POST **`/api/b/auth/logout`** — B端退出登录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| — | Header | Authorization | 是 | Bearer JWT |


**响应示例：**

```json
{
    "code": "00000",
    "message": "退出成功",
    "data": {"loggedOut": true},
    "traceId": "01J7X-BAUTH-002"
}
```

**（3）GET **`/api/b/auth/token/parse`** — B端Token解析（供Agent调用）**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| — | Header | Authorization | 是 | Bearer JWT |


**响应示例：**

```json
{
    "code": "00000",
    "message": "令牌解析成功",
    "data": {
        "userId": 2001,
        "account": "doctor_li",
        "roles": ["DOCTOR"],
        "deptId": 10,
        "doctorId": 101,
        "hospitalId": 1,
        "tokenExpiresAt": "2026-07-30T12:00:00+08:00"
    },
    "traceId": "01J7X-BAUTH-003"
}
```

> Agent 调用此接口时携带前端 JWT Bearer Token，获取 userId + roles + deptId + hospitalId 用于后续 MCP 工具调用的数据隔离。
>

**（4）POST **`/api/b/auth/token/refresh`** — 刷新 B 端访问令牌**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| refreshToken | Body | String | 是 | 刷新令牌 |


**请求示例：**

```http
POST /api/b/auth/token/refresh
Content-Type: application/json

{
    "refreshToken": "rt_01J7X...b"
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
        "refreshToken": "rt_01J7X...b",
        "expiresIn": 7200
    },
    "traceId": "01J7X-BAUTH-004"
}
```

> **刷新令牌存储**：B 端刷新令牌基于 `b_refresh_token` 表（见 §6.4.3），结构参考 C 端 `c_refresh_token`，`user_id` 关联 `b_user.id`。
>

#### 7.3.1 医院/科室/医生管理
**（1）GET **`/api/b/admin/hospitals`** — 查询本院信息**

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "id": 1,
        "name": "示范市第一人民医院",
        "level": "三甲",
        "address": "示范市中心路 1 号",
        "contact": "0100-88886666",
        "status": "ENABLED"
    }
}
```

**（2）GET **`/api/b/admin/departments`** — 查询科室列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Query | String | 否 | 模糊检索 |
| status | Query | String | 否 | ENABLED/DISABLED |
| page | Query | Integer | 否 | 默认 1 |
| size | Query | Integer | 否 | 默认 10 |


**（3）POST **`/api/b/admin/departments`** — 新增科室**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Body | String | 是 | 科室名称 |
| headDoctorId | Body | Long | 否 | 科室负责人 |
| description | Body | String | 否 | 简介 |


**（4）GET **`/api/b/admin/doctors`** — 查询医生列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| deptId | Query | Long | 否 | 科室过滤 |
| name | Query | String | 否 | 模糊检索 |
| status | Query | String | 否 | ENABLED/DISABLED/SUSPENDED |


#### 7.3.2 排班与号源
**（1）POST **`/api/b/admin/schedules`** — 创建排班**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| doctorId | Body | Long | 是 | 医生 ID |
| scheduleDate | Body | String(date) | 是 | yyyy-MM-dd |
| shift | Body | String | 是 | MORNING/AFTERNOON |
| totalSlots | Body | Integer | 是 | 号源总数 |


**（2）PUT **`/api/b/admin/schedules/{id}/slots`** — 配置号源时段**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| slotConfigs | Body | Array | 是 | 时段配置数组 |
| ∟ startTime | Body | String(time) | 是 | HH:mm |
| ∟ endTime | Body | String(time) | 是 | HH:mm |
| ∟ count | Body | Integer | 是 | 该时段号源数 |


**（3）PUT **`/api/b/admin/schedules/{id}/publish`** — 发布排班**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | Path | Long | 是 | 待发布的排班 ID |


> **请求体（Body）**：无。发布动作由路径参数触发。
>

**响应示例（成功）：**

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 2001,
    "status": "PUBLISHED",
    "publishedAt": "2026-07-30T10:00:00+08:00"
  }
}
```

---

**（4）GET **`/api/b/admin/slots/locked`** — 锁定号源看板**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| date | Query | String(date) | 是 | yyyy-MM-dd |
| deptId | Query | Long | 否 | 科室过滤 |


**（5）POST **`/api/b/admin/slots/{id}/force-release`** — 手动释放锁定号源**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | Path | Long | 是 | 锁定中的号源快照 ID（slot_snapshot.id） |


> **请求体（Body）**：无。释放动作由路径参数触发，需管理员权限。
>

**响应示例（成功）：**

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "slotId": 3001,
    "status": "RELEASED"
  }
}
```

---

#### 7.3.3 诊疗管理
**（1）GET **`/api/b/doctor/queue`** — 待接诊列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| deptId | Query | Long | 否 | 按科室过滤（数据权限自动限定） |
| status | Query | String | 否 | PENDING/IN_PROGRESS，默认 PENDING |
| page | Query | Integer | 否 | 页码，默认 1 |
| size | Query | Integer | 否 | 每页条数，默认 10 |


**请求示例：**

```http
GET /api/b/doctor/queue?deptId=10&status=PENDING&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "total": 1,
        "list": [
            {
                "consultId": 5001,
                "patientId": 20001,
                "patientName": "张三",
                "patientGender": "MALE",
                "patientAge": 36,
                "aiSummary": {
                    "chiefComplaint": "咳嗽发热三天",
                    "riskLevel": "ATTENTION"
                },
                "queueNumber": 1,
                "appointmentTime": "2026-07-30T09:00:00+08:00",
                "status": "PENDING"
            }
        ]
    }
}
```

**（2）GET **`/api/b/doctor/queue/{id}`** — 患者详情（含 AI 摘要）**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| id | Path | Long | 是 | 问诊记录 ID（consult_record.id） |


**请求示例：**

```http
GET /api/b/doctor/queue/5001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "consultId": 5001,
        "patient": {
            "id": 20001,
            "name": "张三",
            "gender": "MALE",
            "dateOfBirth": "1990-05-20"
        },
        "allergies": [
            { "allergen": "青霉素", "reaction": "皮疹", "severity": "MODERATE" }
        ],
        "medicalHistories": [
            { "content": "高血压病史5年", "occurredAt": "2021-01-01" }
        ],
        "aiSummary": {
            "chiefComplaint": "咳嗽发热三天",
            "symptoms": "干咳，体温38.5℃",
            "duration": "3天",
            "riskLevel": "ATTENTION"
        },
        "recentPrescriptions": [
            { "id": 6001, "status": "APPROVED", "issuedAt": "2026-06-01T10:00:00+08:00" }
        ]
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3010 | 无权查看该患者（非本人接诊） |


**（3）POST **`/api/b/doctor/consult/{id}/start`** — 开始接诊**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| id | Path | Long | 是 | 问诊记录 ID |


> **请求体**：无。
>

**请求示例：**

```http
POST /api/b/doctor/consult/5001/start
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "consultId": 5001,
        "status": "IN_PROGRESS",
        "startedAt": "2026-07-30T09:05:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3011 | 问诊记录不存在或状态不可接诊 |
| 3012 | 医生当前存在未结束的接诊记录 |


**（4）POST **`/api/b/doctor/consult/{id}/end`** — 结束问诊**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| id | Path | Long | 是 | 问诊记录 ID |


> **请求体**：无。
>

**请求示例：**

```http
POST /api/b/doctor/consult/5001/end
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "consultId": 5001,
        "status": "COMPLETED",
        "endedAt": "2026-07-30T09:20:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3013 | 问诊状态不是 IN_PROGRESS |
| 3014 | 存在未签名的处方草稿 |


**（5）PUT **`/api/b/doctor/consult/{id}/note`** — 保存病历记录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| id | Path | Long | 是 | 问诊记录 ID |
| doctorNote | Body | String | 是 | 病历文本，最大 10000 字符 |


**请求示例：**

```http
PUT /api/b/doctor/consult/5001/note
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
    "doctorNote": "患者主诉咳嗽发热三天，体温38.5℃，双肺听诊呼吸音粗，诊断为急性支气管炎。"
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "consultId": 5001,
        "updatedAt": "2026-07-30T09:15:00+08:00"
    }
}
```

#### 7.3.4 处方管理
**（1）POST **`/api/b/prescriptions`** — 提交处方**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| consultId | Body | Long | 是 | 问诊记录 ID |
| items | Body | Array | 是 | 处方明细 |
| ∟ drugId | Body | Long | 是 | 药品 ID |
| ∟ dosage | Body | String | 是 | QD/BID/TID |
| ∟ frequency | Body | String | 是 | 频次描述 |
| ∟ usageMethod | Body | String | 是 | 口服/外用 |
| ∟ days | Body | Integer | 是 | 用药天数 |
| ∟ quantity | Body | Integer | 是 | 数量 |


**响应示例（WARNING 级风险，处方生效）：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "id": 6001,
        "status": "APPROVED",
        "auditRequired": false,
        "riskWarnings": [
            {
                "level": "WARNING",
                "rule": "重复用药检测",
                "message": "阿司匹林与氯吡格雷同属抗血小板药物"
            }
        ]
    }
}
```

**响应示例（命中审核级规则，进入人工审核）：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "id": 6001,
        "status": "SUBMITTED",
        "auditRequired": true,
        "riskWarnings": [
            {
                "level": "AUDIT",
                "rule": "高危药物联用",
                "message": "处方命中审核级规则，需人工审核"
            }
        ]
    }
}
```

**返回结构与前端判定规则：**

| 场景 | status | auditRequired | riskWarnings | 前端行为 |
| --- | --- | --- | --- | --- |
| 无风险 | APPROVED | false | [] | 处方生效 |
| WARNING 级风险（提示） | APPROVED | false | 含 WARNING 条目 | 处方生效 + 弹窗提示 |
| 审核级风险 | SUBMITTED | true | 含审核级条目 | 进入待审核列表 |
| ERROR 红线 | 返回 3004 | — | — | 不入库，提示修改 |


> 前端仅以 `status + auditRequired` 作为 UI 判定依据，`riskWarnings.level` 仅用于展示提示文案。
>

**（2）GET **`/api/b/prescriptions`** — 查询处方列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| consultId | Query | Long | 否 | 按问诊记录过滤 |
| patientId | Query | Long | 否 | 按患者过滤 |
| status | Query | String | 否 | 多值逗号分隔：DRAFT,SUBMITTED,APPROVED |
| page | Query | Integer | 否 | 默认 1 |
| size | Query | Integer | 否 | 默认 10 |


**请求示例：**

```http
GET /api/b/prescriptions?patientId=20001&status=APPROVED&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "total": 1,
        "list": [
            {
                "id": 6001,
                "consultId": 5001,
                "doctorName": "王医生",
                "patientName": "张三",
                "status": "APPROVED",
                "itemCount": 2,
                "issuedAt": "2026-07-30T09:20:00+08:00"
            }
        ]
    }
}
```

**（3）GET **`/api/b/prescriptions/{id}`** — 处方详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token |
| id | Path | Long | 是 | 处方 ID |


**请求示例：**

```http
GET /api/b/prescriptions/6001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "id": 6001,
        "consultId": 5001,
        "doctor": {
            "id": 401,
            "name": "王医生",
            "title": "主任医师",
            "deptName": "呼吸内科"
        },
        "patient": {
            "id": 20001,
            "name": "张三",
            "gender": "MALE",
            "dateOfBirth": "1990-05-20"
        },
        "status": "APPROVED",
        "auditRequired": false,
        "riskWarnings": [],
        "items": [
            {
                "id": 7001,
                "drugId": 13001,
                "drugName": "阿莫西林胶囊",
                "specification": "0.25g*24粒",
                "dosage": "QD",
                "frequency": "每天一次",
                "usageMethod": "口服",
                "days": 5,
                "quantity": 1
            }
        ],
        "issuedAt": "2026-07-30T09:20:00+08:00",
        "auditedAt": "2026-07-30T09:25:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3020 | 无权查看该处方（数据权限隔离） |


**（4）GET **`/api/b/prescriptions/pending-audit`** — 待审核处方列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token（需 ADMIN/DEPT_HEAD 角色） |
| deptId | Query | Long | 否 | 按科室过滤 |
| page | Query | Integer | 否 | 默认 1 |
| size | Query | Integer | 否 | 默认 10 |


**请求示例：**

```http
GET /api/b/prescriptions/pending-audit?deptId=10&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "total": 1,
        "list": [
            {
                "id": 6002,
                "consultId": 5002,
                "doctorName": "李医生",
                "patientName": "赵四",
                "status": "SUBMITTED",
                "riskWarnings": [
                    { "level": "ERROR", "rule": "过敏检测", "message": "患者对青霉素过敏" }
                ],
                "createdAt": "2026-07-30T09:30:00+08:00"
            }
        ]
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3021 | 当前角色无处方审核权限 |


**（5）PUT **`/api/b/prescriptions/{id}/audit`** — 审核处方**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer Token（需审核权限） |
| id | Path | Long | 是 | 处方 ID |
| action | Body | String | 是 | APPROVED / REJECTED |
| rejectReason | Body | String | 否 | 驳回原因（action=REJECTED 时必填） |


**请求示例：**

```http
PUT /api/b/prescriptions/6002/audit
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
    "action": "REJECTED",
    "rejectReason": "患者青霉素过敏，处方含阿莫西林，请修改"
}
```

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "id": 6002,
        "status": "REJECTED",
        "auditedAt": "2026-07-30T09:35:00+08:00"
    }
}
```

| 错误码 | 说明 |
| --- | --- |
| 3022 | 处方状态不是 SUBMITTED |
| 3023 | 驳回时必须填写驳回原因 |


> **审核权限**：当前登录 `b_user.role` 必须为 `ADMIN` 或 `DEPT_HEAD`，否则返回 `3021`；若为 `DEPT_HEAD`，还需校验该处方所属科室（经 `prescription.doctor_id → doctor.dept_id`）与当前用户 `doctor_id` 所在科室一致，不一致返回 `3021`。角色与科室信息均取自 `b_user` 表。
>

#### 7.3.5 药品与库存管理
**（1）GET **`/api/b/admin/drugs`** — 药品目录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Query | String | 否 | 药品名称关键词 |
| status | Query | String | 否 | 状态(ENABLED/DISABLED) |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"id": 201, "name": "阿莫西林胶囊", "specification": "0.25g×24粒", "unit": "盒", "manufacturer": "XX制药", "approvalNumber": "国药准字H12345678", "status": "ENABLED"}]},
    "traceId": "01J7X-DRUG-C-001"
}
```

**（2）POST **`/api/b/admin/drugs`** — 新增药品**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Body | String | 是 | 药品名称 |
| specification | Body | String | 是 | 规格 |
| unit | Body | String | 是 | 单位 |
| manufacturer | Body | String | 否 | 生产商 |
| approvalNumber | Body | String | 是 | 批准文号 |
| indication | Body | String | 否 | 适应症 |
| contraindication | Body | String | 否 | 禁忌 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 202, "name": "布洛芬片", "createdAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-DRUG-C-002"
}
```

**（3）GET **`/api/b/admin/inventory`** — 库存列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| pharmacyId | Query | Long | 否 | 药房ID |
| drugId | Query | Long | 否 | 药品ID |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {
        "total": 1,
        "list": [
            {
                "drugId": 201,
                "drugName": "阿莫西林胶囊",
                "specification": "0.25g×24粒",
                "availableCount": 100,
                "lockedCount": 5,
                "safetyStock": 20,
                "unitPriceCent": 1500,
                "status": "NORMAL",
                "lockedDetails": [
                    { "drugOrderId": 9001, "quantity": 2, "lockedAt": "2026-07-30T10:00:00" },
                    { "drugOrderId": 9002, "quantity": 3, "lockedAt": "2026-07-30T10:05:00" }
                ]
            }
        ]
    },
    "traceId": "01J7X-INV-001"
}
```

**（4）PUT **`/api/b/admin/inventory/{id}`** — 更新库存**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 库存记录ID |
| availableCount | Body | Integer | 否 | 可用数量 |
| safetyStock | Body | Integer | 否 | 安全库存 |
| unitPriceCent | Body | Integer | 否 | 单价（分） |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"drugId": 201, "availableCount": 120, "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-INV-002"
}
```

**（5）GET **`/api/b/admin/inventory/alerts`** — 低库存预警**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| pharmacyId | Query | Long | 否 | 药房ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"drugId": 202, "drugName": "布洛芬片", "availableCount": 5, "safetyStock": 20, "status": "ALERT"}]},
    "traceId": "01J7X-INV-003"
}
```

**（6）POST **`/api/b/admin/inventory/{id}/unlock`** — 手动释放锁定库存**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 库存记录 ID（pharmacy_drug_stock.id） |
| drugOrderId | Body | Long | 是 | 锁定库存的购药订单 ID |
| reason | Body | String | 否 | 释放原因 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "库存释放成功",
    "data": {
        "drugId": 201,
        "availableCount": 102,
        "lockedCount": 3
    },
    "traceId": "01J7X-INV-004"
}
```

#### 7.3.6 患者管理
**（1）GET **`/api/b/admin/patients`** — 患者列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Query | String | 否 | 患者姓名关键词 |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"id": 20001, "name": "张三", "gender": "MALE", "age": 36, "lastVisitDate": "2026-07-30"}]},
    "traceId": "01J7X-PAT-001"
}
```

**（2）GET **`/api/b/admin/patients/{id}`** — 患者详情**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 患者ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 20001, "name": "张三", "gender": "MALE", "dateOfBirth": "1990-01-01", "allergies": [{"allergen": "青霉素", "reaction": "皮疹", "severity": "MILD"}], "medicalHistories": [{"content": "高血压3年", "occurredAt": "2022-01-01"}]},
    "traceId": "01J7X-PAT-002"
}
```

**（3）GET **`/api/b/admin/patients/{id}/visits`** — 患者就诊记录**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 患者ID |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"consultId": 5001, "doctorName": "王医生", "deptName": "呼吸内科", "startTime": "2026-07-30T09:00:00+08:00", "status": "COMPLETED", "aiSummary": {"chiefComplaint": "咳嗽3天", "riskLevel": "LOW"}}]},
    "traceId": "01J7X-PAT-003"
}
```

**（4）GET **`/api/b/admin/patients/{id}/prescriptions`** — 患者历史处方**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 患者ID |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"id": 6001, "doctorName": "王医生", "status": "APPROVED", "issuedAt": "2026-07-30T10:30:00+08:00", "itemCount": 2}]},
    "traceId": "01J7X-PAT-004"
}
```

**（5）GET **`/api/b/admin/patients/{id}/medications`** — 患者当前用药与随访**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 患者ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"medicationPlans": [{"id": 1, "drugName": "阿莫西林胶囊", "status": "ACTIVE", "nextRemindAt": "2026-07-31T08:00:00+08:00"}], "followUps": [{"id": 1, "type": "复诊提醒", "dueAt": "2026-08-05T10:00:00+08:00", "status": "PENDING_CONFIRM"}]},
    "traceId": "01J7X-PAT-005"
}
```

#### 7.3.7 统计报表
**（1）GET **`/api/b/admin/statistics/overview`** — 运营总览**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| startDate | Query | String | 否 | 起始日期(yyyy-MM-dd) |
| endDate | Query | String | 否 | 结束日期(yyyy-MM-dd) |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"totalAppointments": 150, "completedRate": 0.85, "totalRevenue": 7500000, "totalPrescriptions": 80, "avgWaitTime": 12},
    "traceId": "01J7X-STAT-001"
}
```

**（2）GET **`/api/b/admin/statistics/department`** — 按科室统计**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| deptId | Query | Long | 否 | 科室ID |
| startDate | Query | String | 否 | 起始日期 |
| endDate | Query | String | 否 | 结束日期 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": [{"deptId": 1, "deptName": "呼吸内科", "appointmentCount": 50, "completedCount": 43, "revenueCent": 2500000}],
    "traceId": "01J7X-STAT-002"
}
```

**（3）GET **`/api/b/admin/statistics/daily`** — 按日期统计**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| startDate | Query | String | 是 | 起始日期 |
| endDate | Query | String | 是 | 结束日期 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": [{"date": "2026-07-30", "appointmentCount": 30, "completedCount": 25, "revenueCent": 1500000}],
    "traceId": "01J7X-STAT-003"
}
```

#### 7.3.8 处方模板
> `prescription_template` 表包含 `dept_id` 字段（关联 `department.id`）：保存模板时记录所属科室，查询模板列表时支持按当前医生科室过滤（`dept_id` 为空视为全院通用模板）。
>

**（1）GET **`/api/b/prescription-templates`** — 处方模板列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Query | String | 否 | 模板名称关键词 |
| deptId | Query | Long | 否 | 按科室过滤（未传返回全部可用模板） |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"id": 1, "name": "上呼吸道感染常规处方", "itemCount": 3, "updatedAt": "2026-07-30T10:00:00+08:00"}]},
    "traceId": "01J7X-TMPL-001"
}
```

**（2）POST **`/api/b/prescription-templates`** — 保存处方模板**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| name | Body | String | 是 | 模板名称 |
| deptId | Body | Long | 否 | 所属科室（空表示全院通用模板） |
| items | Body | Array | 是 | 药品列表，每项含drugId/dosage/frequency/usageMethod/days/quantity |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 2, "name": "高血压常规处方", "createdAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-TMPL-002"
}
```

#### 7.3.9 排班扩展
**（1）GET **`/api/b/admin/schedules`** — 查询排班列表**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| doctorId | Query | Long | 否 | 医生ID |
| deptId | Query | Long | 否 | 科室ID |
| date | Query | String | 否 | 日期(yyyy-MM-dd) |
| status | Query | String | 否 | 状态(DRAFT/PUBLISHED/CANCELLED) |
| page | Query | Integer | 否 | 默认1 |
| size | Query | Integer | 否 | 默认10 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"total": 1, "list": [{"id": 301, "doctorName": "王医生", "deptName": "呼吸内科", "scheduleDate": "2026-08-01", "shift": "MORNING", "totalSlots": 20, "bookedCount": 5, "remainCount": 15, "lockedCount": 2, "status": "PUBLISHED"}]},
    "traceId": "01J7X-SCHED-001"
}
```

**（2）GET **`/api/b/admin/schedules/{id}/slots`** — 查询号源时段配置**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 排班ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": [{"id": 401, "startTime": "08:00", "endTime": "08:30", "totalCount": 5, "remainCount": 3}],
    "traceId": "01J7X-SCHED-002"
}
```

**（3）PUT **`/api/b/admin/schedules/{id}/unpublish`** — 取消发布排班**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 排班ID |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 301, "status": "CANCELLED", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-SCHED-003"
}
```

#### 7.3.10 科室与医生管理扩展
**（1）PUT **`/api/b/admin/hospitals/{id}`** — 编辑医院信息**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 医院ID |
| name | Body | String | 否 | 医院名称 |
| level | Body | String | 否 | 等级 |
| description | Body | String | 否 | 描述 |
| address | Body | String | 否 | 地址 |
| contact | Body | String | 否 | 联系方式 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 1, "name": "示范市第一人民医院", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-ADMIN-001"
}
```

**（2）PUT **`/api/b/admin/departments/{id}`** — 编辑科室**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 科室ID |
| name | Body | String | 否 | 科室名称 |
| headDoctorId | Body | Long | 否 | 科室主任ID |
| description | Body | String | 否 | 描述 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 1, "name": "呼吸内科", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-ADMIN-002"
}
```

**（3）PUT **`/api/b/admin/departments/{id}/status`** — 启用/停用科室**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 科室ID |
| status | Body | String | 是 | 状态(ENABLED/DISABLED) |


> **停用前置校验**（status=DISABLED 时）：科室下无 `ENABLED` 医生（否则返回 `4001`）、无 `PUBLISHED` 排班（否则返回 `4002`）、无 `IN_PROGRESS` 问诊（否则返回 `4003`）。
>

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 1, "status": "DISABLED", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-ADMIN-003"
}
```

**（4）PUT **`/api/b/admin/doctors/{id}`** — 编辑医生**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 医生ID |
| name | Body | String | 否 | 姓名 |
| title | Body | String | 否 | 职称 |
| specialty | Body | String | 否 | 专长 |
| introduction | Body | String | 否 | 简介 |


**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 101, "name": "王医生", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-ADMIN-004"
}
```

**（5）PUT **`/api/b/admin/doctors/{id}/status`** — 启用/停用/暂停医生**

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| id | Path | Long | 是 | 医生ID |
| status | Body | String | 是 | 状态(ENABLED/DISABLED/SUSPENDED) |


> **停用前置校验**（status=DISABLED/SUSPENDED 时）：医生无 `PUBLISHED` 排班、无 `IN_PROGRESS` 问诊；存在关联 `b_user` 时建议同步停用（或由管理员决定）。
>

**响应示例：**

```json
{
    "code": "00000",
    "message": "success",
    "data": {"id": 101, "status": "SUSPENDED", "updatedAt": "2026-07-30T10:00:00+08:00"},
    "traceId": "01J7X-ADMIN-005"
}
```



### 7.4 Agent 对话接口
#### 7.4.1 发起对话
**POST **`/api/chat/stream` — SSE 流式对话

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | Header | String | 是 | Bearer JWT |
| content | Body | String | 是 | 用户输入，1-2000 字 |
| scope | Body | String | 是 | c_end / b_end |
| session_id | Body | String | 否 | 会话 ID |
| context | Body | Object | 否 | 附加上下文 |


**SSE 事件：**

```plain
event: message
data: {"delta": "您好，我是智愈先锋"}

event: card
data: {"card_type": "confirm_appointment", "confirm_token": "tok_xxx", "session_id": "sess_abc123", "title": "确认挂号", "expires_at": "2026-07-30T10:15:00+08:00"}

event: done
data: {"session_id": "sess_abc123"}
```

**card 事件参数说明：**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| card_type | string | 卡片类型，如 `confirm_appointment` |
| confirm_token | string | 确认令牌，与 session_id 一并用于确认接口 |
| session_id | string | 当前会话 ID，与 confirm_token 一并用于确认接口 |
| title | string | 卡片标题 |
| expires_at | string | 确认卡片有效期（ISO-8601），过期后需重新发起操作 |


#### 7.4.2 L2 确认回调
**POST **`/api/chat/confirm`

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| confirm_token | Body | String | 是 | 确认令牌 |
| session_id | Body | String | 是 | 会话 ID |


**请求示例：**

```http
POST /api/chat/confirm
Content-Type: application/json

{
    "confirm_token": "tok_xxx",
    "session_id": "sess_abc123"
}
```

**响应示例（成功）：**

```json
{
    "code": "00000",
    "message": "操作成功",
    "data": {
        "action_result": {
            "appointment_id": 7001,
            "status": "UNPAID"
        },
        "message": "挂号成功，请15分钟内完成支付"
    },
    "traceId": "01J7X-CONFIRM-001"
}
```

**响应示例（失败）：**

```json
{
    "code": "CONFIRM_EXPIRED",
    "message": "确认令牌已过期，请重新发起操作",
    "traceId": "01J7X-CONFIRM-002"
}
```

**错误码：**

| 错误码 | 说明 |
| --- | --- |
| CONFIRM_INVALID | 令牌无效 |
| CONFIRM_CONSUMED | 令牌已被使用 |
| CONFIRM_EXPIRED | 令牌已过期 |
| SESSION_MISMATCH | 会话 ID 不匹配 |


## 8. 关键技术设计
### 8.1 防超卖机制
#### 8.1.1 号源防超卖
```mermaid
flowchart LR
    A["用户发起挂号"] --> B["Redis Lua 脚本<br/>DECR + 校验"]
    B --> C{"remain >= 0?"}
    C -->|是| D["事务创建订单"]
    C -->|否| E["返回失败"]
    D --> F["异步写 DB"]
    D --> G["15min 延迟队列"]
```

**核心实现：**

```lua
-- Redis Lua 原子扣减
local remain = redis.call('DECR', KEYS[1])
if remain < 0 then
    redis.call('INCR', KEYS[1])  -- 回滚
    return 0
end
return remain
```

**超时释放：**

+ 订单创建后发送延迟消息（15min TTL）
+ 消费者检查订单状态，未支付则释放号源

#### 8.1.2 药品库存防超卖
+ 使用 Redis 原子扣减 + 预留库存
+ 订单取消/超时释放预留库存

### 8.2 幂等设计
| 场景 | 方案 |
| --- | --- |
| 创建挂号订单 | `X-Idempotency-Key` + Redis 存 24h 结果 |
| 取消订单 | `X-Idempotency-Key` + 状态机条件更新 |
| 支付回调 | 外部渠道 transactionId + paymentId 去重 |
| 重复提交 | 业务级唯一键（如 user_id + slot_id + date） |


### 8.3 事务一致性
| 场景 | 方案 |
| --- | --- |
| 挂号锁号 | Redis 扣减成功 → DB 事务创建订单；事务失败 → Redis 补偿回滚 |
| 支付成功 | 事务更新支付单 + 订单 + 号源状态；失败记录补偿日志 |
| 超时释放 | 消费者查询订单状态，条件更新（CAS） |


### 8.4 数据权限（多医院隔离，基于 b_user）
数据权限基于当前登录 B 端 `b_user` 的 `role / hospital_id / doctor_id` 动态拼接 SQL（在 MyBatis 拦截器或 AOP 中实现）：

+ `ADMIN`：仅按 `hospital_id` 过滤（本院全部数据），不依赖 `doctor_id`；
+ `DEPT_HEAD`：经 `doctor_id` 查医生所属科室，按 `hospital_id + dept_id` 过滤（本科室数据）；
+ `DOCTOR`：按 `hospital_id + doctor_id` 过滤（仅本人数据）。

```java
// 数据权限拦截器核心逻辑
public class SphpDataPermissionInterceptor {
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, ...) {
        LoginUser user = getUser();          // 当前登录 B 端 b_user
        String originalSql = boundSql.getSql();
        String permissionSql = buildPermissionSql(originalSql, user);
        // 替换 SQL
    }
    
    private String buildPermissionSql(String sql, LoginUser user) {
        String alias = parseTableAlias(sql);
        switch (user.getRole()) {
            case "ADMIN":
                // ADMIN 不依赖 doctor_id
                return sql + " AND " + alias + ".hospital_id = " + user.getHospitalId();
            case "DEPT_HEAD":
                Long doctorId = user.getDoctorId();
                if (doctorId == null) {
                    throw new BusinessException("科室负责人账号未关联医生信息");
                }
                Long deptId = doctorMapper.selectById(doctorId).getDeptId();
                if (deptId == null) {
                    throw new BusinessException("关联医生未配置所属科室");
                }
                return sql + " AND " + alias + ".hospital_id = " + user.getHospitalId()
                    + " AND " + alias + ".dept_id = " + deptId;
            case "DOCTOR":
                if (user.getDoctorId() == null) {
                    throw new BusinessException("医生账号未关联医生信息");
                }
                return sql + " AND " + alias + ".hospital_id = " + user.getHospitalId()
                    + " AND " + alias + ".doctor_id = " + user.getDoctorId();
            default:
                return sql;
        }
    }
}
```

### 8.5 安全设计
| 层级 | 机制 |
| --- | --- |
| 密码存储 | BCrypt 哈希（jBCrypt） |
| JWT 鉴权 | C/B 端独立密钥，Agent 调 Java 解析 |
| B 端账号 | b_user 表（ADMIN/DEPT_HEAD/DOCTOR）驱动 RBAC 与数据权限 |
| 通信安全 | 生产环境强制 HTTPS |
| 数据隔离 | hospital_id + user_id 双维度隔离 |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| 敏感字段 | 手机号/身份证响应脱敏（`138****8000`） |
| Agent 红线 | L3 资金操作（支付/退款）与 L4 禁止操作（诊断/开方/删病历）不注册工具，Agent 无法调用，前端仅跳转支付页 |


### 8.6 RabbitMQ 异步任务
| 队列 | 用途 |
| --- | --- |
| `cend.appointment.delay.queue` | 挂号超时释放（15min） |
| `cend.drug-order.delay.queue` | 购药超时释放（15min） |
| `cend.notification.queue` | 站内通知创建 |
| `cend.reminder.queue` | 用药提醒推送 |
| `cend.waitlist.notify.queue` | 候补通知：号源释放后创建通知并推送 |


候补通知逻辑：号源超时/取消释放（§4.2）后，异步任务（RabbitMQ 消费者）按队列顺序将 `appointment_waitlist` 状态置为 `NOTIFIED`，创建 `notification` 记录，并通过 SSE/推送接口通知用户。



### 8.7 Agent MCP 协议
Agent 通过 MCP（Model Context Protocol）调用 Java REST API：

```plain
Agent 编排层（MCP Client）
    │ tools/list → 获取工具 Schema
    │ tools/call → 执行工具
    ▼
MCP Server（Python 内嵌）
    │ HTTP REST → Java :8080
    ▼
Java 后端
```

**工具注册示例（Function Calling Schema）：**

```python
{
    "name": "create_appointment",
    "description": "创建挂号锁定订单",
    "parameters": {
        "type": "object",
        "properties": {
            "slot_id": {"type": "integer", "description": "号源时段ID"},
            "patient_id": {"type": "integer", "description": "就诊人ID"}
        },
        "required": ["slot_id"]
    },
    "security_level": "L2"
}
```



## 9. 测试用例与异常场景
### 9.1 验收用例（覆盖正常+异常）
| 场景 | 预期结果 |
| --- | --- |
| 验证码错误/过期/重复 | 注册失败，验证码不可重复验证 |
| C/B Token 混用 | 对方鉴权拒绝，返回未认证 |
| CJwtInterceptor | 有效 C 端 JWT 写上下文；请求结束清理 ThreadLocal |
| JWT 白名单与回调 | 验证码/注册/登录/刷新免 JWT；回调不走用户 JWT，校验渠道签名 |
| 密码与敏感字段 | 登录/改密/支付用 jBCrypt；响应脱敏 |
| token/parse 有效/无效 | 有效返回 userId/account/expiresAt；无效/过期/B 端 Token 返回 401+A0301 |
| 医院链路筛选 | 缺/错 hospitalId 返回 A0301/A0402 |
| 两人并发抢最后一号 | 仅一人 LOCKED+UNPAID，另一人 B0201 |
| 重复提交创建/支付 | 幂等键返回首次结果，不重复扣库存/支付 |
| 15 分钟未支付 | 挂号 CANCELLED+RELEASED；购药 EXPIRED+释放库存；支付单关闭 |
| 密码错/非本人/已关闭支付 | 支付失败，订单与库存不变 |
| 重复支付回调 | 仅首次改状态，后续 ACK 不重复执行 |
| 非本人访问档案/处方/报告/订单 | 返回 A0301 或不存在，不泄露 |
| 报告/处方解读 | 仅说明+免责声明，不确诊不改处方 |
| 注册本人就诊人 | 事务建 patient+SELF+is_default=true |
| 新增家庭成员 | ≤5 非 SELF，不建 c_user；重复/超限 A0506 |
| 默认就诊人兼容 | 未传 patientId 用本人；传成员按该成员 |
| 家庭成员数据隔离 | 越权 A0301 |
| 家庭成员解绑 | 软删除，历史记录保留，不可新建业务 |
| 本人档案保护 | SELF 不可经家庭成员接口编辑/解绑，A0443 |
| 院内药房推荐 | 按处方医生医院，默认药房优先，不接收经纬度 |
| 处方 ERROR 红线 | 3004 不入库 |
| 数据隔离（B 端） | 医生 A 看不到医院 B 数据；管理员限本院 |
| Agent 真实调 API | 对话中真实调查询/创建接口，非纯文本 |
| Agent L2 确认 | 推卡片→确认→一次性消费执行；超时/跨会话/越权拒绝 |
| Agent L3/L4 拦截 | L3/L4 工具不在工具清单注册，Agent 不产生 tool_call |
| RAG 降级 | pgvector 不可用跳过检索并标注 |
| Redis 不可用 | L2 操作拒绝；限流降级放行 |


### 9.2 异常场景与降级
| 故障 | 检测 | 降级 | 用户感知 |
| --- | --- | --- | --- |
| LLM 超时/5xx/429 | httpx 异常/状态码 | 重试/切备用供应商 | 繁忙提示或自愈 |
| LLM 非 JSON | JSONDecodeError | 丢弃 tool_call 注入原文自愈 | 无感知 |
| Java 超时/5xx | httpx 异常 | 不重试降级 | 系统繁忙+trace_id |
| Java 4xx 业务异常 | HTTP 4xx | 透传 LLM 转自然语言 | 如"该时段已约满，推荐…" |
| token/parse 失败 | 4xx/5xx/超时 | fatal 返回 401 | 登录过期请重登 |
| pgvector 不可用 | 连接异常 | 跳过 RAG | 正常回复+标注 |
| Redis 不可用(L2) | 连接异常 | 拒绝所有 L2 | 操作暂不可用 |
| Redis 不可用(限流/缓存) | 连接异常 | 放行/回退内存 | 无感知 |


外部依赖故障不致进程崩溃（全 try/except）；错误写 structlog 含 trace_id，面向用户脱敏；降级经 error SSE 实时告知前端。

## 9. 排期计划
### 9.1 里程碑
| 阶段 | 内容 | 日期 |
| --- | --- | --- |
| 需求评审 | 产品需求评审 | 07/28 |
| 技术方案 | 系分编写与评审 | 07/29-07/30 |
| 数据库设计 | DB DDL 定稿 + 建表 | 07/31-08/01 |
| 后端 API 开发 | C/B 端 API 实现 | 08/03-08/08 |
| Agent 开发 | 工具调用 + 对话链路 | 08/03-08/09 |
| 联调测试 | 前后端 + Agent 全链路 | 08/09-08/12 |
| 验收交付 | MVP 演示 | 08/13 |


### 9.2 甘特图
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/__mermaid_v3/166aaf81c96276a69699e7e75b2823dc.svg)

## 附录：Agent 工具清单
### C 端工具（30 个）
| 分组 | 工具名 | 等级 |
| --- | --- | --- |
| 导诊 | create_triage_assessment | L1 |
| 挂号 | query_departments, query_doctors, query_schedule_slots, create_appointment, query_appointments, cancel_appointment, join_waitlist, query_payment_status | L1/L2 |
| 问诊 | save_pre_consultation, query_consultations, send_consultation_message | L1/L2 |
| 处方 | query_prescriptions, interpret_prescription | L1 |
| 购药 | query_pharmacy_stock, create_drug_order, query_drug_orders, cancel_drug_order, confirm_drug_receipt | L1/L2 |
| 健康 | query_health_record, manage_allergy, manage_medical_history, query_reports, create_report, query_medication_plans, update_medication_plan, query_follow_ups, confirm_follow_up, manage_notifications | L1/L2 |
| 本地 | search_medical_knowledge | L1 |


### B 端工具（9 个）
| 分组 | 工具名 | 等级 |
| --- | --- | --- |
| 接诊辅助 | query_patient_history | L2 |
| 接诊辅助 | query_drug_guide, check_drug_interaction, generate_draft_note | L1/L2 |
| 导诊推荐 | recommend_care | L1 |
| 处方审核 | check_contraindication, check_allergy_risk, check_duplicate_medication | L1 |
| 报告解读 | interpret_report（本地） | L1 |


### 工具 Schema 示例
> 每个分组提供一个完整示例，同组其余工具按相同结构扩展（`name` / `description` / `parameters` / `java_endpoint` / `security_level`）。L3/L4 不注册。
>

#### C 端（节选分组示例）
```json
{
    "name": "create_triage_assessment",
    "description": "提交症状导诊评估",
    "parameters": {
        "type": "object",
        "properties": {
            "symptom": {"type": "string", "description": "主要症状描述"},
            "duration": {"type": "string", "description": "持续时间"},
            "hospital_id": {"type": "integer", "description": "医院ID"}
        },
        "required": ["symptom", "hospital_id"]
    },
    "java_endpoint": "POST /api/c/v1/triage/assessments",
    "security_level": "L1"
}
```

```json
{
    "name": "create_appointment",
    "description": "创建挂号锁定订单",
    "parameters": {
        "type": "object",
        "properties": {
            "slot_id": {"type": "integer", "description": "号源时段ID"},
            "patient_id": {"type": "integer", "description": "就诊人ID"}
        },
        "required": ["slot_id"]
    },
    "java_endpoint": "POST /api/c/v1/appointments",
    "security_level": "L2"
}
```

挂号分组其余工具参数速览：

+ `query_departments`（L1）：`hospital_id` 必填 → `GET /api/c/v1/departments`
+ `query_doctors`（L1）：`hospital_id`、`department_id`、`date` → `GET /api/c/v1/doctors`
+ `query_schedule_slots`（L1）：`doctor_id`、`hospital_id`、`date` → `GET /api/c/v1/doctors/{doctorId}/slots`
+ `cancel_appointment`（L2）：`appointment_id` → `POST /api/c/v1/appointments/{id}/cancel`
+ `join_waitlist`（L2）：`slot_id`、`patient_id` → `POST /api/c/v1/waitlists`
+ `query_payment_status`（L1）：`payment_id` → `GET /api/c/v1/payments/{id}`

```json
{
    "name": "save_pre_consultation",
    "description": "创建或保存预问诊",
    "parameters": {
        "type": "object",
        "properties": {
            "appointment_id": {"type": "integer", "description": "挂号订单ID"},
            "chief_complaint": {"type": "string", "description": "主诉"}
        },
        "required": ["appointment_id", "chief_complaint"]
    },
    "java_endpoint": "POST /api/c/v1/consultations/pre-consultations",
    "security_level": "L2"
}
```

问诊分组其余工具：`query_consultations`（L1）`GET /api/c/v1/consultations`；`send_consultation_message`（L2）`POST /api/c/v1/consultations/{id}/messages`。

```json
{
    "name": "query_prescriptions",
    "description": "查询处方列表",
    "parameters": {
        "type": "object",
        "properties": {"patient_id": {"type": "integer", "description": "就诊人ID"}},
        "required": []
    },
    "java_endpoint": "GET /api/c/v1/prescriptions",
    "security_level": "L1"
}
```

处方分组其余工具：`interpret_prescription`（L1）`GET /api/c/v1/prescriptions/{id}/interpretation`。

```json
{
    "name": "create_drug_order",
    "description": "创建购药订单",
    "parameters": {
        "type": "object",
        "properties": {
            "prescription_id": {"type": "integer", "description": "处方ID"},
            "pharmacy_id": {"type": "integer", "description": "药房ID"},
            "delivery_address": {"type": "string", "description": "配送地址"}
        },
        "required": ["prescription_id", "pharmacy_id", "delivery_address"]
    },
    "java_endpoint": "POST /api/c/v1/drug-orders",
    "security_level": "L2"
}
```

购药分组其余工具：`query_pharmacy_stock`（L1）`GET /api/c/v1/pharmacies/inventory`；`query_drug_orders`（L1）`GET /api/c/v1/drug-orders`；`cancel_drug_order`（L2）`POST /api/c/v1/drug-orders/{id}/cancel`；`confirm_drug_receipt`（L2）`POST /api/c/v1/drug-orders/{id}/confirm-receipt`。

```json
{
    "name": "manage_allergy",
    "description": "维护健康档案过敏史",
    "parameters": {
        "type": "object",
        "properties": {
            "action": {"type": "string", "enum": ["ADD", "UPDATE"], "description": "操作类型"},
            "allergen": {"type": "string", "description": "过敏原"}
        },
        "required": ["action", "allergen"]
    },
    "java_endpoint": "POST /api/c/v1/health-record/allergies",
    "security_level": "L2"
}
```

健康分组其余工具：`query_health_record`（L1）`GET /api/c/v1/health-record`；`manage_medical_history`（L2）`POST /api/c/v1/health-record/histories`；`query_reports`（L1）`GET /api/c/v1/reports`；`create_report`（L2）`POST /api/c/v1/reports`；`query_medication_plans`（L1）`GET /api/c/v1/medication-plans`；`update_medication_plan`（L2）`PATCH /api/c/v1/medication-plans/{id}`；`query_follow_ups`（L1）`GET /api/c/v1/follow-ups`；`confirm_follow_up`（L2）`POST /api/c/v1/follow-ups/{id}/confirm`；`manage_notifications`（L1）`GET /api/c/v1/notifications`、`POST /api/c/v1/notifications/{id}/read`。

```json
{
    "name": "search_medical_knowledge",
    "description": "本地 RAG 科普检索",
    "parameters": {
        "type": "object",
        "properties": {"query": {"type": "string", "description": "检索关键词"}},
        "required": ["query"]
    },
    "java_endpoint": "本地工具，不经 Java",
    "security_level": "L1"
}
```

#### B 端（节选分组示例）
```json
{
    "name": "query_patient_history",
    "description": "查询患者既往史与就诊记录（需确认）",
    "parameters": {
        "type": "object",
        "properties": {"patient_id": {"type": "integer", "description": "患者ID"}},
        "required": ["patient_id"]
    },
    "java_endpoint": "GET /api/b/admin/patients/{id}",
    "security_level": "L2"
}
```

接诊辅助分组其余工具：`query_drug_guide`（L1）`GET /api/b/admin/drugs`；`check_drug_interaction`（L1，本地规则引擎）；`generate_draft_note`（L2）`PUT /api/b/doctor/consult/{id}/note`。

```json
{
    "name": "recommend_care",
    "description": "导诊推荐科室",
    "parameters": {
        "type": "object",
        "properties": {"symptom": {"type": "string", "description": "症状描述"}},
        "required": ["symptom"]
    },
    "java_endpoint": "POST /api/c/v1/triage/assessments",
    "security_level": "L1"
}
```

```json
{
    "name": "check_contraindication",
    "description": "处方禁忌/过敏/重复用药风险检查",
    "parameters": {
        "type": "object",
        "properties": {"prescription_id": {"type": "integer", "description": "处方ID"}},
        "required": ["prescription_id"]
    },
    "java_endpoint": "本地规则引擎",
    "security_level": "L1"
}
```

处方审核分组其余工具：`check_allergy_risk`（L1）、`check_duplicate_medication`（L1），参数同为 `prescription_id`。

```json
{
    "name": "interpret_report",
    "description": "检查报告解读（本地 RAG）",
    "parameters": {
        "type": "object",
        "properties": {"report_id": {"type": "integer", "description": "报告ID"}},
        "required": ["report_id"]
    },
    "java_endpoint": "本地工具，不经 Java",
    "security_level": "L1"
}
```

> **L3/L4 工具不注册**：支付扣款、开具处方、下诊断结论、删除病历等操作 Agent 不可调用。
>

---

