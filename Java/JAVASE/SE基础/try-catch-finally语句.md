是Java中处理异常的核心机制，用于捕获和处理程序运行时可能出现的错误
## 基本语法
try {
	    // 可能抛出异常的代码
		} catch (异常类型1 变量名) {
	    // 处理异常类型1
		} catch (异常类型2 变量名) {
		    // 处理异常类型2
		} finally {
    // 无论是否发生异常，最终都会执行的代码
}

## 示例
   try {
            String str = null;
            System.out.println(str.length()); // NullPointerException
        } catch (NullPointerException e) {
            System.out.println("空指针异常");
        } finally {
            System.out.println("finally块始终执行");
        }
    }