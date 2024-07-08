package LLVM.Instructions;

import LLVM.Value;
import Types.Array;
import Types.Pointer;
import Types.Type;
import Types.Void;

public class MemoryInstruction extends Instruction {
    private MemoryType memoryType;
    private Type arrayType;

    public MemoryInstruction(String name, Type type, Value parent, MemoryType memoryType, Value... values) {
        super("%_" + name, type, parent, values);
        this.memoryType = memoryType;
        int l = values.length;
        if (memoryType == MemoryType.Store) {
            this.name = "";
            this.type = new Void();
        } else if (memoryType == MemoryType.Alloca) {
            this.type = new Pointer(type);
        } else if (memoryType == MemoryType.Getelementptr) {
            if (type instanceof Array array) {
                if (l == 2) {
                    this.type = new Pointer(type);
                    this.arrayType = type;
                } else if (l == 3) {
                    this.type = new Pointer(array).getObjectType();
                    this.arrayType = type;
                }
            } else if (type instanceof Pointer pointer) {
                if (l == 2) {
                    this.type = type;
                    this.arrayType = pointer.getObjectType();
                } else if (l == 3) {
                    this.type = new Pointer(((Array) ((Pointer) type).getObjectType()).getElementsType());
                    this.arrayType = pointer.getObjectType();
                }
            }
        }
    }

    @Override
    public String toString() {
        if (memoryType == MemoryType.Load) {
            return getName() + " = load " + getType()
                    + ", " + getUsedValue(0).getType()
                    + " " + getUsedValue(0).getName();
        } else if (memoryType == MemoryType.Store) {
            return "store " + getUsedValue(0).getType() +
                    " " + getUsedValue(0).getName() + ", " +
                    getUsedValue(1).getType() + " " + getUsedValue(1).getName();
        } else if (memoryType == MemoryType.Alloca) {
            return this.getName() + " = alloca " + ((Pointer)this.getType()).getObjectType();
        } else if (memoryType == MemoryType.ZextTo) {
            return this.getName() + " = zext " +
                    this.getUsedValue(0).getType() + " " + this.getUsedValue(0).getName() +
                    " to " + this.getType();
        } else if (memoryType == MemoryType.Getelementptr) {
            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append(" = getelementptr ").append(arrayType).append(", ");
            for (int i = 0; i < this.getValuesSize(); i++) {
                sb.append(getUsedValue(i).getType());
                sb.append(" ");
                sb.append(getUsedValue(i).getName());
                if (i < getValuesSize() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}
