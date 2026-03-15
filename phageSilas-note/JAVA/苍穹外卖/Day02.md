# 建造者模式Builder()
## 基本使用
对于一个一般的用户类,构建其对象时一般是无参构造,有参构造+get/set方法;调用时一般也是new一个,然后通过get/set赋值
不过若用户类属性过于冗长,属性很多,那么调用时要多次调用set/get
belike:
``` java
// 1. 先 new 一个空盒子出来 Employee employee = new Employee(); // 2. 然后一行一行地塞数据
employee.setUsername(employeeDTO.getUsername()); employee.setName(employeeDTO.getName()); employee.setPhone(employeeDTO.getPhone()); // 不需要赋值的就不写，默认是 null
```
使用builder即可通过builder()开头,build()结尾的链式编程调用
belike:
``` java
// 假设前端传过来一个 employeeDTO，你想把它转换成你的 Employee 实体对象
Employee newEmployee = Employee.builder() // 🟢 第一步：开启建造
                .username(employeeDTO.getUsername()) // 🟡 第二步：各种赋值（顺序随你意）
                .name(employeeDTO.getName())
                .phone(employeeDTO.getPhone())
                // 假设这里故意不给 sex 和 idNumber 赋值，也是完全合法的
                .build();                        // 🔴 第三步：打包生成最终对象
```

## 使用前提
**现在一般通过lombok插件的注解@Builder直接写在用户类前代替手搓**
若不使用@Builder,就要修改原本的用户类中的构造方法
``` java
public class Employee {
    // 1. 目标类的属性（通常是 private 的，保证封装性）
    private String username;
    private String name;

    // 2. 核心：私有化构造方法！
    // 强制规定：外部不许直接 new Employee()，必须传一个 Builder 进来。
    private Employee(Builder builder) {
        this.username = builder.username;
        this.name = builder.name;
    }

    // 3. 提供一个静态的 builder() 方法，作为外界点餐的“入口”
    public static Builder builder() {
        return new Builder();
    }

    // ==========================================
    // 4. 重点：在内部定义一个静态的 Builder 助手类
    // ==========================================
    public static class Builder {
        // Builder 类里面要有一模一样的属性，用来做“暂存”
        private String username;
        private String name;

        // 【赋值方法】对应前面的 .username(...)
        public Builder username(String username) {
            this.username = username;
            return this; // ⬅️ 魔法的核心：把当前的 Builder 对象自己返回出去
        }

        // 【赋值方法】对应前面的 .name(...)
        public Builder name(String name) {
            this.name = name;
            return this; // ⬅️ 再次返回自己，实现链式调用
        }

        // 【收尾方法】对应前面的 .build()
        public Employee build() {
            // 把自己（this，也就是填满属性的 Builder）传给 Employee 的私有构造方法
            return new Employee(this); 
        }
    }
    
    // 省略 的getter 方法...
}

```

# 对象属性拷贝BeanUtils.copyProperties
## 使用场景
承接自上一条建造者模式,若两个类有较多的相同属性,如用户属性类和它对应的DTO类,可以通过Spring提供的该工具类实现
## 使用方法
``` java
// 1. 先 new 一个空的目标对象
Employee employee = new Employee();

// 2. 调用工具类进行拷贝 (源对象, 目标对象)
BeanUtils.copyProperties(employeeDTO, employee);
//会自动把 employeeDTO里的值全部塞进 employee 里。左➡️右
```

## 线程局部变量ThreadLocal
前提知识:**每发一次请求,那么后端所有层均算作同一个线程**,并且每次请求线程均不一样具有线程隔离特性
## 作用省流:
相当于单独把本次请求中的后端一个属性或者对象提取出来,然后可以在同一个线程中任意调用,并且由于每次请求在后端都算做同一个线程,**那么在本次请求中,我就可以任意调用各种参数/对象**.避免了想要获取某一个属性却需要层层传递并且还要接收许多冗余信息的情况

