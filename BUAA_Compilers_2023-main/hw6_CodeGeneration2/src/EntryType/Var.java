package EntryType;

public class Var extends FuncParam {
    private int value;

    public Var(int value) {
        this.value = value;
        this.type = 0;
    }

    public Var(String name) { // 作为函数形参
        this.type = 0;
        this.value = 0;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
