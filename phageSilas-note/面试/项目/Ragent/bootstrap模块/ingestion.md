## 总结

`ingestion` 包是 Ragent AI 平台的**文档摄入流水线子系统**，负责将外部文档（本地文件、URL、飞书、S3 等）经过**获取 → 解析 → 增强 → 分块 → 富化 → 索引**六个环节，最终写入向量数据库（Milvus），为后续的 RAG 问答提供可检索的知识基础。它是整个 RAG 链路中"**知识入库**"的核心模块。

---

## 整体架构

![[image-3.png]]

---

## 核心设计：可配置的 DAG 流水线

整个模块基于**策略模式 + 责任链模式**构建了一个灵活可配置的流水线引擎：

- **[PipelineDefinition](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/pipeline/PipelineDefinition.java)** — 流水线定义，包含节点列表和连接关系
- **[NodeConfig](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/pipeline/NodeConfig.java)** — 每个节点的配置（类型、条件、下一节点、参数）
- **[IngestionEngine](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/IngestionEngine.java)** — **流水线执行引擎**，按 `nextNodeId` 链式执行各节点，支持：
  - 🔍 **环形检测** — 执行前验证 DAG 无环
  - 🎯 **条件执行** — 通过 [ConditionEvaluator](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/ConditionEvaluator.java) 动态跳过节点
  - 📋 **全链路日志** — 每个节点的执行耗时、输入输出记录到 [NodeLog](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/NodeLog.java)

---
## 六种流水线节点

所有节点都实现统一的 [IngestionNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/IngestionNode.java) 接口，通过 [IngestionContext](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/IngestionContext.java) 在节点间传递共享状态：

| 节点           | 类                                                                                                                            | 作用                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Fetcher**  | [FetcherNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/FetcherNode.java)   | 从多数据源获取文档原始字节流。通过策略模式路由到 [LocalFileFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/LocalFileFetcher.java)、[HttpUrlFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/HttpUrlFetcher.java)、[FeishuFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/FeishuFetcher.java)、[S3Fetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/S3Fetcher.java)，支持幂等（已有字节流则跳过） |
| **Parser**   | [ParserNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/ParserNode.java)     | 使用 Apache Tika 将 PDF/Word/Excel/Markdown 等格式解析为纯文本，支持按 MIME 类型配置解析规则                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **Enhancer** | [EnhancerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/EnhancerNode.java) | 调用 LLM 对**整篇文档**进行增强：上下文增强、关键词提取、问题生成、元数据提取                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **Chunker**  | [ChunkerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/ChunkerNode.java)   | 将长文本按策略切分成小块（默认 512 token，128 重叠），**同时对每个 chunk 生成向量嵌入（embedding）**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Enricher** | [EnricherNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/EnricherNode.java) | 对**每个 chunk** 调用 LLM 进行精细增强：提取关键词、生成摘要、补充元数据                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **Indexer**  | [IndexerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/IndexerNode.java)   | 将处理后的分块数据**写入 Milvus 向量数据库**，自动创建 Collection，支持 `skipIndexerWrite` 模式供调用方统一事务写入                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |

---

## 对外接口

通过两个 REST Controller 暴露服务：

- **[IngestionPipelineController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller/IngestionPipelineController.java)** — 流水线的 CRUD 管理（创建/编辑/查看/删除流水线模板）
- **[IngestionTaskController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller/IngestionTaskController.java)** — 任务的执行与查询（创建执行任务、上传文件触发任务、查看任务状态和节点日志）

---

## 在整个 Ragent AI 项目中的定位

```
┌──────────────────────────────────────────────────────────────┐
│                      Ragent AI 平台                           │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │  Ingestion   │───▶│  Retrieval   │───▶│  Generation  │   │
│  │   (本模块)    │    │  (多路检索)   │    │  (LLM生成)   │   │
│  │  文档入库     │    │  意图识别     │    │  流式输出     │   │
│  └──────────────┘    └──────────────┘    └──────────────┘   │
│         │                   ▲                    │           │
│         ▼                   │                    ▼           │
│  ┌──────────────────────────────────────────────────┐       │
│  │              向量数据库 (Milvus)                   │       │
│  └──────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────┘
```


简单来说，`ingestion` 是 RAG 三大环节中的**第一环**——没有 ingestion 把文档变成可检索的向量数据，后续的检索和生成就无从谈起。它是平台的"**知识入口**"。


## `ingestion` 包下各子包的职责

按分层架构自底向上组织：

---

### 1. `domain/` — 领域模型层（核心数据结构）

定义了整个摄入系统的**所有领域概念**：

