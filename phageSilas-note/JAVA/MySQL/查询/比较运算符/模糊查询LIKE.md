# 格式
SELECT 1 FROM  A where xx LIKE "n "

# (1)  %
==%表示省略或不确定的字符==
比如annual 就可以表示为 a% 或者 %a%, expert表示为%t或者%t%
即    **a%表示以a开头的**
    **%a%表示包含a的**
     **%a表示以a结尾的**

 可以包含多个比如%a%e%和%e%a%就表示既包含a也包含e
 **(注意:有前后关系)**

# (2)  _
==_ 表示一个省略或者不确定的字符==
比如 __ a%表示第2个字母是a的字符


# 注意
若想查询%或_ 本身,需要用到**转义字符** \
比如_ \ _ %表示第二个是_ 的字符

ESCAPE可自定义转义字符
比如 
SELECT * FROM files 
WHERE filename LIKE 'report $ _ %' ESCAPE '$' ;**(将 $ 定义为转义字符)**
表示report后接以_ 开头的,如report_123.txt  report_data.csv  report_final.pdf`