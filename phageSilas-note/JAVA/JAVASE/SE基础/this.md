
首先认识成员变量与局部变量
![[Pasted image 20250718210356.png|544x389]]
成员变量:类中方法外的变量
局部变量:方法中的变量

## 就近原则
![[JAVA/JAVASE/SE基础/Attachment/image.png]]
若没有限制,调用时会优先调用最近的变量
## this
使用this就可以调用成员变量
![[JAVA/JAVASE/SE基础/Attachment/image-2.png]]
注意:不能使用name=name的写法,这是指局部变量等于局部变量,和成员变量无关

```java
public void setAge(Integer age) {
    this.age = age;  // this.age表示当前对象的age属性
    // 左边的age是属性，右边的age是参数
    // this 指向调用该方法的对象
}
```