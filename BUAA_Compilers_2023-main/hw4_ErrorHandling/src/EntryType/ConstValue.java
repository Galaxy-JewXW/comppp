package EntryType;

import EntryType.*;

public class ConstValue {
    // 用于常量初始化解析 (ConstInitVal)
    private int type; // 0: int, 1: Array1, 2: Array2
    private ConstVar var;
    private ConstArray1 array1;
    private ConstArray2 array2;

    public ConstValue(int type, ConstVar var) {
        this.type = type;
        this.var = var;
    }

    public ConstValue(int type, ConstArray1 array1) {
        this.type = type;
        this.array1 = array1;
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

    public ConstValue(int type, ConstArray2 array2) {
        this.type = type;
        this.array2 = array2;
    }



    public void addValues(int value) {
        if (type == 1) {
            array1.addValues(value);
        }
        else if (type == 2) {
            array2.addValues(value);
        }
    }

    public int getType() {
        return type;
    }
}