| 子包 | 类 | 职责 |
|---|---|---|
| **pipeline/** | [PipelineDefinition](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/pipeline/PipelineDefinition.java)、[NodeConfig](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/pipeline/NodeConfig.java) | 流水线的完整定义和节点配置（类型、条件、下一节点、参数），是流水线的"蓝图" |
| **context/** | [IngestionContext](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/IngestionContext.java) | **流水线执行的共享上下文**，承载 `rawBytes → rawText → chunks → enhancedText → keywords` 等全链路的中间数据，是节点间唯一的"数据总线" |
| | [DocumentSource](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/DocumentSource.java) | 文档来源描述（类型 + 位置/路径） |
| | [StructuredDocument](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/StructuredDocument.java) | 解析后的结构化文档对象 |
| | [NodeLog](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/context/NodeLog.java) | 每个节点的执行日志（耗时、成功/失败、输出摘要） |
| **enums/** | [IngestionNodeType](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/enums/IngestionNodeType.java) | 六种节点类型：`fetcher`、`parser`、`enhancer`、`chunker`、`enricher`、`indexer` |
| | [SourceType](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/enums/SourceType.java) | 文档来源类型：`file`、`url`、`feishu`、`s3` |
| | [IngestionStatus](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/enums/IngestionStatus.java) | 任务状态：RUNNING、COMPLETED、FAILED |
| | [EnhanceType](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/enums/EnhanceType.java)、[ChunkEnrichType](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/enums/ChunkEnrichType.java) | 文档级/Chunk级增强任务类型 |
| **result/** | [NodeResult](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/result/NodeResult.java)、[IngestionResult](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/domain/result/IngestionResult.java) | 节点级/任务级执行结果 |
| **settings/** | ChunkerSettings、ParserSettings、EnhancerSettings、EnricherSettings、IndexerSettings | 各节点的可配置参数（如分块大小、增强任务列表、索引字段等） |

---

### 2. `node/` — 节点实现层（处理逻辑）

六种流水线节点的具体实现，全部实现 [IngestionNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/IngestionNode.java) 接口：

| 类 | 在流水线中的位置 | 核心职责 |
|---|---|---|
| [FetcherNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/FetcherNode.java) | 入口 | 按 `SourceType` 路由到具体 Fetcher，拉取文档字节流，存入 `context.rawBytes` |
| [ParserNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/ParserNode.java) | 第2步 | 用 Apache Tika 将 PDF/Word/Excel 等解析为纯文本，存入 `context.rawText` |
| [EnhancerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/EnhancerNode.java) | 第3步 | 调用 LLM 对整篇文档做上下文增强、关键词提取、问题生成，结果写入 context |
| [ChunkerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/ChunkerNode.java) | 第4步 | 将文本切分为小块，**同时生成向量嵌入**，存入 `context.chunks` |
| [EnricherNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/EnricherNode.java) | 第5步 | 对每个 chunk 调用 LLM 提取关键词/摘要/元数据，写入 chunk 的 metadata |
| [IndexerNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/node/IndexerNode.java) | 终点 | 将 chunks 写入 Milvus 向量数据库，自动创建 Collection |

---

### 3. `engine/` — 流水线执行引擎

| 类 | 职责 |
|---|---|
| [IngestionEngine](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/IngestionEngine.java) | **核心调度器**：按 `nextNodeId` 链式驱动节点执行，负责环形检测、起始节点查找、条件判断、异常处理、状态管理 |
| [ConditionEvaluator](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/ConditionEvaluator.java) | 评估节点上的条件表达式，决定是否跳过某个节点（支持 `eq`、`contains`、`in` 等运算符） |
| [NodeOutputExtractor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/NodeOutputExtractor.java) | 从 context 中提取每个节点的输出摘要，用于日志记录 |

---

### 4. `strategy/fetcher/` — 文档获取策略

FetcherNode 通过**策略模式**将获取逻辑委托给这些实现：

| 类 | 接口 | 支撑的数据源 |
|---|---|---|
| [DocumentFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/DocumentFetcher.java) | 统一接口 | `supportedType()` + `fetch(source)` |
| [LocalFileFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/LocalFileFetcher.java) | — | 本地文件系统 (`file`) |
| [HttpUrlFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/HttpUrlFetcher.java) | — | HTTP/HTTPS URL (`url`) |
| [FeishuFetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/FeishuFetcher.java) | — | 飞书文档 (`feishu`) |
| [S3Fetcher](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/S3Fetcher.java) | — | S3 兼容对象存储 (`s3`) |
| [FetchResult](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/strategy/fetcher/FetchResult.java) | — | 获取结果：字节内容 + MIME 类型 + 文件名 |

---

### 5. `service/` — 业务服务层

| 类 | 职责 |
|---|---|
| [IngestionPipelineService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/service/IngestionPipelineService.java) / Impl | 流水线的 CRUD 管理：创建、编辑、查看、删除、分页查询流水线定义 |
| [IngestionTaskService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/service/IngestionTaskService.java) / Impl | **任务执行入口**：组装 context → 获取 PipelineDefinition → 调用 Engine 执行 → 持久化结果和日志 |
| [IntentTreeService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/service/IntentTreeService.java) / Impl | 意图树相关业务（摄入模块中的意图分类辅助逻辑） |

---

### 6. `controller/` — REST 接口层

| Controller | 暴露的 API | 职责 |
|---|---|---|
| [IngestionPipelineController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller/IngestionPipelineController.java) | `POST/GET/PUT/DELETE /ingestion/pipelines` | 流水线模板的 CRUD |
| [IngestionTaskController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller/IngestionTaskController.java) | `POST /ingestion/tasks`、`POST /ingestion/tasks/upload`、`GET /ingestion/tasks/{id}` | 触发执行任务、上传文件、查看任务状态和节点日志 |

辅助子包：
- **request/** — 入参 DTO（CreateRequest、UpdateRequest、NodeRequest）
- **vo/** — 出参 ViewObject（PipelineVO、TaskVO 等）

---

### 7. `dao/` — 数据持久层

| 子包 | 内容 | 职责 |
|---|---|---|
| **entity/** | IngestionPipelineDO、IngestionPipelineNodeDO、IngestionTaskDO、IngestionTaskNodeDO | MyBatis-Plus 实体，对应数据库中的流水线和任务表 |
| **mapper/** | 对应的 Mapper 接口 | 数据库 CRUD 操作 |

---

### 8. `prompt/` — LLM 提示词管理

| 类 | 职责 |
|---|---|
| [EnhancerPromptManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/prompt/EnhancerPromptManager.java) | 管理 Enhancer 节点各类增强任务的默认 System Prompt（如上下文增强、关键词提取、问题生成） |
| [EnricherPromptManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/prompt/EnricherPromptManager.java) | 管理 Enricher 节点各类 Chunk 增强任务的默认 System Prompt（如摘要、关键词、元数据提取） |

---

### 9. `util/` — 工具类

| 类 | 职责 |
|---|---|
| [MimeTypeDetector](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/util/MimeTypeDetector.java) | 根据文件字节或扩展名检测 MIME 类型 |
| [JsonResponseParser](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/util/JsonResponseParser.java) | 解析 LLM 返回的 JSON 响应（字符串列表或 JSON 对象） |
| [PromptTemplateRenderer](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/util/PromptTemplateRenderer.java) | 用变量替换渲染 Prompt 模板 |
| [HttpClientHelper](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/util/HttpClientHelper.java) | HTTP 客户端辅助工具 |

---

### 调用链路总结

```
Controller ──▶ Service ──▶ Engine ──▶ Node (6种)
    │              │           │
    ▼              ▼           ▼
 request/vo     dao/mapper   domain/context (数据总线)
                             domain/pipeline (流水线定义)
                             strategy/fetcher (获取策略)
                             prompt/ (LLM提示词)
                             util/ (工具类)
```


整个 `ingestion` 包以 **IngestionContext 为数据总线**、**IngestionEngine 为调度中枢**、**六个 Node 为处理单元**、**策略模式处理多源获取**，构成了一套完整的、可配置的文档摄入流水线。

## 为什么会有意图树相关操作
意图树中 **KB（知识库）类型的叶子节点** 必须关联一个知识库和它对应的 Milvus Collection。而知识库正是摄入流水线的产出物。所以创建意图节点时需要 **读取摄入模块的知识库元数据**。

---

### 数据流向关系

![[image-4.png]]


**摄入** 负责"造"知识库 → **意图树** 负责"用"知识库（路由查询到正确的知识库去检索）。
两者共享知识库这一概念，`IntentTreeService` 被放在 `ingestion` 包中是因为它是知识库的**消费者**，需要引用知识库的元数据。

---
### 模块边界现状

实际上从代码引用可以看出，这是一个 **跨模块依赖**：

| 组件 | 所在包 | 所属概念领域 |
|---|---|---|
| `IntentTreeController` | `rag.controller` | 意图识别 |
| `IntentTreeService` (接口) | `ingestion.service` | ⚠️ 摄入（不太合理） |
| `IntentTreeServiceImpl` | `ingestion.service.impl` | ⚠️ 摄入（不太合理） |
| `IntentTreeFactory` | `rag.core.intent` | 意图识别 |
| `IntentTreeCacheManager` | `rag.core.intent` | 意图识别 |
| `IntentNodeDO/Mapper` | `rag.dao.*` | 意图识别 |
| `KnowledgeBaseMapper` | `knowledge.dao.*` | 知识库 |

核心意图分类引擎（Factory、Cache、Classifier）全在 `rag` 包中，但 **CRUD 管理的 Service 层** 被放在了 `ingestion`。这是因为 IntentTreeService 需要注入 `KnowledgeBaseMapper` 来查询知识库信息，而知识库管理逻辑集中在 ingestion 模块。

---

### 总结

简单来说：**IntentTreeService 放在 ingestion 包中是一个"就近放置"的选择**——它虽然管理的是意图树（属于 RAG 查询阶段的配置），但创建 KB 类型节点时必须关联摄入产出的知识库，因此被放在了摄入模块中。从严格的领域划分来看，它更适合放在 `rag` 或一个独立的 `intent` 包中，属于项目演进过程中模块边界不够清晰的地方。

## 这6个IngestionNode实现类是怎么运行的,为什么只有注入了IngestionNode,但是没有地方说明要在什么时候切换到不同的实现类

切换不在代码里写死，而是**由数据库中存储的流水线配置数据驱动**的。整个机制是一个典型的 **策略模式 + 数据驱动** 设计。

---

### 完整执行链路

![[image-5.png|579x345]]

---

### 第一步：Spring 注入全部实现，按类型建 Map

[IngestionEngine](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/IngestionEngine.java#L47-L55) 通过构造器接收 Spring 自动注入的**所有 6 个实现类**，然后按 `getNodeType()` 的返回值构建 Map：

```java
// 每个实现类的 getNodeType() 返回值就是 key：
// FetcherNode  → "fetcher"
// ParserNode   → "parser"
// EnhancerNode → "enhancer"
// ChunkerNode  → "chunker"
// EnricherNode → "enricher"
// IndexerNode  → "indexer"

public IngestionEngine(List<IngestionNode> nodes, ...) {
    this.nodeMap = nodes.stream()
            .collect(Collectors.toMap(IngestionNode::getNodeType, n -> n));
}
```


---

### 第二步：用户创建流水线时指定 nodeType

[IngestionPipelineNodeRequest](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/controller/request/IngestionPipelineNodeRequest.java#L28-L58) 中有一个 `nodeType` 字段，用户在创建流水线时显式指定每个节点是什么类型：

```json
{
  "name": "文档入库流水线",
  "nodes": [
    {"nodeId": "n1", "nodeType": "fetcher", "nextNodeId": "n2", "settings": {...}},
    {"nodeId": "n2", "nodeType": "parser",  "nextNodeId": "n3", "settings": {...}},
    {"nodeId": "n3", "nodeType": "chunker", "nextNodeId": "n4", "settings": {...}},
    {"nodeId": "n4", "nodeType": "indexer", "nextNodeId": null, "settings": {...}}
  ]
}
```


这些配置存入数据库后，每次执行任务时从数据库读出还原为 `PipelineDefinition`。

---

### 第三步：执行时按 nodeType 字符串查表路由

[IngestionEngine.executeNode()](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion/engine/IngestionEngine.java#L205-L212) 中，根据当前节点的 `nodeType` 去 `nodeMap` 查对应的实现：

```java
private NodeResult executeNode(IngestionContext context, NodeConfig nodeConfig) {
    String nodeType = nodeConfig.getNodeType();  // 从数据库配置中读到的字符串
    IngestionNode node = nodeMap.get(nodeType);  // 按字符串查 Map，找到具体实现
    // ...
    NodeResult result = node.execute(context, nodeConfig); // 多态调用
}
```


而链式调度则通过 `config.getNextNodeId()` 推动：

```java
private void executeChain(String nodeId, Map<String, NodeConfig> nodeConfigMap, ...) {
    String currentNodeId = nodeId;  // 从起始节点开始
    while (currentNodeId != null) {
        NodeConfig config = nodeConfigMap.get(currentNodeId);
        executeNode(context, config);              // ← 按 nodeType 路由实现
        currentNodeId = config.getNextNodeId();     // ← 按 nextNodeId 驱动下一个节点
    }
}
```


---

### 总结

```
Spring DI 注入 6 个实现
       │
       ▼
  nodeMap = {"fetcher"→FetcherNode, "parser"→ParserNode, ...}
       │
       │  用户创建流水线时指定每个节点的 nodeType（存入 DB）
       │  执行任务时从 DB 读出 PipelineDefinition
       ▼
  executeNode() → nodeMap.get(nodeConfig.nodeType) → 找到对应实现 → 执行
```


**切换逻辑不在代码里，而在数据库里**。你想让流水线跳过 Enhancer？创建时不要加 `"nodeType": "enhancer"` 的节点就行。你想在 Chunker 之前插入一个自定义节点？注册一个新的 `IngestionNode` 实现，然后在流水线配置中加入对应的 `nodeType` 即可。这就是**可配置流水线**的核心价值。