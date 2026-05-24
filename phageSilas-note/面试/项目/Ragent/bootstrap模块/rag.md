## RAG 包概览

`rag` 包是整个项目的**核心大脑**——负责从用户提问到 AI 回答的完整 RAG（检索增强生成）流程。它包含 **20 个子包**，按职责分为 6 大层。

---

## 一、请求入口层

### [`controller/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller)

对外暴露 REST API 的控制器，共 8 个 Controller：

| Controller | 职责 |
|---|---|
| [RAGChatController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/RAGChatController.java) | **核心入口**：SSE 流式对话 `/rag/v3/chat`、停止任务 `/rag/v3/stop` |
| [ConversationController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/ConversationController.java) | 会话 CRUD（创建、列表、删除、重命名） |
| [IntentTreeController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/IntentTreeController.java) | 意图树节点管理（增删改查、批量操作） |
| [RagTraceController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/RagTraceController.java) | 链路追踪查询（run 列表、节点详情） |
| [RAGSettingsController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/RAGSettingsController.java) | 系统设置读写 |
| [QueryTermMappingController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/QueryTermMappingController.java) | 查询词映射管理 |
| [SampleQuestionController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/SampleQuestionController.java) | 示例问题管理 |
| [MessageFeedbackController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/controller/MessageFeedbackController.java) | 消息反馈（点赞/点踩） |

`request/` 和 `vo/` 子目录分别存放请求 DTO 和响应 VO。

---

## 二、核心流水线层 — `core/`（最重要）

`core/` 包含 8 个子包，构成 RAG 的**完整处理流水线**，由 [StreamChatPipeline](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/pipeline/StreamChatPipeline.java) 统一编排。

### 流水线执行顺序

```
用户提问
  │
  ▼
① memory/        加载对话历史 + 追加当前问题
  │
  ▼
② rewrite/       改写问题 + 拆分子问题（复杂问句拆成多个）
  │
  ▼
③ intent/        对每个子问题进行意图分类（属于哪个知识库 / MCP 工具）
  │
  ├── ④ guidance/   问题模糊？→ LLM 生成引导语，直接返回（短路）
  ├── ⑤ 纯系统意图？  → 不需要检索，直接用系统 Prompt 回答（短路）
  │
  ▼
⑥ retrieve/      多通道并行检索（Milvus 向量 + 意图定向）
  │
  ├── 结果为空？    → "未检索到相关内容"（短路）
  │
  ▼
⑦ prompt/        组装 System Prompt + 检索证据 + 对话历史 + 用户问题
  │
  ▼
⑧ infra LLM      流式调用大模型，SSE 推送给前端
  │
  ▼
⑨ mcp/           （可选）MCP 工具调用，将工具结果注入上下文
```


---

### ② [`core/rewrite/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite) — 查询改写

| 类 | 作用 |
|---|---|
| [QueryRewriteService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite/QueryRewriteService.java) 接口 | 将口语化问题改写成适合检索的简洁查询 |
| [MultiQuestionRewriteService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite/MultiQuestionRewriteService.java) | 用 LLM 将复杂问句拆成多个子问题（"A和B分别是什么？" → ["A是什么", "B是什么"]） |
| [QueryTermMappingService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite/QueryTermMappingService.java) | 术语映射（用户说"数据库"映射到技术文档里的"DB"） |

**关键数据**：[`RewriteResult`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/rewrite/RewriteResult.java) — 包含 `rewrittenQuestion`（改写后问题）和 `subQuestions`（子问题列表）。

---

### ③ [`core/intent/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent) — 意图识别

这是 RAG 的核心决策层，决定"该去哪些知识库检索、该调用哪些 MCP 工具"。

