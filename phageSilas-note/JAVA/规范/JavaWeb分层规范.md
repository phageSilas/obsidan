# Java 后端标准分层架构与 POJO 规范总结

## 一、 核心逻辑分层（Controller-Service-Mapper）

系统的“骨架”，各层严格遵守**“上层调用下层，下层不可调用上层，各司其职”**的原则。

|**分层名称**|**英文/别名**|**核心职责**|**绝不该做的事**|
|---|---|---|---|
|**表现层**|**Controller** / Web层 / API层|1. 接收前端 HTTP 请求<br><br>  <br><br>2. 基础参数校验（如 `@NotNull`）<br><br>  <br><br>3. 调用 Service 处理业务<br><br>  <br><br>4. 包装统一结果（如 `Result`）返回给前端|绝不能写复杂的业务逻辑（如 if-else 算各种金额），不能写 SQL。|
|**业务层**|**Service**|1. 系统的“大脑”，编写核心业务逻辑<br><br>  <br><br>2. 组合、编排多个 Mapper 数据<br><br>  <br><br>3. 负责数据对象的转换（如 DTO 转 DO，DO 转 VO）|绝不能处理 HTTP 请求参数，不能直接暴露给前端。|
|**持久层**|**Mapper** / DAO / Repository|1. 专注与数据库（MySQL/Redis等）交互<br><br>  <br><br>2. 执行 CRUD（增删改查）操作|绝不能包含业务逻辑计算。只负责“存”和“取”。|

---

## 二、 数据载体分类（各种实体 O）

系统的“血液”，统称为 POJO（Plain Old Java Object）。它们的作用是在不同分层之间安全、按需地传递数据。

|**实体类型**|**全称与含义**|**作用位置**|**核心作用与使用规范**|
|---|---|---|---|
|**DO / Entity**|**Data Object**<br><br>  <br><br>数据对象 / 实体类|`Mapper <-> 数据库`|**内部账本**。与数据库表结构**严格一一对应**。绝对不能通过接口直接返回给前端，防止密码、底薪等敏感数据泄露。|
|**DTO**|**Data Transfer Object**<br><br>  <br><br>数据传输对象|`前端 -> Controller -> Service`|**请求表单 / 防火墙**。专门用于接收前端传来的数据。只定义允许前端修改的字段，防止黑客通过“批量赋值漏洞”篡改系统核心字段（如 `isAdmin`）。|
|**VO**|**View Object**<br><br>  <br><br>视图对象|`Service -> Controller -> 前端`|**展示橱窗**。专门用于组装后返回给前端展示的数据。前端页面需要什么字段就给什么，屏蔽内部无关字段，节省带宽并提高安全性。|
|**Query**|**Query Object**<br><br>  <br><br>查询对象 (可选)|`前端 -> Controller -> Mapper`|专门用于封装复杂的查询条件，例如分页参数（`page`, `size`）、时间范围（`startTime`, `endTime`）、模糊搜索关键字等。|
|**BO**|**Business Object**<br><br>  <br><br>业务对象 (可选)|`Service 内部`|当业务极为复杂，需要把多个不同的 DO 或 DTO 组合在一起进行复杂计算时，会在 Service 内部临时使用。简单项目通常不需要。|

---

## 三、 标准数据流转全景图（Data Flow）

理解数据在分层中的变形过程，是掌握后端架构的关键：

### ⬆️ 1. 写入/新增场景（前端 -> 数据库）

1. 前端提交表单（JSON）。
    
2. **Controller** 用 **`DTO`** 接收数据。
    
3. Controller 将 **`DTO`** 传给 **Service**。
    
4. **Service** 将 **`DTO`** 转换为 **`DO (Entity)`**，并补充后端控制的字段（如创建时间、默认状态）。
    
5. **Service** 将 **`DO`** 传给 **Mapper**。
    
6. **Mapper** 将 **`DO`** 写入数据库。
    

### ⬇️ 2. 查询/展示场景（数据库 -> 前端）

1. **Controller** 接收请求参数（或用 **`Query`** 对象接收），调用 **Service**。
    
2. **Service** 调用 **Mapper** 查数据。
    
3. **Mapper** 从数据库查出完整数据，封装成 **`DO (Entity)`** 返回给 **Service**。
    
4. **Service** 出于安全和精简考虑，将 **`DO`** 中的可用数据提取出来，转化为 **`VO`**。
    
5. **Service** 将 **`VO`** 返回给 **Controller**。
    
6. **Controller** 将 **`VO`** 包装成 JSON 返回给前端。