**MyBatis-Plus (简称 MP)** 是一个基于 MyBatis 的增强工具。它的核心理念是“只做增强不做改变”，即在 MyBatis 的基础上扩展功能，而不影响其原有的使用方式。它可以极大地简化开发流程，提高开发效率。

在 Java 后端开发（尤其是 Spring Boot 生态）中，MyBatis-Plus 几乎已经成为操作关系型数据库的“标配”之一。

以下是关于 MyBatis-Plus 的详细介绍：

### 1. 核心特性

- **无侵入**：只做增强不做改变，引入它不会对现有工程产生影响，可以和传统的 MyBatis 平滑共存。
    
- **损耗小**：启动即会自动注入基本 CRUD（增删改查），性能基本无损耗，直接面向对象操作。
    
- **强大的 CRUD 操作**：内置通用的 Mapper、通用的 Service，仅仅通过少量配置即可实现单表大部分 CRUD 操作，更有强大的条件构造器（Wrapper），满足各类复杂查询。
    
- **支持 Lambda 语法**：通过 Lambda 表达式编写查询条件，可以有效防止 SQL 注入，同时也避免了手动输入字段名容易拼写错误的问题（如 `User::getName`）。
    
- **支持主键自动生成**：内置多种主键生成策略（如雪花算法 Snowflake、UUID 等），可自由配置。
    
- **内置分页插件**：基于 MyBatis 物理分页，开发者只需按普通查询来写，配置好插件后，传入 `Page` 对象即可自动实现分页。
    
- **支持逻辑删除**：支持在应用层直接配置逻辑删除（如将 `deleted` 字段标为 1 而不是真正从数据库删除数据），后续的查询/删除操作会自动加上逻辑删除的判断。
    
- **支持乐观锁**：通过 `@Version` 注解，轻松实现数据库乐观锁机制。
    
- **代码生成器**：提供代码生成器（AutoGenerator），可以根据数据库表结构，一键自动生成 Entity、Mapper、Service、Controller 等基础代码，极大解放双手。
    
---

### 2. 核心组件与概念

- **`BaseMapper<T>`**：MyBatis-Plus 提供的一个顶级 Mapper 接口。只要你的 Mapper 继承了它，就自动拥有了 `insert`、`deleteById`、`updateById`、`selectList` 等几十个单表操作方法，完全不需要写 XML 文件。
    
- **`IService<T>` / `ServiceImpl<M, T>`**：服务层（Service）的通用接口和实现类，对 `BaseMapper` 进行了进一步封装，提供了批量插入、批量更新等更丰富的方法。
    
- **`Wrapper` (条件构造器)**：这是 MP 最强大的功能之一。它允许你在 Java 代码中以面向对象的方式拼接 SQL 的 `WHERE` 条件。
    
    - `QueryWrapper`：用于查询和删除时的条件封装。
        
    - `UpdateWrapper`：用于更新时的条件封装。
        
    - `LambdaQueryWrapper`：推荐使用，通过 Lambda 语法避免硬编码字段名。
        

---

### 3. 快速上手示例

假设我们有一张用户表 `user`，在 Spring Boot 环境下使用 MyBatis-Plus 非常简单：

**第一步：定义实体类 (Entity)**
```Java
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user") // 映射数据库表名
public class User {
    @TableId // 标记主键
    private Long id;
    private String name;
    private Integer age;
    private String email;
}
```

**第二步：编写 Mapper 接口**

``` Java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 后，所有的单表 CRUD 方法都已经就绪，无需写任何 SQL 或 XML
}
```

**第三步：在业务中使用**

``` Java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public void testMyBatisPlus() {
        // 1. 插入一条数据
        User user = new User();
        user.setName("张三");
        user.setAge(25);
        userMapper.insert(user);

        // 2. 根据 ID 查询
        User fetchedUser = userMapper.selectById(1L);

        // 3. 使用 LambdaQueryWrapper 进行复杂查询 (查询年龄大于20岁且名字包含"张"的用户)
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(User::getAge, 20)
               .like(User::getName, "张");
        List<User> userList = userMapper.selectList(wrapper);
    }
}
```

### 总结

