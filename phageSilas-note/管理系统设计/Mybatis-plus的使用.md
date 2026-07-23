# MyBatis-Plus 使用方法和项目规范

下面是基于当前短链接项目整理出来的 MyBatis-Plus 使用方式。重点参考了：

- [pom.xml](/D:/IDEA-java/mading_shortLink/pom.xml)
- [ShortLinkApplication.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/ShortLinkApplication.java)
- [DataBaseConfiguration.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/config/DataBaseConfiguration.java)
- [MyMetaObjectHandler.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/config/MyMetaObjectHandler.java)
- [BaseDO.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/common/database/BaseDO.java)
- [ShortLinkDO.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/dao/entity/ShortLinkDO.java)
- [ShortLinkMapper.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/dao/mapper/ShortLinkMapper.java)
- [ShortLinkServiceImpl.java](/D:/IDEA-java/mading_shortLink/project/src/main/java/com/nageoffer/shortlink/project/service/impl/ShortLinkServiceImpl.java)

## 1. 依赖引入

项目在父 `pom.xml` 中统一管理 MyBatis-Plus 版本：

```
<mybatis-plus.version>3.5.3.1</mybatis-plus.version>
```

业务模块中引入：

```
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

如果是单体项目，直接在当前项目的 `pom.xml` 中引入即可。

## 2. Mapper 扫描

启动类上使用：

```
@MapperScan("com.xxx.project.dao.mapper")
```

例如本项目：

```
@SpringBootApplication
@MapperScan("com.nageoffer.shortlink.project.dao.mapper")
public class ShortLinkApplication {
}
```

规范：

```
所有 Mapper 接口统一放在 dao.mapper 包下。
启动类使用 @MapperScan 统一扫描，不建议每个 Mapper 都加 @Mapper。
```

## 3. 实体类设计

实体类统一放在：

```
dao.entity
```

实体命名推荐：

```
UserDO
OrderDO
ShortLinkDO
```

其中 `DO` 表示 Data Object，对应数据库表。

示例：

```
@Data
@TableName("t_link")
public class ShortLinkDO extends BaseDO {

    private Long id;

    private String fullShortUrl;

    private String originUrl;

    private String gid;
}
```

## 4. 表名映射

如果类名和表名不完全一致，使用：

```
@TableName("t_user")
```

规范：

```
实体类必须明确标注 @TableName。
即使 MyBatis-Plus 能自动推断，也建议显式声明，避免表名前缀、下划线、复数形式导致误判。
```

## 5. 字段映射

普通字段如果符合驼峰转下划线规则，可以不写注解。

例如：

```
private String fullShortUrl;
```

会映射到：

```
full_short_url
```

特殊字段需要使用：

```
@TableField
```

本项目里有两个典型场景。

### 5.1 数据库关键字字段

```
@TableField("`describe`")
private String describe;
```

因为 `describe` 可能和 SQL 语义冲突，所以用反引号包起来。

### 5.2 非数据库字段

```
@TableField(exist = false)
private Integer todayPv;
```

表示这个字段不在当前表中，只是查询结果额外承接字段。

规范：

```
数据库不存在的字段必须加 @TableField(exist = false)。
SQL 关键字字段必须用 @TableField 显式指定。
```

## 6. 公共实体字段

项目把通用字段抽到 `BaseDO`：

```
@Data
public class BaseDO {

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
```

业务实体继承：

```
public class UserDO extends BaseDO {
}
```

规范：

```
所有需要持久化的业务实体都继承 BaseDO。
公共字段建议包含 createTime、updateTime、delFlag。
```

## 7. 自动填充

项目通过 `MetaObjectHandler` 自动填充公共字段：

```
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createTime", Date::new, Date.class);
        strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
        strictInsertFill(metaObject, "delFlag", () -> 0, Integer.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
    }
}
```

规范：

```
新增数据时自动填充 createTime、updateTime、delFlag。
修改数据时自动填充 updateTime。
业务代码里不要手动 setCreateTime、setUpdateTime。
```

## 8. Mapper 接口设计

Mapper 继承 `BaseMapper<T>`：

```
public interface UserMapper extends BaseMapper<UserDO> {
}
```

继承后自动拥有：

```
insert
deleteById
updateById
selectById
selectOne
selectList
selectPage
selectCount
```

规范：

```
简单 CRUD 不写 XML，直接使用 BaseMapper。
复杂 SQL、联表查询、聚合统计、自定义排序再写自定义 Mapper 方法。
```

## 9. Service 接口设计

Service 接口继承：

```
public interface UserService extends IService<UserDO> {
}
```

Service 实现类继承：

```
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO>
        implements UserService {
}
```

继承 `ServiceImpl` 后，可以直接使用：

```
baseMapper.insert(entity);
baseMapper.selectOne(wrapper);
save(entity);
updateById(entity);
list(wrapper);
getById(id);
```

规范：

```
业务 Service 继承 IService。
业务 ServiceImpl 继承 ServiceImpl<Mapper, Entity>。
复杂业务仍然写在 ServiceImpl 中，不要把业务逻辑塞进 Mapper。
```

## 10. 查询条件写法

项目大量使用：

```
LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
        .eq(UserDO::getUsername, username)
        .eq(UserDO::getDelFlag, 0);
