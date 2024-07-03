import Lexer.Token;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private boolean isRoot;
    private SymbolTable parent;
    private ArrayList<SymbolTable> children;
    private HashMap<String, TableEntry> entries;

    public SymbolTable(SymbolTable parent, boolean isRoot) {
        this.isRoot = isRoot;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.entries = new HashMap<>();
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

    public boolean isRoot() {
        return isRoot;
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

    public TableEntry getEntry(String name) {
        if (entries.containsKey(name)) {
            return entries.get(name);
        }
        if (parent != null) {
            return parent.getEntry(name);
        }
        return null;
    }
}

