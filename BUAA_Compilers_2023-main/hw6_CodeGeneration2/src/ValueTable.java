import Values.Value;

import java.util.ArrayList;
import java.util.HashMap;

public class ValueTable {
    private boolean isRoot;
    private ValueTable parent;
    private ArrayList<ValueTable> children;
    private HashMap<String, Value> entries;

    public ValueTable(ValueTable parent, boolean isRoot) {
        this.isRoot = isRoot;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.entries = new HashMap<>();
    }

    public void addEntry(String name, Value entry) { // name是源语言的name，而非LLVM的name
        entries.put(name, entry);
    }

    public boolean containsEntry(String name) {
        return entries.containsKey(name);
    }

    public void addChildTable(ValueTable child) {
        children.add(child);
    }

    public ValueTable getParent() {
        return parent;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public int getSize() {
        return entries.size();
    }

    public boolean nameExisted(String name) {
        if (entries.containsKey(name)) {
            return true;
        }
        if (parent != null) {
            return parent.nameExisted(name);
        }
        return false;
    }

    public Value getEntry(String name) {
        if (entries.containsKey(name)) {
            return entries.get(name);
        }
        if (parent != null) {
            return parent.getEntry(name);
        }
        return null;
    }

    public void printEntriesName() {
        StringBuilder sb = new StringBuilder();
        for (String name : entries.keySet()) {
            sb.append(name);
            sb.append(" ");
            sb.append(entries.get(name));
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
}