```

优点是字段引用安全，重构字段名时不容易出错。

推荐：

```
Wrappers.lambdaQuery(Entity.class)
```

不推荐：

```
new QueryWrapper<Entity>().eq("username", username)
```

除非是聚合查询、别名查询、动态 SQL 场景。

规范：

```
普通查询优先使用 LambdaQueryWrapper。
不要手写字符串字段名。
查询未删除数据时必须带 delFlag = 0。
```

## 11. 更新条件写法

项目使用：

```
LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
        .eq(GroupDO::getUsername, UserContext.getUsername())
        .eq(GroupDO::getGid, gid)
        .eq(GroupDO::getDelFlag, 0);

GroupDO groupDO = new GroupDO();
groupDO.setDelFlag(1);

baseMapper.update(groupDO, updateWrapper);
```

规范：

```
更新必须带明确 where 条件。
涉及用户数据时，where 条件必须带 userId 或 username，避免越权更新。
逻辑删除优先 update delFlag，不直接物理 delete。
```

## 12. 分页插件配置

项目配置了 MyBatis-Plus 分页插件：

```
@Configuration
public class DataBaseConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

规范：

```
使用分页前必须配置 PaginationInnerInterceptor。
MySQL 项目设置 DbType.MYSQL。
```

## 13. 分页请求设计

本项目的分页请求 DTO 直接继承 `Page<T>`：

```
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {

    private String gid;

    private String orderTag;
}
```

这样前端传：

```
{
  "current": 1,
  "size": 10,
  "gid": "xxx"
}
```

MyBatis-Plus 能自动识别分页参数。

规范建议：

```
简单项目可以让分页请求 DTO 继承 Page<T>。
更规范的项目可以自定义 PageRequest，再在 Service 中转换成 Page<T>。
```

## 14. 分页查询写法

普通分页：

```
IPage<LinkAccessLogsDO> page = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
```

返回 DTO 时使用：

```
IPage<RespDTO> result = page.convert(each -> BeanUtil.toBean(each, RespDTO.class));
```

本项目示例：

```
IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);

return resultPage.convert(each -> {
    ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
    result.setDomain("http://" + result.getDomain());
    return result;
});
```

规范：

```
Mapper 查询返回 DO。
Service 层负责 DO 转 RespDTO。
分页对象转换优先使用 IPage.convert()。
Controller 不直接返回 DO。
```

## 15. 自定义 XML SQL

复杂查询使用 Mapper 方法 + XML：

Mapper：

```
IPage<ShortLinkDO> pageLink(ShortLinkPageReqDTO requestParam);
```

XML：

``` sql
<select id="pageLink"
        parameterType="com.xxx.ShortLinkPageReqDTO"
        resultType="com.xxx.ShortLinkDO">
    SELECT t.*
    FROM t_link t
    WHERE t.gid = #{gid}
    AND t.del_flag = 0
    ORDER BY t.create_time DESC
</select>
```

MyBatis-Plus 会根据参数中的 `current`、`size` 自动拼分页。

规范：

```
单表简单查询用 Wrapper。
多表 join、聚合统计、复杂排序用 XML。
XML namespace 必须对应 Mapper 全限定名。
XML 方法 id 必须对应 Mapper 方法名。
```

## 16. 注解 SQL

项目里也使用了 `@Select`：

```
@Select("SELECT ...")
List<HashMap<String, Object>> listTopIpByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
```

规范：

```
短 SQL 可以用 @Select。
复杂 SQL、动态 SQL、foreach、choose 较多时优先 XML。
不要在注解里写过长 SQL，否则可读性会明显下降。
```

## 17. 参数绑定规范

单个对象参数建议：

```
method(@Param("param") ReqDTO requestParam);
```

SQL 中使用：

```
#{param.gid}
#{param.startDate}
```

多个参数建议全部加 `@Param`：

```
List<Map<String, Object>> selectUvTypeByUsers(
        @Param("gid") String gid,
        @Param("fullShortUrl") String fullShortUrl,
        @Param("userAccessLogsList") List<String> userAccessLogsList
);
```

规范：

```
自定义 Mapper 方法只要参数超过一个，就必须加 @Param。
XML 或注解 SQL 中参数名必须和 @Param 保持一致。
```

## 18. QueryWrapper 使用场景

项目中有一个聚合统计使用了普通 `QueryWrapper`：

```
QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
        .select("gid as gid, count(*) as shortLinkCount")
        .in("gid", requestParam)
        .eq("enable_status", 0)
        .eq("del_flag", 0)
        .eq("del_time", 0L)
        .groupBy("gid");

List<Map<String, Object>> list = baseMapper.selectMaps(queryWrapper);
```

