package Instructions;

import Type.Type;
import Values.Value;
import Type.VoidType;
import Type.PointerType;

public class MemoryInstr extends Instruction{
    private MemoryType memoryType;

    public MemoryType getMemoryType() {
        return memoryType;
    }

    public MemoryInstr(String name, Type type, Value parent, MemoryType memoryType,
                       Value... operands) {
        super("%" + name, type, parent, operands);
        this.memoryType = memoryType;

        if (memoryType == MemoryType.Store) {
            this.name = "";
            this.type = new VoidType();
        } else if (memoryType == MemoryType.Alloca) {
            this.type = new PointerType(type);
        }
    }

    public String toString() {
        if (memoryType == MemoryType.Load) {
            return getName() + " = load " + getType().toString()
                    + ", " + getUsedValue(0).getType().toString()
                    + " " + getUsedValue(0).getName();
        } else if (memoryType == MemoryType.Store) {
            return "store " + getUsedValue(0).getType().toString() +
                    " " + getUsedValue(0).getName() + ", " +
                    getUsedValue(1).getType().toString() + " " + getUsedValue(1).getName();
        } else if (memoryType == MemoryType.Alloca) {
            return this.getName() + " = alloca " + ((PointerType )this.getType()).getPointToType();
        } else {
            return "";
        }
    }
}
