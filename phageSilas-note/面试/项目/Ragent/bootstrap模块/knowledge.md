## knowledge 包各子包职责

`knowledge` 包是 **知识库管理层**——它位于底层摄入流水线和向量存储之上，提供知识库、文档、分块三个粒度的管理能力。可以理解为"知识库的资源管理器"。

---

### 层次关系

```
KnowledgeBase (知识库)                     e.g. "产品A知识库"
  ├── Document (文档)                      e.g. "用户手册.pdf"
  │     ├── Chunk (第1段)                  e.g. "第一章..."
  │     ├── Chunk (第2段)
  │     └── ...
  └── Document (文档)
        ├── Chunk
        └── ...
```


---

### 1. `controller/` — REST 接口层（三组 API）

| Controller | 路径前缀 | 职责 |
|---|---|---|
| [KnowledgeBaseController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/controller/KnowledgeBaseController.java) | `/knowledge-base` | 知识库 CRUD：创建、重命名、删除、查询详情、分页列表、查询支持的分块策略 |
| [KnowledgeDocumentController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/controller/KnowledgeDocumentController.java) | `/knowledge-base/{kb-id}/docs` | 文档管理：上传文件、触发分块、删除、更新、分页查询、搜索、启用/禁用、查看分块日志 |
| [KnowledgeChunkController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/controller/KnowledgeChunkController.java) | `/knowledge-base/docs/{doc-id}/chunks` | Chunk 管理：分页查询、新增、更新、删除、单个/批量启用禁用 |

---

### 2. `service/` — 业务逻辑层

| 接口 | 核心职责 |
|---|---|
| [KnowledgeBaseService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/KnowledgeBaseService.java) | 知识库的创建（含向量集合创建）、重命名、删除、分页查询 |
| [KnowledgeDocumentService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/KnowledgeDocumentService.java) | **核心枢纽**：上传文档（本地文件 / 远程 URL）、`startChunk()` 发 MQ 消息触发异步分块、`executeChunk()` 执行完整分块流程（由 MQ 消费者调用）、删除、启用/禁用 |
| [KnowledgeChunkService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/KnowledgeChunkService.java) | Chunk 的 CRUD、批量创建（可选同步写向量库）、启用/禁用、按文档批量操作 |
| [KnowledgeDocumentScheduleService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/service/KnowledgeDocumentScheduleService.java) | 定时刷新任务的配置管理 |

---

### 3. `dao/` — 数据持久层

| 实体 | 对应表 |
|---|---|
| `KnowledgeBaseDO` | 知识库（名称、描述、向量集合名、分块配置） |
| `KnowledgeDocumentDO` | 文档（来源类型、文件名、状态、哈希、ETag 等） |
| `KnowledgeChunkDO` | 分块（内容、序号、启用状态、向量 ID） |
| `KnowledgeDocumentChunkLogDO` | 分块操作日志（记录每次分块的输入输出） |
| `KnowledgeDocumentScheduleDO` | 定时刷新任务（远程 URL、Cron 表达式、下次执行时间） |
| `KnowledgeDocumentScheduleExecDO` | 定时刷新执行记录 |

`handler/JsonbTypeHandler` 用于处理 PostgreSQL 的 JSONB 字段类型映射。

---

### 4. `mq/` — RocketMQ 异步分块

文档分块是一个耗时的 CPU/IO 密集型操作（文本提取 → 分块 → 向量嵌入 → 写库），如果同步执行会阻塞 HTTP 请求。因此采用异步模式：

![[image-6.png|579x176]]

| 类 | 职责 |
|---|---|
| [KnowledgeDocumentChunkEvent](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/mq/event/KnowledgeDocumentChunkEvent.java) | MQ 消息体：docId、kbId、操作人 |
| [KnowledgeDocumentChunkConsumer](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/mq/KnowledgeDocumentChunkConsumer.java) | RocketMQ 消费者，消费后调用 `executeChunk()` 执行分块 |
| [KnowledgeDocumentChunkTransactionChecker](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/mq/KnowledgeDocumentChunkTransactionChecker.java) | 事务消息回查（保证消息不丢） |

---

### 5. `schedule/` — 定时刷新子系统

支持对**远程 URL 文档**设置 Cron 定时拉取——当源文件更新时自动重新摄入。这是一个完整的迷你调度引擎：
![[image-7.png|579x318]]


| 类 | 职责 |
|---|---|
| [KnowledgeDocumentScheduleJob](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/KnowledgeDocumentScheduleJob.java) | `@Scheduled` 定时扫描到期任务 + 恢复卡在 RUNNING 的异常文档 |
| [ScheduleLockManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/ScheduleLockManager.java) | 基于数据库的分布式锁（lock_until 字段 CAS 更新） |
| [ScheduleRefreshProcessor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/ScheduleRefreshProcessor.java) | 核心刷新逻辑：拉取远程文件 → 检测变更 → 触发重新分块 |
| [ScheduleStateManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/ScheduleStateManager.java) | 管理任务的 nextRunTime、lastRunStatus、lastErrorMessage 等状态 |
| [CronScheduleHelper](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/CronScheduleHelper.java) | Cron 表达式解析，计算下次执行时间 |
| [DocumentStatusHelper](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/schedule/DocumentStatusHelper.java) | 恢复卡在 RUNNING 状态超过阈值的文档 |

---

### 6. `handler/` — 远程文件处理

| 类 | 职责 |
|---|---|
| [RemoteFileFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/handler/RemoteFileFetcher.java) | 流式拉取远程 URL 文件，支持 HEAD 预检、文件大小限制、SHA-256 内容哈希、ETag/Last-Modified 变更检测。用于**上传时拉取远程文件**（`fetchAndStore`）和**定时刷新时检测变更**（`fetchIfChanged`）两种场景 |

---

### 7. `filter/` — Servlet 过滤器

| 类 | 职责 |
|---|---|
| [UploadRateLimitFilter](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/filter/UploadRateLimitFilter.java) | 基于 Redisson 信号量的上传限流，在 multipart 解析**之前**拦截（`HIGHEST_PRECEDENCE`），避免临时文件产生后才发现超过并发限制。超限返回 429 |

---

### 8. `config/` — 配置类

| 类 | 职责 |
|---|---|
| [KnowledgeScheduleProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/config/KnowledgeScheduleProperties.java) | 定时任务配置：扫描间隔、锁超时、批量大小、最小拉取间隔、RUNNING 超时阈值（`rag.knowledge.schedule.*`） |
| [RagSemaphoreProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/config/RagSemaphoreProperties.java) | 信号量配置：上传并发许可数、最大等待时间、租约时长 |
| [SemaphoreInitializer](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge/config/SemaphoreInitializer.java) | 启动时初始化 Redisson 信号量 |

---

### 9. `enums/` — 枚举定义

| 枚举 | 值 | 用途 |
|---|---|---|
| `DocumentStatus` | `pending` → `running` → `success` / `failed` | 文档处理状态生命周期 |
| `ProcessMode` | — | 处理模式（如手动/自动） |
| `ScheduleRunStatus` | — | 定时任务执行状态 |
| `SourceType` | — | 文档来源类型（本地/远程） |

---

### 与 `ingestion` 包的关系

![[image-8.png|579x159]]


- `knowledge` 包关注 **"管理"**——知识库/文档/分块的 CRUD、状态流转、定时调度
- `ingestion` 包关注 **"执行"**——实际的文档获取、解析、分块、向量化的流水线
- 两者通过 `KnowledgeDocumentService.executeChunk()` 桥接，该方法内部调用了 IngestionEngine 执行完整流水线