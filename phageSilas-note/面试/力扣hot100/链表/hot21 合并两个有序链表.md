将两个升序链表合并为一个新的 **升序** 链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。 

**示例 1：**

![|586x270](https://assets.leetcode.com/uploads/2020/10/03/merge_ex1.jpg)

**输入：** l1 = [1,2,4], l2 = [1,3,4]
**输出：**[1,1,2,3,4,4]

**示例 2：**

**输入：** l1 = [], l2 = []
**输出：**[]

**示例 3：**

**输入：** l1 = [], l2 = [0]
**输出：**[0]

**提示：**

- 两个链表的节点数目范围是 `[0, 50]`
- `-100 <= Node.val <= 100`
- `l1` 和 `l2` 均按 **非递减顺序** 排列



## 完整 Java 代码

```java
/**
 * Definition for singly-linked list.
 * 单链表节点的定义，每个节点包含一个值和指向下一个节点的引用
 */
public class ListNode {
    int val;        // 节点存储的值
    ListNode next;  // 指向下一个节点的指针（引用）
    
    ListNode() {}   // 无参构造方法
    
    ListNode(int val) { 
        this.val = val; 
    }  // 带值构造方法
    
    ListNode(int val, ListNode next) { 
        this.val = val; 
        this.next = next; 
    }  // 带值和下一个节点的构造方法
}

public class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // ========== 第1步：创建一个虚拟头节点（哨兵节点）==========
        // 原因：我们不知道新链表的第一个节点是谁（可能是list1的头，也可能是list2的头）
        // 虚拟头节点让我们可以统一处理"第一个节点"和"后续节点"的情况，避免特殊判断
        ListNode dummy = new ListNode(-1);
        
        // ========== 第2步：创建一个指针，始终指向新链表的最后一个节点 ==========
        // 原因：我们需要知道新链表的"尾部"在哪里，才能将新节点接上去
        // 初始时，新链表只有一个虚拟节点，所以prev指向dummy
        ListNode prev = dummy;
        
        // ========== 第3步：同时遍历两个链表，比较节点值大小 ==========
        // 原因：两个链表都是升序的，所以每次只需要比较当前两个链表的头节点
        // 较小的那个就是"当前所有未合并节点中最小的"，应该放到新链表末尾
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                // list1当前节点更小（或相等），把它接到新链表末尾
                prev.next = list1;
                // list1指针后移，准备比较下一个
                list1 = list1.next;
            } else {
                // list2当前节点更小，把它接到新链表末尾
                prev.next = list2;
                // list2指针后移，准备比较下一个
                list2 = list2.next;
            }
            // 新链表长度增加了，prev指针也要后移
            prev = prev.next;
        }
        
        // ========== 第4步：处理剩余节点 ==========
        // 原因：上面的while循环在"其中一个链表遍历完"时就结束了
        // 但另一个链表可能还有剩余节点（而且它们已经是有序的）
        // 因为两个原链表都是升序的，所以剩余部分直接接到末尾即可
        if (list1 != null) {
            prev.next = list1;
        }
        if (list2 != null) {
            prev.next = list2;
        }
        
        // ========== 第5步：返回结果 ==========
        // 原因：dummy是虚拟节点，它的next才是真正的新链表头节点
        // 返回 dummy.next，而不是 dummy
        return dummy.next;
    }
}
```

---

## 图解执行过程（对应你上传的图片）

```
初始状态：
list1:  1 → 2 → 4
list2:  1 → 3 → 4

dummy → ? （还不知道接谁）

第1轮：list1.val(1) <= list2.val(1)，接list1的1
dummy → 1(red) → ?
list1变成: 2 → 4
list2变成: 1 → 3 → 4

第2轮：list1.val(2) > list2.val(1)，接list2的1
dummy → 1(red) → 1(purple) → ?
list1变成: 2 → 4
list2变成: 3 → 4

第3轮：list1.val(2) <= list2.val(3)，接list1的2
dummy → 1(red) → 1(purple) → 2(red) → ?
...

最终结果：
dummy → 1(purple) → 1(red) → 2(red) → 3(purple) → 4(red) → 4(purple)
        ↑ 返回这个
```

---

## 核心思路总结

| 要点 | 解释 |
|------|------|
| **双指针** | `list1` 和 `list2` 两个指针同时往后走，每次取较小的 |
| **虚拟头节点** | 避免讨论"第一个节点是谁"的特殊情况，代码更简洁 |
| **尾指针** | `prev` 始终指向新链表最后一个节点，方便接新节点 |
| **处理剩余** | 一个链表走完后，另一个直接接上（因为已经有序） |

---

## 时间/空间复杂度

- **时间复杂度**：O(n + m)，n 和 m 是两个链表的长度，每个节点只访问一次
- **空间复杂度**：O(1)，只使用了几个指针，没有创建新节点（只是重新连接原有节点的 next 指针）

---

## 面试中可能会追问的

1. **递归写法怎么做？**（代码更短，但空间复杂度 O(n+m)）
2. **如果要求合并 k 个有序链表呢？**（用优先队列/分治）
3. **如果要求去重合并呢？**（相等时只接一个）

如果你需要递归版本或者想继续练习其他链表题，随时发给我！