| 类 | 作用 |
|---|---|
| [IntentNode](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent/IntentNode.java) | 意图节点实体（KB 节点绑定 `kbId`+`collectionName`；MCP 节点绑定工具定义） |
| [IntentClassifier](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent/IntentClassifier.java) 接口 | 用 LLM 让每个子问题匹配到最相关的意图节点 |
| [IntentResolver](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent/IntentResolver.java) | **并行** 对每个子问题调用 `IntentClassifier`，过滤低分意图，限制总数防 token 超限 |
| [IntentTreeCacheManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent/IntentTreeCacheManager.java) | 意图树缓存（Caffeine），避免每次请求都查 DB |
| [NodeScoreFilters](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/intent/NodeScoreFilters.java) | 按意图类型过滤（`mcp()` / `kb()`） |

**流程**：
```
子问题列表 → 并行 classify → NodeScore 列表（每个带分数）
→ 过滤 score < 0.5 → 限制总数 ≤ MAX_INTENT_COUNT → 保底每子问题至少1个
```


---

### ④ [`core/guidance/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/guidance) — 歧义引导

| 类 | 作用 |
|---|---|
| [IntentGuidanceService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/guidance/IntentGuidanceService.java) | 检测问题是否模糊，生成引导提示（"您想了解哪个方面？"） |
| [AmbiguityLLMChecker](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/guidance/AmbiguityLLMChecker.java) | 用 LLM 判断改写后问题是否歧义 |

**效果**：当所有意图节点匹配分数都极低时，不强行检索，而是引导用户澄清问题，避免"答非所问"。

---

### ⑥ [`core/retrieve/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve) — 多通道检索（最复杂）

**设计模式**：策略 + 责任链 + 并行执行。

```
MultiChannelRetrievalEngine
  │
  ├── 阶段1: 并行检索通道（CompletableFuture 并发）
  │   ├── VectorGlobalSearchChannel    ← 全局向量检索（Milvus）
  │   └── IntentDirectedSearchChannel  ← 意图定向检索（指定 collection）
  │
  ├── 阶段2: 后置处理器链（顺序执行）
  │   ├── DeduplicationPostProcessor   ← 去重（MMR 算法）
  │   └── RerankPostProcessor          ← 重排序（Rerank 模型）
  │
  └── 输出: List<RetrievedChunk>
```


**核心类**：

| 类                                                                                                                                                                         | 作用                                    |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| [MultiChannelRetrievalEngine](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/MultiChannelRetrievalEngine.java)             | 协调器：收集意图 → 并行调通道 → 顺序执行后处理器           |
| [SearchChannel](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/channel/SearchChannel.java) 接口                              | 检索通道抽象，含 `isEnabled()`、`search()`、优先级 |
| [VectorGlobalSearchChannel](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/channel/VectorGlobalSearchChannel.java)         | 全局向量检索：将问题向量化后在 Milvus 全库搜索 Top-K     |
| [IntentDirectedSearchChannel](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/channel/IntentDirectedSearchChannel.java)     | 意图定向：只搜该意图节点绑定的 collection            |
| [SearchResultPostProcessor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/postprocessor/SearchResultPostProcessor.java)   | 后处理器接口                                |
| [DeduplicationPostProcessor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/postprocessor/DeduplicationPostProcessor.java) | MMR 去重（避免返回相似度过高的 chunk）              |
| [RerankPostProcessor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/retrieve/postprocessor/RerankPostProcessor.java)               | Rerank 重排序（用更精确的模型重新打分）               |

`channel/strategy/` 下还有两种并行检索策略：
- `CollectionParallelRetriever` — 同一 KB 下多个 collection 并行搜
- `IntentParallelRetriever` — 多个意图节点并行搜

---

### ⑦ [`core/prompt/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt) — Prompt 组装

将检索结果、MCP 工具结果、子问题等组装成最终发给 LLM 的消息列表。

