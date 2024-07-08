package LLVM.Instructions;

import LLVM.User;
import LLVM.Value;
import Types.Type;

public class Instruction extends User {
    public Instruction(String name, Type type, Value parent, Value... values) {
        super(name, type, parent, values);
    }
}
