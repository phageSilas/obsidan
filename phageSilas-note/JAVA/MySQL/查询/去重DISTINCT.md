# 格式
SELECT DISTINCT   Column-1 FROM table-A；
将A中的1列的所有重复行剔除，只留下1个

# 注意
## （1） 
若DISTINCT跟了多个列，则会同时去除这些列共有的重复行
比如 SELECT DISTINCT Co-1  Co-2  Co-3 FROM table-A；
## （2）
若是将DISTINCT插入在不同行中间，一般会报错
比如 SELECT Co-1 DINSTINCT Co-2 FROM table-A；
则之后去除2的重复行，而不去除1的，导致两列的行数不相等，无法同时返回而报错