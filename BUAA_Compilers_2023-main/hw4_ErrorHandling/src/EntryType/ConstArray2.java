package EntryType;

import java.util.ArrayList;

public class ConstArray2 {
    private int d1; // -1 表示是函数形参
    private int d2;
    private ArrayList<Integer> values;
    // private int[][] values;

    public ConstArray2(int d1, int d2) {
        this.d1 = d1;
        this.d2 = d2;
        this.values = new ArrayList<>();
    }

    public int getD2 () {
        return d2;
    }

    public void addValues(int value) {
        values.add(value);
    }
}
