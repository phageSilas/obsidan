### 🌟 核心前提：接口文档（The Contract）

在一切开始之前，前端和后端必须先有一份**接口文档**（如 Swagger/Apifox 导出）。 这份文档规定了：请求的 URL 是什么、是 GET 还是 POST/PUT、Headers 里的 `Content-Type` 是什么（如 `application/json`）、需要传什么参数、以及最后会返回什么结构的 JSON 数据。

---

### 🚀 第一阶段：前端发起请求 (Front-end Request)

1. **触发动作：** 用户在浏览器页面上修改了员工的名字，并点击了“保存修改”按钮。
    
2. **组装数据：** 前端代码（如 Vue/React）将用户输入的 `id` 和 `name` 打包成一个 JSON 对象：`{"id": 101, "name": "新名字"}`。
    
3. **设置请求头：** 前端在发起 HTTP 请求时，会在 Headers 中设置 `Content-Type: application/json`，告诉后端“我发给你的是 JSON 格式的数据”。
    
4. **发送请求：** 前端根据接口文档，向指定的 URL（例如 `PUT /api/employees/update`）发送网络请求。
    

---

### 🛡️ 第二阶段：后端接收与路由分发 (Controller Layer)

1. **拦截与匹配：** Spring Boot 接收到请求后，会去寻找哪个类的哪个方法头上标有 `@PutMapping("/api/employees/update")`。
    
2. **数据反序列化：** 找到对应的 `EmployeeController` 方法后，由于方法参数前加了 **`@RequestBody`** 注解，Spring 底层的 Jackson 工具会自动把前端传来的 JSON 字符串，**反序列化（解析）** 成对应的 Java 传输对象 **`EmployeeDTO`**。
    

---

### ⚙️ 第三阶段：业务逻辑与对象转换 (Service Layer)

1. **调用服务：** Controller 层不做具体的业务，它把 `EmployeeDTO` 传给 `EmployeeService` 接口的实现类。
    
2. **DTO 转 Entity（核心隔离）：** 为了防止数据库的敏感结构暴露，Service 层通常会使用工具类（如 `BeanUtils.copyProperties(dto, entity)`），将 DTO 中的数据拷贝到真正与数据库表对应的 **`Employee` (Entity 实体类)** 对象中。
    
    - _注意点：_ 实际开发中要注意处理 null 值覆盖的问题。
        
3. **调用持久层：** 数据准备完毕，Service 层调用 `EmployeeMapper` 接口准备修改数据库。
    

---

### 🗄️ 第四阶段：数据库交互 (Mapper/MyBatis Layer & DB)

1. **解析 SQL：** MyBatis 拿到 `Employee` 实体对象后，通过配置文件或注解，提取对象里的 `id` 和 `name`，生成最终的 SQL 语句：`UPDATE employee SET name = ? WHERE id = ?`。
    
2. **执行与返回（写操作）：** 数据库执行这条 Update 语句。由于是增删改等“写操作”，数据库底层只返回一个**整数 (`int`)**，代表“受影响的行数”（比如返回 `1` 表示修改成功了 1 行）。
    
    - _补充（读操作）：_ 如果是查询（Select），数据库返回的是 `ResultSet`（二维表格）。MyBatis 会利用 **Java 反射机制**，根据列名和属性名的对应关系，调用 `set` 方法把表格数据一行行“塞”进新的 Java 对象里，最后返回 `List` 或单个对象。
        
3. **业务判断：** Service 层根据 Mapper 返回的 `int` 是否大于 0，判断修改是否真正成功。
    

---

### 📦 第五阶段：后端原路返回 (Response Formatting)

1. **统一结果封装：** Service 层执行完毕后回到 Controller 层。Controller 通常不会直接扔个数字给前端，而是把结果包装进一个**“统一结果类 (Result)”**中。
    
2. **序列化回 JSON：** 组合好的 Result 对象（如包含状态码 `code`，提示语 `message`，甚至更新后的 `data`）在被 Controller 返回时，由于类上加了 **`@RestController`** 注解，Spring 会再次把它转换成 JSON 字符串。
    
    JSON
    
    ```
    {
      "code": 200,
      "message": "员工信息修改成功",
      "data": null
    }
    ```
    
3. **发送响应：** 携带这个 JSON 和 HTTP 状态码的响应报文，顺着网线飞回给前端的浏览器。
    

---

### 🖥️ 第六阶段：前端响应与页面更新 (UI Update)

1. **接收“发令枪”：** 前端的网络请求代码（如 Axios/Fetch）收到了后端的 JSON 响应。
    
2. **状态判断：** 前端代码判断 `if (response.data.code === 200)`。
    
3. **数据状态同步（关键！）：** 前端确认后端操作成功后，使用 JavaScript 开始修改页面显示，有两种常见策略：
    
    - **策略一（重新请求）：** 前端代码立刻再发一次 GET 请求，向后端要一份最新的员工列表，然后整体替换页面的表格。
        
    - **策略二（本地更新）：** 前端直接在浏览器的内存中，找到那个存着表格数据的数组，用代码修改/剔除掉对应的数据，Vue/React 会自动感应到数组变化，瞬间刷新页面的那一行。