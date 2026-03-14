**ASC 升序     DESC降序**
若要同时使用where 和 order by
where需要在from之后,在order by 之前
SELECT 1,2
FROM A
WHERE 1>='a'
ORDER BY 2 DESC;

## 二级排序
用逗号连接即可
ORDER BY 1 ASC,2DESC;
