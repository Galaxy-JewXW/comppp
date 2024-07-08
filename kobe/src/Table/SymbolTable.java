package Table;


public class SymbolTable extends Table<TableEntry> {
    public SymbolTable(SymbolTable parent, boolean isRoot) {
        super(parent, isRoot);
    }
}

