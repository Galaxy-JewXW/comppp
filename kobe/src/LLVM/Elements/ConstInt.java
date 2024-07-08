package LLVM.Elements;

import Types.Int;

public class ConstInt extends Element {
    private int value;

    public ConstInt(int size, int value) {
        super(Integer.toString(value), new Int(size), null);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
        return this.getName();
    }
}