| 类 | 作用 |
|---|---|
| [RAGPromptService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt/RAGPromptService.java) | **核心编排**：选模板 → 填充证据 → 构建 `[system, history..., user-with-evidence]` |
| [PromptTemplateLoader](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt/PromptTemplateLoader.java) | 从 classpath 加载 Mustache 模板文件 |
| [ContextFormatter](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt/ContextFormatter.java) | 将检索到的 chunks 格式化为 LLM 可读的文本 |
| [PromptScene](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/prompt/PromptScene.java) | 枚举：`KB_ONLY` / `MCP_ONLY` / `MIXED` / `EMPTY` |

**三种场景**：
```
KB_ONLY  → 用模板 RAG_ENTERPRISE_PROMPT_PATH
MCP_ONLY → 用模板 MCP_ONLY_PROMPT_PATH
MIXED    → 用模板 MCP_KB_MIXED_PROMPT_PATH（知识库 + 工具调用混合）
```


---

### ⑨ [`core/mcp/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/mcp) — MCP 工具调用

支持 LLM 在执行过程中调用外部工具（如数据库查询、API 调用）。

| 类 | 作用 |
|---|---|
| [McpToolExecutor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/mcp/McpToolExecutor.java) 接口 | MCP 工具执行抽象 |
| [McpToolRegistry](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/mcp/McpToolRegistry.java) | 工具注册中心 |
| [McpParameterExtractor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/mcp/McpParameterExtractor.java) | 从 LLM 输出中提取工具参数 |

---

### ① [`core/memory/`](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/memory) — 对话记忆

| 类 | 作用 |
|---|---|
| [ConversationMemoryService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/memory/ConversationMemoryService.java) | 对话历史加载/追加 |
| [ConversationMemoryStore](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/memory/ConversationMemoryStore.java) | 历史持久化（JDBC/ES） |
| [ConversationMemorySummaryService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core/memory/ConversationMemorySummaryService.java) | 长篇对话自动摘要，压缩历史 token |

---

## 三、业务服务层 — `service/`

| 子包/类                                                                                                                                                                      | 作用                                                                                |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| [RAGChatServiceImpl](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/impl/RAGChatServiceImpl.java)                                | `streamChat()` 主流程：创建 Trace → 创建 Pipeline 上下文 → 调用 `StreamChatPipeline.execute()` |
| [pipeline/StreamChatPipeline](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/pipeline/StreamChatPipeline.java)                   | **流水线编排器**，私有方法串联全部核心步骤                                                           |
| [pipeline/StreamChatContext](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/pipeline/StreamChatContext.java)                     | 流水线上下文（承载 question、history、callback、intents 等）                                    |
| [handler/StreamTaskManager](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/handler/StreamTaskManager.java)                       | 管理并发的流式任务，支持按 `taskId` 取消                                                         |
| [handler/StreamChatEventHandler](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/handler/StreamChatEventHandler.java)             | SSE 事件处理器                                                                         |
| [ratelimit/ChatQueueLimiter](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/ratelimit/ChatQueueLimiter.java)                     | 聊天排队限流（控制并发 LLM 调用数）                                                              |
| [ratelimit/FairDistributedRateLimiter](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/ratelimit/FairDistributedRateLimiter.java) | **分布式公平限流器**：Redis ZSet 排队 + Lua 原子抢位 + Redisson Semaphore 控流                     |
| [RagTraceRecordService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/RagTraceRecordService.java)                               | 链路追踪记录的持久化                                                                        |
| [S3FileStorageService](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/service/impl/S3FileStorageService.java)                            | 文件上传到 S3（MinIO），聊天中引用文件                                                           |

---

## 四、链路追踪层 — `trace/`

| 类                                                                                                                                                 | 作用                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [StreamChatTraceRunner](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/trace/StreamChatTraceRunner.java)         | 用 callback 包装器拦截 onFirstContent → 记录 TTFT；onComplete/onError → finishRun |
| [RagStreamTraceSupportImpl](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/trace/RagStreamTraceSupportImpl.java) | Trace 上下文支持                                                              |
| [aop/RagTraceAspect](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/aop/RagTraceAspect.java)                     | AOP 切面：拦截 `@RagTraceNode` 注解的方法，自动记录节点耗时和状态                              |

