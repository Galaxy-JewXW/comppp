package LLVM;


import Types.Type;

import java.util.ArrayList;

public class Value {
    private static int counter = 0;
    private final int id;
    private final String name;
    private final Type type;
    private final Value parent;
    private final ArrayList<User> users = new ArrayList<>();

    public Value(String name, Type type, Value parent) {
        this.id = counter;
        counter++;
        this.type = type;
        this.name = name;
        this.parent = parent;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void addUser(User user) {
        users.add(user);
    }
}
