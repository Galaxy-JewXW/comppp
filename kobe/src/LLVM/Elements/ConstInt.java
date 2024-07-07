package LLVM.Elements;

import Type.Int;

public class ConstInt extends Element {
    private int value;

    public ConstInt(int value, int size) {
        super(Integer.toString(value), new Int(size), null);
    }

    public int getValue() {
        return value;
    }


}
