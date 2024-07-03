package Values;

import STable.TableEntryType;
import Type.Type;

import java.util.ArrayList;

public class Value {
    private static int idCounter;
    private int id;
    protected String name;
    protected Type type;
    protected Value parent;

    protected ArrayList<User> users;

    public Value(String name, Type type, Value parent) {
        this.id = idCounter;
        idCounter++;
        this.type = type;
        this.parent = parent;
        this.users = new ArrayList<>();
        this.name = name;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
