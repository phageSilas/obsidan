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

修改原本的用户类中的构造方法
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

