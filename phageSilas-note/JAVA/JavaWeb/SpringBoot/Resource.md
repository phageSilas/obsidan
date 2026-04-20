Maven 要求将资源文件（如 PDF、TXT、XML、属性配置文件等）默认放在 `src/main/resources` 目录下，主要是基于 Maven 的核心设计理念以及 Java 项目的构建和运行机制。

具体原因可以归结为以下几个核心维度：

### 1. 约定优于配置 (Convention over Configuration)

这是 Maven 最核心的设计哲学。在 Maven 出现之前（比如使用 Ant 构建项目的时代），开发者需要手动编写繁杂的脚本来告诉构建工具：源码在哪里、资源文件在哪里、编译后的文件放到哪里。

Maven 直接制定了一套**标准目录结构**。通过默认规定 `src/main/java` 存放 Java 源码，`src/main/resources` 存放资源文件，开发者在绝大多数情况下不需要在 `pom.xml` 中写任何路径配置，Maven 就能自动找到并处理这些文件。这极大地降低了团队的协作成本和项目的维护成本。


``` java
可以使用
Resource r =new  xxx("文件名")
将文件转为resource对象
```

### 2. 构建时的处理动作不同（编译 vs 复制）

Java 源码和非代码资源文件在项目的构建过程（Build Process）中，需要经历的操作是完全不同的：

- **`src/main/java` 目录**：由 `maven-compiler-plugin`（编译插件）负责处理。它会扫描里面的 `.java` 文件并**编译**成 `.class` 字节码文件。**默认情况下，Maven 会直接忽略该目录下的所有非 `.java` 文件**（比如你的 PDF 或 TXT 文件）。
    
- **`src/main/resources` 目录**：由 `maven-resources-plugin`（资源插件）负责处理。这里面的文件不需要编译，Maven 的动作非常简单：把它们原封不动地**复制**到输出目录中（有时也会在这个阶段进行占位符的过滤和替换）。
    

如果在开发时把 PDF 放在 `src/main/java` 下，默认情况下这些文件会被构建工具过滤掉，根本不会被打进最终的 JAR 包里，从而导致运行时出现 `FileNotFoundException` 或 `NullPointerException`。

并且放在resource下,代码导入文件时可以直接使用相对路径
``` java
new PagePdfDocumentReader("中二知识笔记.pdf", ...)
```
否则就要使用绝对路径
``` java
new PagePdfDocumentReader("xx/xx/xx/中二知识笔记.pdf", ...)
```

### 3. 运行时的统一类路径 (Classpath)

虽然在开发阶段，源码和资源文件被物理地隔离在两个不同的目录中，但在 Maven 构建完成后，它们会**“殊途同归”**。

执行 `mvn compile` 后，Maven 会将：

1. `src/main/java` 编译生成的 `.class` 文件
    
2. `src/main/resources` 中复制过来的所有资源文件
    

统一输出合并到同一个目标目录：**`target/classes`**。

在 Java 运行时，`target/classes` 目录就是程序的**根类路径 (Classpath Root)**。这种设计使得在代码中读取资源文件变得极其方便和一致。你可以直接使用 ClassLoader 加载：

Java

```
// 无论是在本地 IDE 运行，还是打成 JAR 包后运行，这段代码都能稳定找到文件
InputStream is = getClass().getClassLoader().getResourceAsStream("example.pdf");
```

### 4. 职责分离的工程规范 (Separation of Concerns)

从软件工程的管理角度来看，将“动态的业务逻辑代码”与“静态的资源文件/配置文件”进行物理隔离，是一种良好的架构实践。这使得项目目录结构更加清晰，开发者在阅读和浏览代码时，一眼就能区分哪些是代码逻辑，哪些是环境配置或静态资产。

---

> **💡 补充说明：可以改变默认行为吗？**
> 
> 虽然强烈建议遵循 Maven 的默认规范，但 Maven 也是极其灵活的。如果你接手了一个旧项目，资源文件（如 MyBatis 的 XML 映射文件或普通的 TXT）必须和 Java 代码放在一起，你可以通过在 `pom.xml` 中添加配置来覆盖默认规则：
> 
> XML
> 
> ```
> <build>
>     <resources>
>         <resource>
>             <directory>src/main/java</directory>
>             <includes>
>                 <include>**/*.pdf</include>
>                 <include>**/*.txt</include>
>             </includes>
>         </resource>
>     </resources>
> </build>
> ```
> 
> 这样 Maven 就会去 `src/main/java` 目录下把指定的资源文件也复制到输出目录中了。

