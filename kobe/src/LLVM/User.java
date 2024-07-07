package LLVM;

import Type.Type;

import java.util.ArrayList;

public class User extends Value {
    private ArrayList<Value> values;

    public User(String name, Type type, Value parent, Value... values) {
        super(name, type, parent);
        this.values = new ArrayList<>();
        for (Value value : values) {
            this.values.add(value);
            value.addUser(this);
        }
    }

    public Value getUsedValue(int index) {
        return values.get(index);
    }

    public int getValuesSize() {
        return values.size();
    }
}
