package EntryType;

import java.util.ArrayList;

public class Array1 extends FuncParam{
    private int d;  // 维度, = -1 表示是函数形参
    private ArrayList<Integer> values;

    public Array1(int d) {
        this.d = d;
        this.values = new ArrayList<>();
        this.type = 1;
    }

    public Array1() { // 作为函数形参
        this.d = 0;
        this.values = new ArrayList<>();
        this.type = 1;
    }

    public int getType() {
        return type;
    }

    public int getValue(int i) {
        return values.get(i);
    }
}
