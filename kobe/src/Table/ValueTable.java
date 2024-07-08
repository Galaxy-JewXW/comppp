package Table;

import LLVM.Value;

public class ValueTable extends Table<Value> {
    public ValueTable(ValueTable parent, boolean isRoot) {
        super(parent, isRoot);
    }
}
