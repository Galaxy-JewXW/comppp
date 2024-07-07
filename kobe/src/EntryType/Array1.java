package EntryType;

import java.util.ArrayList;

public class Array1 extends FuncParam {
    private final int dimension;
    private ArrayList<Integer> values;

    public Array1(int d) {
        this.dimension = d;
        this.values = new ArrayList<>();
        this.type = 1;
    }

    public Array1() { // 作为函数形参
        this.dimension = 0;
        this.values = new ArrayList<>();
        this.type = 1;
    }

    public int getValue(int i) {
        return values.get(i);
    }
}
