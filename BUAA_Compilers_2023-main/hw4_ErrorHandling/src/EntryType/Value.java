package EntryType;

import EntryType.*;

public class Value {
    private int type; // 0: int, 1: Array1, 2: Array2
    private Var var;
    private Array1 array1;
    private Array2 array2;

    public Value(int type, Var var) {
        this.type = type;
        this.var = var;
    }

    public Value(int type, Array1 array1) {
        this.type = type;
        this.array1 = array1;
    }

    public Value(int type, Array2 array2) {
        this.type = type;
        this.array2 = array2;
    }
}