如果你面对的是大量单表操作的项目，**MyBatis-Plus 能帮你省去 80% 以上编写繁琐 SQL 和 XML 的时间**。对于复杂的多表联查，你依然可以像以前一样，在 Mapper.xml 中手写 SQL，享受 MyBatis 原生的灵活性。它是典型的“鱼与熊掌兼得”的框架。</M,>
# 常用操作
## (1)eq 表示查询条件,等同于where  
``` java
    LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)  
                    .eq(UserDO::getUsername, username);  
  
                    // 设置指定字段的相等条件  
                    eq(R column, Object val)  
                    // 根据条件设置指定字段的相等条件  
                    eq(boolean condition, R column, Object val)  
  
```
    上面的效果是: select * from user where username = ?  
  
## (2)set 表示更新条件  
``` java
Wrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)  
 .set(Objects.equals(reqParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, 0);  
```
    set的第一个参数是判断条件,如果第一个参数为true,则执行后面两个参数:要set的字段和set的值  
  
## (3)连续的eq间使用and连接  
``` java
    LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)  
            .eq(UserDO::getUsername, username)  
            .eq(UserDO::getPassword, password);  
            .set(UserDO::getSex, Sex);  
```
    以上的效果是:两个eq条件满足后,便对Sex字段进行更新  
  
## (4)在实体类的属性上使用@TableField(fill=FieldFill.INSERT)注解,表示该字段在插入时自动填充  
    @TableField(fill = FieldFill.INSERT_UPDATE),表示该字段在插入和更新时自动填充  
  
    注意配置自动填充配置类  
## (5)update  
``` java
 baseMapper.update(updateShortLinkDO, updateWrapperGidIsEqual)  
```
 满足第二个参数的条件,则更新第一个参数 

## 常见逻辑

``` java
LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, reqParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, reqParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(0)
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);
```

这短代码非常典型，它是 MyBatis-Plus 中使用 **`LambdaUpdateWrapper` 进行按条件更新**的标准写法。

为了让你更容易理解，我们先**拆解并解释你提供的这段代码**，然后系统地介绍一下 **LambdaWrapper** 的概念以及 **MyBatis-Plus 常用的持久层操作**。

---

### 一、 详细解释这段代码的运行逻辑

这段代码的最终目的是：**在数据库中寻找一条特定的短链接记录，并将其状态修改为“禁用”（或者说修改为特定的状态值 0）。**

我们逐段来看：

#### 1. 构造 WHERE 条件 (定位要修改的数据)
``` Java
LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
        .eq(ShortLinkDO::getFullShortUrl, reqParam.getFullShortUrl())
        .eq(ShortLinkDO::getGid, reqParam.getGid())
        .eq(ShortLinkDO::getEnableStatus, 1)
        .eq(ShortLinkDO::getDelFlag, 0);
```

- **`Wrappers.lambdaUpdate(ShortLinkDO.class)`**：创建了一个用于更新的 Lambda 条件构造器。
    
- **`.eq(...)`**：代表 SQL 中的 `=` (equal)。
    
- 这四行 `.eq` 联合起来，相当于在拼装 SQL 的 `WHERE` 语句。意思是：
    
    - 完整短链接 (`full_short_url`) 必须等于请求参数传来的值。
        
    - 分组ID (`gid`) 必须等于请求参数传来的值。
        
    - 当前的启用状态 (`enable_status`) 必须是 `1` (可能代表目前是启用状态)。
        
    - 删除标记 (`del_flag`) 必须是 `0` (代表未被逻辑删除)。
        

#### 2. 构造 SET 内容 (准备要修改的新值)
``` Java
ShortLinkDO shortLinkDO = ShortLinkDO.builder()
        .enableStatus(0)
        .build();
```

- 这里利用了 Lombok 的 `@Builder` 模式，创建了一个 `ShortLinkDO` 的实体对象。
    
- **注意：** 这个对象里**只有 `enableStatus` 字段被赋了值（值为 0）**，其他字段默认都是 `null`。
    
- 在 MyBatis-Plus 中，当把实体对象传给 `update` 方法时，**它默认只会把非 `null` 的字段拼接到 SQL 的 `SET` 语句中**。
    

#### 3. 执行更新操作

