package STable;

public enum TableEntryType {
    Var, // 变量
    ConstVar, // 常量

    Array1, // 一维数组
    ConstArray1, // 一维常量数组
    Array2, // 二维数组
    ConstArray2, // 二维常量数组

    FunctionVoid, // 无返回值函数
    FunctionInt, // 有返回值函数

    ReferencedEntry, // used for function parameters,
    LabelType, // For Values.BasicBlock
}
