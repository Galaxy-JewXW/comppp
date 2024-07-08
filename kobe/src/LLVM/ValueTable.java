package LLVM;

import Table.Table;

public class ValueTable extends Table<Value> {
    public ValueTable(ValueTable parent, boolean isRoot) {
        super(parent, isRoot);
    }
}
