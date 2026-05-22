### super

![[JAVA基础笔试/基础/Attachment/image.png|588]]
-在Java中，当子类构造方法中使用super()显式调用父类构造方法时，必须将其放在构造方法的第一行。这是因为子类实例化时必须先完成父类的初始化。
-this()和super()不能同时出现在同一个构造函数中。因为它们都需要在第一行，且都是用于调用构造方法的语句，这两者在逻辑上是互斥的。一个构造方法要么调用父类构造方法(super())，要么调用本类的其他构造方法(this())。

## try
![[JAVA基础笔试/基础/Attachment/image-1.png]]
-try块后可以只跟finally块而不带catch块,
-其中catch和finally块至少要有其中一个,但不是必须要有catch块。当程序不需要捕获异常时,可以只使用try-finally结构,finally块中的代码无论是否发生异常都会执行,通常用于释放资源等清理工作。

## 字符串拼接
![[JAVA基础笔试/基础/Attachment/image-2.png|586x282]]
以加号前的类型为准,小括号优先级最高
String先出现，则其后的int统一当作String来拼接
若两个int先出现，则int会先执行运算
如果有括号，括优先级高于运算符


## 数组
![[JAVA基础笔试/基础/Attachment/image-3.png|586x427]]
A：char[][] 定义了二位字符数组。在Java中，**使用字符串对char数组赋值，必须使用toCharArray()方法进行转换**。所以A错误。

B、C：在Java中定义String数组，有**两种定义方式：String a[]和String[] a**。**所以B、C正确**。

D、E：数组是**一个引用类型变量** ，因此**使用它定义一个变量时，仅仅定义了一个变量** **，这个引用变量还未指向任何有效的内存** **，因此定义数组不能指定数组的长度**。**所以D、E错误**。

F：**Object类是所有类的父类**。**子类其实是一种特殊的父类，因此子类对象可以直接赋值给父类引用变量，无须强制转换，这也被称为向上转型。这体现了多态的思想**。**所以F正确**。

## 模板方法
![[image-5.png]]

## 继承
![[image-6.png|586x530]]
Java只支持单继承，实现多重继承三种方式：（1）直接实现多个接口 （2）扩展(extends)一个类然后实现一个或多个接口  （3）通过内部类去继承其他类

![[image-8.png]]

在Java中，向下转型（将父类引用指向的子类对象，显式转换为子类类型）可能失败并抛出 ClassCastException ，主要场景如下：
- 父类引用指向的实际对象类型，并非目标子类类型： 例如，父类 Animal 引用指向 Dog 对象（ Animal animal = new Dog(); ），若强行将其向下转型为 Cat 类型（ Cat cat = (Cat) animal; ），会因实际对象是 Dog 而非 Cat 而失败。 
- 父类引用指向的是父类自身的对象： 若父类引用直接指向父类实例（ Animal animal = new Animal(); ），此时无论试图将其转型为任何子类（如 Dog dog = (Dog) animal; ），都会失败，因为父类对象无法被当作子类对象处理。 简单说，向下转型成功的前提是：父类引用实际指向的对象，必须是目标子类的实例。否则就会转型失败，这也是为什么向下转型前通常建议用 instanceof 判断（如 if (animal instanceof Dog) { Dog dog = (Dog) animal; } ），以避免异常。
  
作者：QQDDBB  
链接：[https://www.nowcoder.com/exam/test/97009744/submission?pid=67353701](https://www.nowcoder.com/exam/test/97009744/submission?pid=67353701)  
来源：牛客网

## 值传递
![[image-7.png|539x751]]

1. 对于参数str:  
- str是String类型,在Java中String是不可变的  
- 方法中的str="world"只是改变了方法参数的引用,并不会影响到test1.str的值  
- 因此test1.str依然保持"hello"不变  
  
2. 对于参数ch:  
- ch是字符数组,数组是引用类型  
- 方法中的ch[0]='d'改变了数组实际内容  
- 由于方法参数ch和test1.ch指向同一个数组对象,所以原数组的第一个字符被改为'd'

new出来的对象都放在堆中，而没有new的都放在栈中。

