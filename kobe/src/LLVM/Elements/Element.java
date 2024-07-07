package LLVM.Elements;

import LLVM.User;
import LLVM.Value;
import Type.Type;

public class Element extends User {
    public Element(String name, Type type, Value parent, Value... values) {
        super(name, type, parent, values);
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