``` Java
baseMapper.update(shortLinkDO, updateWrapper);
```

- **`baseMapper.update(实体对象, 条件包装器)`**：这是 MyBatis-Plus 提供的通用更新方法。
    
- `shortLinkDO` 提供了要更新成什么样（`SET` 语句）。
    
- `updateWrapper` 提供了要更新哪些行（`WHERE` 语句）。
    

#### 💡 这段代码翻译成底层的 SQL 语句相当于：
``` SQL
UPDATE t_short_link 
SET enable_status = 0 
WHERE full_short_url = 'xxx' 
  AND gid = 'xxx' 
  AND enable_status = 1 
  AND del_flag = 0;
```

_(附带一提：除了新建实体类去传值，`LambdaUpdateWrapper` 还有另一种写法，直接使用 `.set()`，例如：`updateWrapper.set(ShortLinkDO::getEnableStatus, 0); baseMapper.update(null, updateWrapper);`，这两种效果是一样的。)_




---

### 二、 什么是 LambdaWrapper？

**Wrapper（条件构造器）** 是 MyBatis-Plus 的核心利器，用来动态拼接 SQL 条件。

而 **`LambdaWrapper`** 是它的进阶版，也是目前官方最推荐的用法。

**为什么推荐用 LambdaWrapper？**

以前写条件是这样的（硬编码字段名）：

``` Java
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("user_name", "张三"); // 这里的 "user_name" 是魔法值/字符串
```

**痛点**：如果你以后重构代码，把数据库的 `user_name` 改成了 `name`，编译器不会报错，只有等代码运行到这里才会报 SQL 语法错误。

**LambdaWrapper 解决了这个问题：**
``` Java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUserName, "张三"); // 传入的是 Java 8 的方法引用
```

**优势**：

1. **防误写**：直接引用实体类的 `get` 方法，底层利用反射自动将其转换为数据库列名（如把 `getUserName` 转换为 `user_name`）。
    
2. **利于重构**：如果实体类字段改名了，IDE 直接报错提示，保证了代码的类型安全（Type Safe）。
    

它主要分为两类：

- **`LambdaQueryWrapper`**：用于 `SELECT` 和 `DELETE` 操作，只拼接 `WHERE` 条件。
    
- **`LambdaUpdateWrapper`**：用于 `UPDATE` 操作，除了拼接 `WHERE` 条件，还可以通过 `.set(列, 值)` 拼接 `SET` 语句。
    

常用的方法有：`eq`(=), `ne`(!=), `gt`(>), `ge`(>=), `lt`(<), `le`(<=), `like`(模糊查询), `in`, `orderByAsc` 等。

---

## 详细解释update

我们可以把 `baseMapper.update(实体对象, Wrapper条件)` 这个操作形象地比喻为一次**精确打击**：

- **第二个参数 `updateWrapper`**：是“雷达瞄准系统”**，负责生成 `WHERE` 语句，告诉你**“去打谁”。
    
- **第一个参数 `shortLinkDO`**：是“弹头/载荷”**，负责生成 `SET` 语句，告诉你**“改成什么样”。
    

下面为你详细拆解 `shortLinkDO` 的原理，以及 MP 是如何知道该更新哪张表的。

---

### 一、 第一个参数 `shortLinkDO` 的作用与底层原理

#### 1. 它的核心作用：生成 SQL 的 `SET` 部分

在关系型数据库中，更新语句的格式是 `UPDATE 表名 SET 列名=新值 WHERE 条件`。

`shortLinkDO` 这个实体对象的作用，就是全权负责提供 `SET 列名=新值` 这部分内容。

#### 2. 底层运行原理：反射与“非空判断”

当你把 `shortLinkDO` 传给 `update()` 方法时，MyBatis-Plus 底层会执行以下一套极其聪明的逻辑：

- **第一步：反射解析对象**
    
    MP 底层通过 Java 的反射机制，扫描传入的这个 `shortLinkDO` 对象内部的所有属性（字段）。
    
