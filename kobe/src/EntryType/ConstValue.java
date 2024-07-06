package EntryType;

import EntryType.*;

public class ConstValue {
    // 用于常量初始化解析 (ConstInitVal)
    private String type; // 0: int, 1: Array1, 2: Array2
    private ConstVar var;
    private ConstArray1 array1;
    private ConstArray2 array2;

    public ConstValue(String type, ConstVar var) {
        this.type = type;
        this.var = var;
        this.array1 = null;
        this.array2 = null;
    }

    public ConstValue(String type, ConstArray1 array1) {
        this.type = type;
        this.var = null;
        this.array1 = array1;
        this.array2 = null;
    }

    public ConstValue(String type, ConstArray2 array2) {
        this.type = type;
        this.var = null;
        this.array1 = null;
        this.array2 = array2;
    }

    public ConstVar getVar() {
        return var;
    }

    public ConstArray1 getArray1() {
        return array1;
    }

    public ConstArray2 getArray2() {
        return array2;
    }

    public void addValues(int value) {
        if (type.equals("Array1")) {
            array1.addValues(value);
        }
        else if (type.equals("Array2")) {
            array2.addValues(value);
        }
    }

    public String getType() {
        return type;
    }
}
