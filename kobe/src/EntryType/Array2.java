package EntryType;

import java.util.ArrayList;

public class Array2 extends FuncParam {
    private final int dimension1;
    private final int dimension2;
    private ArrayList<Integer> values;

    public Array2(int d1, int d2) {
        this.dimension1 = d1;
        this.dimension2 = d2;
        this.values = new ArrayList<>();
        this.dimension = 2;
    }

    public Array2(int d2) { // 作为函数形参
        this.dimension2 = d2;
        this.dimension1 = 0;
        this.values = new ArrayList<>();
        this.dimension = 2;
    }

    public int getValue(int i1, int i2) {
        return values.get(i1 * i2 + i2);
    }

    public int getDimension2() {
        return dimension2;
    }

    public int getType() {
        return dimension;
    }
}