- **第二步：非空过滤（核心规则）**
    
    这是 MP 最重要的特性之一：**默认情况下，MP 只会把“值不为 null”的字段拼接到 `SET` 语句中**。
    
    结合你之前的代码：
    ``` Java
    ShortLinkDO shortLinkDO = ShortLinkDO.builder()
            .enableStatus(0) // 只有这个字段被显式赋值了
            .build();        // 其他没赋值的字段，默认全都是 null
    ```
    
    MP 扫描后发现，这个对象里除了 `enableStatus` 是 `0`，其他的 `id`, `fullShortUrl`, `createTime` 等等全都是 `null`。因此，它**直接忽略**所有为 `null` 的字段。
    
- **第三步：字段名到列名的映射**
    
    MP 提取出非空的 `enableStatus` 字段后，会根据实体类上的 `@TableField` 注解，或者默认的“驼峰转下划线”规则，将其转换为数据库中的列名 `enable_status`。
    
- **第四步：拼接 SQL**
    
    最终，MP 带着这个转换结果去拼装 SQL：`SET enable_status = 0`。
    

**💡 为什么要这么设计？**

如果 MP 不做非空判断，而是把对象的所有字段都拼到 `SET` 里，那就会变成 `SET enable_status = 0, full_short_url = null, gid = null...`，这会把你数据库里原本好好的数据全部清空成 NULL！所以“忽略 null 值”是极其安全的防御性设计。

---

### 二、 这个 `update` 是怎么知道要修改哪张表的？

我们在调用 `baseMapper.update()` 的时候，既没有传表名，也没有写 SQL，那它是怎么知道要修改 `t_short_link`（假设表名是这个）这张表的呢？

这得益于 Spring Boot 启动时的“预加载与泛型推断机制”。它发生在代码运行之前（项目启动阶段）。

#### 第一步：通过泛型“顺藤摸瓜”

你的 Mapper 接口一定是这么定义的：
``` Java
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {
}
```

当项目启动时，MyBatis-Plus 会去扫描所有的 Mapper 接口。当扫描到 `ShortLinkMapper` 时，它看到了你继承了 `BaseMapper`，并且**泛型参数指定了 `<ShortLinkDO>`**。

这就建立了一个死绑定的关系：**这个 Mapper 专属于 `ShortLinkDO` 这个实体类**。

#### 第二步：解析实体类上的注解（寻找表名）

既然知道了实体类是 `ShortLinkDO`，MP 紧接着就会用反射去读取 `ShortLinkDO` 类的源码。

它会寻找类头部的 `@TableName` 注解：
``` Java
@TableName("t_short_link")  // <-- MP 找到了这个！
public class ShortLinkDO { ... }
```

- **如果写了 `@TableName("t_short_link")`**：MP 就直接把 `t_short_link` 作为这张表的表名。
    
- **如果没写 `@TableName` 注解**：MP 会启动默认策略，将类名 `ShortLinkDO` 做驼峰转下划线的处理，可能会推断表名为 `short_link_d_o`。
    

#### 第三步：将生成的 SQL 模板缓存起来

在项目启动的瞬间，MP 已经通过上面的步骤知道了：

1. 表名是 `t_short_link`。
    
2. 所有的列名是什么（通过扫描实体类的字段）。
    
3. 主键是哪个字段（通过寻找 `@TableId` 注解）。
    

有了这些信息，MP 就会在内存中为这个 Mapper 动态生成一套完整的 CRUD SQL 模板，并缓存到 MyBatis 的 `MappedStatement` 注册表中。

#### 第四步：运行时直接调用

当你真正在业务代码里执行 `baseMapper.update(shortLinkDO, updateWrapper)` 时，MP 根本不需要再去猜表名。它直接从启动时缓存好的元数据中提取出表名 `t_short_link`，然后把 `shortLinkDO` 解析出的 `SET` 语句，以及 `updateWrapper` 解析出的 `WHERE` 语句组装在一起，发给 MySQL 数据库。

### 总结

- **`shortLinkDO` 的作用**：充当数据载体，MP 通过反射获取其中非 null 的字段，动态生成 `SET` 语句。
    
- **如何知道表名**：基于泛型 `<ShortLinkDO>` 找到实体类 -> 读取实体类上的 `@TableName` 注解获取表名。这一切在 Spring 容器启动时就已经解析并缓存完毕了，运行时直接调用，效率极高。