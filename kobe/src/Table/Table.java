package Table;

import java.util.ArrayList;
import java.util.HashMap;

public class Table<T> {
    private final boolean isRoot;
    private final Table<T> parent;
    private final ArrayList<Table<T>> children = new ArrayList<>();
    private final HashMap<String, T> entries = new HashMap<>();

    public Table(Table<T> parent, boolean isRoot) {
        this.isRoot = isRoot;
        this.parent = parent;
    }

    public void addEntry(String name, T value) {
        entries.put(name, value);
    }

    public boolean containsEntry(String name) {
        return entries.containsKey(name);
    }

    public void addChildTable(Table<T> child) {
        children.add(child);
    }

    public Table<T> getParent() {
        return parent;
    }

    public boolean haveName(String name) {
        if (entries.containsKey(name)) {
            return true;
        } else if (parent != null) {
            return parent.haveName(name);
        }
        return false;
    }

    public T getEntry(String name) {
        if (entries.containsKey(name)) {
            return entries.get(name);
        } else if (parent != null) {
            return parent.getEntry(name);
        }
        return null;
    }

    public void printNames() {
        StringBuilder sb = new StringBuilder();
        for (String name : entries.keySet()) {
            sb.append(name);
            sb.append(" ");
            sb.append(entries.get(name));
            sb.append("\n");
        }
        System.out.println(sb);
    }
}