**记录指标**：
- TTFT（首包时间）：从请求进入到前端收到第一个字
- 每个节点的耗时（rewrite、intent、retrieve 等）
- 整个 run 的成功/失败状态

---

## 五、消息队列层 — `mq/`

| 类 | 作用 |
|---|---|
| [MessageFeedbackConsumer](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/mq/MessageFeedbackConsumer.java) | 异步消费用户反馈（点赞/点踩），写入 DB |

反馈通过 RocketMQ 异步解耦，避免阻塞 SSE 连接。

---

## 六、基础设施层

### `config/` — 配置

| 类                                                                                                                                                | 作用                                  |
| ------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------- |
| [RAGConfigProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/RAGConfigProperties.java)           | RAG 核心配置                            |
| [SearchChannelProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/SearchChannelProperties.java)   | 检索通道开关和参数                           |
| [RAGRateLimitProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/RAGRateLimitProperties.java)     | 限流参数                                |
| [MemoryProperties](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/MemoryProperties.java)                 | 记忆配置（摘要触发阈值等）                       |
| [MilvusConfig](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/MilvusConfig.java)                         | Milvus 连接配置                         |
| [DemoModeInterceptor](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/DemoModeInterceptor.java)           | 演示模式拦截器                             |
| [ThreadPoolExecutorConfig](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/config/ThreadPoolExecutorConfig.java) | 专用线程池（retrieval、intent、streaming 等） |

### `dao/` — 数据访问

| 实体                         | 作用                  |
| -------------------------- | ------------------- |
| `ConversationDO/MessageDO` | 会话和消息记录             |
| `IntentNodeDO`             | 意图树节点（KB 节点/MCP 节点） |
| `QueryTermMappingDO`       | 查询词映射               |
| `RagTraceRunDO/NodeDO`     | 链路追踪记录              |
| `MessageFeedbackDO`        | 消息反馈（点赞/点踩）         |
|                            |                     |

### `dto/` / `enums/` / `constant/` — 公共数据结构

| 包 | 内容 |
|---|---|
| `dto/` | `RetrievalContext`（检索结果）、`IntentCandidate`、`StoredFileDTO` 等 |
| `enums/` | `IntentKind`（KB/MCP/SYSTEM）、`IntentLevel`、`SSEEventType` |
| `constant/` | 路径常量、阈值常量 |

### `eval/` — 评估模块

| 类 | 作用 |
|---|---|
| [EvalController](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/eval/EvalController.java) | 离线评估接口（测试 RAG 质量） |

### `util/` — 工具

[FileTypeDetector](file:///D:/IDEA-java/ragent/bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/util/FileTypeDetector.java) — 文件 MIME 类型检测。

---

## 完整请求生命周期

```
GET /rag/v3/chat?question=xxx&conversationId=yyy
         │
         ▼
RAGChatController.chat()
  → 创建 SseEmitter，调用 RAGChatService.streamChat()
         │
         ▼
RAGChatServiceImpl.streamChat()
  → StreamChatTraceRunner.run()  (trace 包装)
         │
         ▼
StreamChatPipeline.execute(ctx)
  ├── loadMemory()         → ConversationMemoryService
  ├── rewriteQuery()       → QueryRewriteService (LLM 改写 + 拆分子问题)
  ├── resolveIntents()     → IntentResolver (并行 classify)
  ├── handleGuidance()     → 歧义？直接返回引导语
  ├── handleSystemOnly()   → 纯系统？直接 LLM 回答
  ├── retrieve()           → MultiChannelRetrievalEngine (并行通道 + 后处理)
  ├── handleEmptyRetrieval() → 无结果？提示用户
  └── streamRagResponse()  → RAGPromptService → LLMService.streamChat()
         │
         ▼
SSE 推送给前端 (text/event-stream)
  event: content → "您好..."
  event: complete → done
```