这种场景需要写 SQL 片段、别名、聚合函数，`LambdaQueryWrapper` 不太方便。

规范：

```
普通字段条件用 LambdaQueryWrapper。
聚合函数、字段别名、selectMaps 场景可用 QueryWrapper。
QueryWrapper 中的字段字符串必须和数据库字段一致。
```

## 19. 插入数据

项目插入数据：

```
baseMapper.insert(shortLinkDO);
```

或者：

```
shortLinkGotoMapper.insert(linkGotoDO);
```

规范：

```
新增数据时不要手动设置 createTime、updateTime、delFlag。
依赖 MetaObjectHandler 自动填充。
唯一键冲突可以捕获 DuplicateKeyException 转业务异常。
```

## 20. 事务规范

项目在涉及多表写入时使用事务：

```
@Transactional(rollbackFor = Exception.class)
public ShortLinkCreateRespDTO createShortLink(...) {
    baseMapper.insert(shortLinkDO);
    shortLinkGotoMapper.insert(linkGotoDO);
}
```

规范：

```
多表写入必须加 @Transactional。
涉及数据库 + Redis 时，数据库事务只能保证数据库一致性，Redis 需要额外设计补偿或删除缓存策略。
rollbackFor 建议显式指定 Exception.class。
```

## 21. 逻辑删除规范

本项目没有使用 MyBatis-Plus 内置 `@TableLogic`，而是手动维护：

```
delFlag = 0 未删除
delFlag = 1 已删除
```

查询时手动加：

```
.eq(Entity::getDelFlag, 0)
```

删除时手动更新：

```
entity.setDelFlag(1);
baseMapper.update(entity, wrapper);
```

规范建议二选一：

```
方案一：手动 delFlag，所有查询必须显式加 delFlag = 0。
方案二：使用 @TableLogic，让 MyBatis-Plus 自动处理逻辑删除。
```

对于你自己的项目，我更建议使用 `@TableLogic`，更不容易漏条件。

## 22. 推荐项目结构

```
src/main/java/com/xxx/project
├── config
│   ├── MybatisPlusConfiguration.java
│   └── MyMetaObjectHandler.java
├── common
│   └── database
│       └── BaseDO.java
├── dao
│   ├── entity
│   │   └── UserDO.java
│   └── mapper
│       └── UserMapper.java
├── service
│   ├── UserService.java
│   └── impl
│       └── UserServiceImpl.java
├── dto
│   ├── req
│   └── resp
└── controller
```

## 23. 推荐编码规范

```
1. 实体类统一以 DO 结尾。
2. Mapper 统一继承 BaseMapper<DO>。
3. Service 统一继承 IService<DO>。
4. ServiceImpl 统一继承 ServiceImpl<Mapper, DO>。
5. Controller 不直接操作 Mapper。
6. Controller 不直接返回 DO。
7. 普通查询使用 LambdaQueryWrapper。
8. 修改和删除必须带完整 where 条件。
9. 用户维度数据必须带当前登录用户条件。
10. 分页返回使用 IPage<T> 或封装后的 PageResult<T>。
11. 多表复杂查询使用 XML。
12. XML SQL 必须注意 del_flag、enable_status 等业务状态条件。
13. 新增和修改时间由 MetaObjectHandler 自动填充。
14. 多表写入使用 @Transactional。
15. 不在 Mapper 中写业务判断，Mapper 只负责数据访问。
```

## 24. 你自己项目的最小可用模板

依赖：

```
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

启动类：

```
@SpringBootApplication
@MapperScan("com.xxx.project.dao.mapper")
public class Application {
}
```

配置类：

```
@Configuration
public class MybatisPlusConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

公共实体：

```
@Data
public class BaseDO {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
```

自动填充：

```
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        strictInsertFill(metaObject, "delFlag", () -> 0, Integer.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
```

实体：

```
@Data
@TableName("t_user")
public class UserDO extends BaseDO {

    private Long id;

    private String username;

    private String password;
}
```

Mapper：

```
public interface UserMapper extends BaseMapper<UserDO> {
}
```

Service：

```
public interface UserService extends IService<UserDO> {
}
```

ServiceImpl：

```
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO>
        implements UserService {

    public UserDO getByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);

        return baseMapper.selectOne(wrapper);
    }
}
```

## 25. 总结

这个项目的 MyBatis-Plus 使用方式可以总结成一句话：

```
简单 CRUD 交给 BaseMapper 和 ServiceImpl，复杂 SQL 交给 XML，公共字段交给 MetaObjectHandler，查询更新条件优先用 LambdaWrapper，分页交给 MybatisPlusInterceptor。
```

你自己做单体项目时，优先学这几块就够了：

```
@MapperScan
BaseMapper
IService / ServiceImpl
@TableName / @TableField
LambdaQueryWrapper / LambdaUpdateWrapper
Page / IPage
MetaObjectHandler
MybatisPlusInterceptor
@Transactional
```

这套打好以后，普通后台管理系统、业务系统、CRM、权限系统基本都能撑起来。