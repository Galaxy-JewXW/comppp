## IRPort 各函数作用及参数说明

### isConst
标志全局变量应该写 constant(true) 或者 global(true)

### buildCallWithReturn
使用：最后一个参数是可变参数，可以传入任意个数的Value
但是一定要把函数传入，之后传入函数的参数

ArrayList 转 为可变参数：`ArrayList.toArray(new Value[0])`

``` java
curValue = IRPort.buildCallWithReturn(curBasicBlock, function, function);
```

### buildCallWithNoReturn
使用：最后一个参数是可变参数，可以传入任意个数的Value
但是一定要把函数传入，之后传入函数的参数

ArrayList 转 为可变参数：`ArrayList.toArray(new Value[0])`

``` java
IRPort.buildCallWithNoReturn(curBasicBlock, function, function, constNode/expNode);
```

### buildMemoryInstr
#### Store
使用：
``` java
MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store
                        , curValue, leftOp);
```
把curValue存到leftOp中

#### Load
