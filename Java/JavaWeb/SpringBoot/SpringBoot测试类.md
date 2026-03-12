## 原理 
SpringBoot的
@Component将Dao和Service的实现类存入IOC容器
@Autowired将成员变量所需的bean对象赋值给该变量

## 测试类
自带的test文件夹下的xxTest类
构造方法上写注解@SpringBootTest
调用的成员方法上写注解@Test
![[JavaWeb/SpringBoot/Attachment/image.png]]
