#### Java中Character类的常用方法

Character字符函数使用：（括号里的都是char型数据）

| 运用                   | 方法                                         | 返回值                                              |
| ---------------------- | -------------------------------------------- | --------------------------------------------------- |
| 判断是不是字母         | .isLetter(‘a’)                               | Boolean值                                           |
| 判断是不是数字         | .isDigit(‘3’)                                | Boolean值                                           |
| 判断是不是字母或数字   | isLetterOrDigit(‘s’)                         | Boolean值                                           |
| 判断是不是空格         | .isWhitespace(’\\t’、’\\n’、’ ')             | Boolean值                                           |
| 判断是不是大写         | .isUpperCase(‘A’)                            | Boolean值                                           |
| 判断是不是小写         | .isLowerCase(‘a’)                            | Boolean值                                           |
| 判断是不是一个空格     | isWhitespace()                               | Boolean值                                           |
| 指定为大写             | .toUpperCase(‘a’)（字符串用 .toUpperCase()） | 返回字符（若不是英文，返回原字符）                  |
| 指定为小写             | .toLowerCase(‘A’)                            | 返回字符（若不是英文，返回原字符）                  |
| 获取字符ch的数值       | .getNumericValue(‘a’)                        | 返回字符ch的数值（a是10，z是35）                    |
| 获取当前字符的哈希表码 | .hashCode(‘a’)                               | 返回当前字符的哈希表码（效果Unicode字符集编码一样） |
| 转为字符串             | .toString(‘a’)                               | 返回字符串的形式                                    |



#### String 转Int

```java
String myString = "1234";
int foo = Integer.parseInt(myString);
```

#### Int 转 String

```java
String s = Integer.toString(i);
String s = String.valueOf(i);
```

#### ArrayList

**1\. List实例化**

```java
 /* 实例化List */
List<String> lists = new ArrayList<String>();
```

**2\. 添加元素**

（1）在List尾添加元素  
void add(E element)  
在List尾插入元素

```java
 /* 添加元素 */
lists.add("a");
lists.add("b");
lists.add("c");
lists.add("d");
```

（2）在指定位置添加元素  
void add(int index, E element)  
在指定位置插入元素，后面的元素都往后移一个元素。

```java
/* 将字符串添加到指定位置 */
lists.add(3, "e");
```

（3）插入其他集合全部的元素  
boolean addAll(int index, Collection<? extends E> c)  
在指定的位置中插入c集合全部的元素，如果集合发生改变，则返回true，否则返回false。  
意思就是当插入的集合c没有元素，那么就返回false，如果集合c有元素，插入成功，那么就返回true。

```java
/* 生成新list */
List<String> new_lists = new ArrayList<String>();
new_lists = lists.subList(0,2);
/* lists.addAll后新的lists */
lists.addAll(new_lists);
```

**3\. 修改元素**  
E set(int index, E element)  
在索引为index位置的元素更改为element元素

```java
 /* 修改元素 */
lists.set(4, "f");
```

**4\. 删除元素**  
E remove(int index)  
删除指定索引的对象

```java
 /* 删除元素(index或object) */
lists.remove(0);
lists.remove("b");
```

**5\. 查询元素**  
（1）指定位置范围内结果  
List subList(int fromIndex, int toIndex)  
返回从索引fromIndex到toIndex的元素集合，包左不包右（截取集合）

```java
 /* 生成新list */
List<String> new_lists = new ArrayList<String>();
new_lists = lists.subList(0,2);
```

（2）查找指定位置的元素  
E get(int index)  
返回list集合中指定索引位置的元素  
（3）查找元素的位置  
int indexOf(Object o)  
返回list集合中第一次出现o对象的索引位置，如果list集合中没有o对象，那么就返回-1  
lastIndexOf(Object o)  
返回list集合中最后一次出现o对象的索引位置，如果list集合中没有o对象，那么就返回-1

```java
/* 查询 */
String item = lists.get(1);
int n = lists.indexOf("c");
int n_last = lists.lastIndexOf("c");
System.out.println("get(位置为1的元素)：" + item + "  indexOf(c第一次出现的位置)：" + n 
+ "  indexOf(c最后一次出现的位置)：" + n_last);
```

（4）查看是否包含某个元素  
boolean contains(Object o)  
返回true或者false

```java
System.out.println("是否包含a：" + lists.contains("a"));
```

**6\. 清空List**

```java
/* 清空 */
lists.clear();
```

**7\. 输出List**

（1）直接输出

```java
/* 直接输出 */
System.out.println("直接输出：" + lists);
```

（2）普通输出

```java
/* 普通输出 */
Iterator<String> it = lists.iterator();
while (it.hasNext()) {
System.out.print(it.next() + " ");
}
System.out.println();
```

（3）Java8新特性输出

```java
/* Java8输出 */
lists.forEach((x) -> System.out.print(x + " "));
System.out.println();
```

**8\. 判断是否为空**  
boolean isEmpty()

```java
 System.out.println("lists是否为空：" + lists.isEmpty());
```