## 使用及举例
先写一个工具类BaseContext来封装ThreadLocal
``` java
public class BaseContext {  
  
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();  
  
    public static void setCurrentId(Long id) {  
        threadLocal.set(id);  
    }  
  
    public static Long getCurrentId() {  
        return threadLocal.get();  
    }  
  
    public static void removeCurrentId() {  
        threadLocal.remove();  
    }  
  
}
```
然后通过其中的setCurrentId()来获取某一步的参数,然后在其他步骤中通过getCurrentId()把获取到的参数放出来调用
获取:
``` java
Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());  
BaseContext.setCurrentId(empId);//通过ThreadLocal获取当前登录员工id
```
调用:
``` java
  
employee.setCreateUser(BaseContext.getCurrentId());//设置创建人  
employee.setUpdateUser(BaseContext.getCurrentId());
//把set获得到的empId放出来
```


# 分页查询插件PageHelper
``` java
PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
```
在Sevice的实现类中引入该方法,即可自动拆解字符串获取起始页和每页数据数量,并动态拼装sql语句,
**注意**:employeePageQueryDTO中封装着起始页码和每页数量,以及分页查询时的条件
若要使用该插件,其返回值要遵循其规则,是固定的返回值类型
``` java
Page<Employee> page = employeeMapper.page(employeePageQueryDTO);
//
```
**注意:** startPage必须紧邻查询语句(调用查询的接口)
## **过程省流**:
1) PageHelper.start方法获得起始页码和每页数量,并通过ThreadLocal存储这俩参数
2) Page<Employee'> page接收来自employeeMapper.page(employeePageQueryDTO);的返回值,使用集合是因为查询到的数据有多条
3) employeeMapper.page(employeePageQueryDTO);执行的时候,PageHelper会对查询语句进行拦截和操作,最终将篡改后的数据一条一条封装成集合返回给Page<Employee'>

## 原理
### 第一步：把分页参数塞进口袋
``` java
PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
```

- **表面上**：你调用了这个方法，传了页码和每页条数。
    
- **暗地里**：`PageHelper` 根本没有去碰数据库，它只是非常乖巧地把这两个参数封装成一个 `Page` 对象，然后**塞进了当前线程的 `ThreadLocal` 里面**。
    
- **此时状态**：准备工作完成，当前线程的“口袋”里装好了 `(pageNum=1, pageSize=10)`，等待后续使用。
    

### 第二步：业务查询与“半路打劫”

``` Java
Page<Employee> page = employeeMapper.page(employeePageQueryDTO);
```


当这行代码执行时，流程是这样的：

1. **生成原始 SQL**：MyBatis 根据你的 XML 或注解，生成了基础的 SQL，比如 `SELECT * FROM employee WHERE name = '张三'`。
    
2. **拦截器出场**：PageHelper 事先在 MyBatis 里安插了一个“拦截器”（你可以理解为城管）。在 SQL 真正发给 MySQL 之前，拦截器把它拦住了。
    
3. **摸口袋（ThreadLocal）**：拦截器去当前线程的 `ThreadLocal` 里摸了一下，发现：“哟，这哥们口袋里装了分页参数 `(pageNum=1, pageSize=10)`！”
    
4. **篡改 SQL（拼接 LIMIT）**：拦截器立刻把原始 SQL 进行了改造。
    
    - 它先偷偷帮你查了一次总数：`SELECT COUNT(*) FROM employee WHERE name = '张三'`。
        
    - 然后把原始 SQL 改写成分页 SQL：`SELECT * FROM employee WHERE name = '张三' LIMIT 0, 10`。
        
5. **执行并清理**：拦截器把改造后的 SQL 发给数据库执行，拿到结果后，它会做一件我们上一节强调过的保命操作——**调用 `ThreadLocal.remove()` 把口袋清空**，防止影响下一次请求。

_(注意：因为 MyBatis 拦截器返回的对象其实是被 PageHelper 包装过的一个叫 `Page` 的类，它继承了 `ArrayList`，所以你可以直接强转成 `Page<Employee>`。)_

### 第三步：包装返回结果

Java

```
return new PageResult(page.getTotal(), page.getResult());
```

最后一步就很简单了，你从刚才那个特殊的 `Page` 对象中，抽取出“总记录数（Total）”和“当前页的数据列表（Result）”，组装成前端需要的统一格式返回给用户。

# 消息转换器
由Spring MVC框架提供,写在Configuration配置类中,格式较固定