package Instructions;

import Type.Type;
import Values.Value;
import Type.VoidType;
import Type.PointerType;
import Type.ArrayType;

public class MemoryInstr extends Instruction{
    private MemoryType memoryType;
    private Type baseType; // 数组类型 Getelementptr

    public MemoryType getMemoryType() {
        return memoryType;
    }

    public MemoryInstr(String name, Type type, Value parent, MemoryType memoryType,
                       Value... operands) {
        super("%" + name, type, parent, operands);
        this.memoryType = memoryType;
        int operandsLength = operands.length;

        if (memoryType == MemoryType.Store) {
            this.name = "";
            this.type = new VoidType();
        } else if (memoryType == MemoryType.Alloca) {
            this.type = new PointerType(type);
        } else if (memoryType == MemoryType.Getelementptr) {
            // 数组传参，操作数：基址，指针寻址下标
            // Value base, Value pointerIndex
            // pointIndex 和 ArrayIndex
           if ((type instanceof ArrayType) && operandsLength == 2) {
               this.type = new PointerType(type);
               this.baseType = type;
           } else if ((type instanceof PointerType) && operandsLength == 2) {
               this.type = type;
               this.baseType = ((PointerType) type).getPointToType();

               // getElementptr 每次只寻址一次，操作数：基址，指针寻址下标，数组寻址下标
           } else if ((type instanceof ArrayType) && operandsLength == 3) {
               this.type = new PointerType(((ArrayType) type).getElementType());
               this.baseType = type;
           } else if ((type instanceof PointerType) && operandsLength == 3) {
               this.type = new PointerType(((ArrayType) ((PointerType) type).getPointToType()).getElementType());
               this.baseType = ((PointerType) type).getPointToType();
           }
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
        } else if (memoryType == MemoryType.ZextTo) {
            return this.getName() + " = zext " +
                    this.getUsedValue(0).getType() + " " + this.getUsedValue(0).getName() +
                    " to " + this.getType();
        } else if (memoryType == MemoryType.Getelementptr) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.getName());
            stringBuilder.append(" = getelementptr ");
            stringBuilder.append(baseType);
            stringBuilder.append(", ");
            for (int i = 0; i < this.getUsedValuesNum(); i++) {
                stringBuilder.append(this.getUsedValue(i).getType());
                stringBuilder.append(" ");
                stringBuilder.append(this.getUsedValue(i).getName());
                stringBuilder.append(", ");
            }
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
            return stringBuilder.toString();
        }
        else {
            return "";
        }
    }
}
