package EntryType;

import java.util.ArrayList;

public class Array2 extends FuncParam {
    private int d1; // -1 表示是函数形参
    private int d2;
    private ArrayList<Integer> values;

    public Array2(int d1, int d2) {
        this.d1 = d1;
        this.d2 = d2;
        this.values = new ArrayList<>();
        this.type = 2;
    }

    public Array2(int d2) { // 作为函数形参
        this.d2 = d2;
        this.d1 = 0;
        this.values = new ArrayList<>();
        this.type = 2;
    }

    public int getValue(int i1, int i2) {
        return values.get(i1 * i2 + i2);
    }

    public int getD2 () {
        return d2;
    }

    public int getType() {
        return type;
    }
}
