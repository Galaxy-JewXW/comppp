package Table;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private final boolean isRoot;
    private final SymbolTable parent;
    private final ArrayList<SymbolTable> children = new ArrayList<>();
    private final HashMap<String, TableEntry> entries = new HashMap<>();

    public SymbolTable(SymbolTable parent, boolean isRoot) {
        this.isRoot = isRoot;
        this.parent = parent;
    }

    public void addEntry(String name, TableEntry entry) {
        entries.put(name, entry);
    }

    public boolean containsEntry(String name) {
        return entries.containsKey(name);
    }

    public void addChildTable(SymbolTable child) {
        children.add(child);
    }

    public SymbolTable getParent() {
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

    public TableEntry getEntry(String name) {
        if (entries.containsKey(name)) {
            return entries.get(name);
        } else if (parent != null) {
            return parent.getEntry(name);
        }
        return null;
    }
}

