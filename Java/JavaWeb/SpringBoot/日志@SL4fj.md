## 等级
![[JavaWeb/SpringBoot/Attachment/image-3.png]]
## 技巧
1) 可以使用占位符 {}来避免"xxx:"+id导致的字符串拼接不美观
![[JavaWeb/SpringBoot/Attachment/image-4.png]]

2) lombok注解 **@SLf4j** 等效于
private static final Logger log=LoggerFactory.getLogger(xxxx.class);
对象名就是log