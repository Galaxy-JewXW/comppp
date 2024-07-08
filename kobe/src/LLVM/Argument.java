package LLVM;

import Types.Type;

public class Argument extends Value {
    public Argument(String name, Type type, Value parent) {
        super("%" + name, type, parent);
    }

    @Override
    public String toString() {
        return getName();
    }
}
