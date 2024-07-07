package EntryType;

public class Var extends FuncParam {
    private int value;

    public Var() { // 作为函数形参
        this.type = 0;
        this.value = 0;
    }

    public int getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
