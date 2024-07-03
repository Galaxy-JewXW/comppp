package Values.Constants;

import Type.IntegerType;

public class ConstantInt extends Constant{
    // 整数常量
    private int val;

    public ConstantInt(int bits, int val) {
        super(Integer.toString(val), new IntegerType(bits), null);
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public String toString() {
        return this.getName();
    }
}
