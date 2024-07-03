package EntryType;

import java.util.ArrayList;

public class ConstArray1 {
    private int d;
    private ArrayList<Integer> values;

    public ConstArray1(int d) {
        this.d = d;
        this.values = new ArrayList<>();
    }

    public ConstArray1(int d, ArrayList<Integer> values) {
        this.d = d;
        this.values = values;
    }

    public void addValues(int value) {
        values.add(value);
    }

    public int getValue(int i) {
        return values.get(i);
    }
}
