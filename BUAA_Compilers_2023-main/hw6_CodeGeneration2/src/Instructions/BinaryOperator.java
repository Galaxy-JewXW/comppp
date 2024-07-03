package Instructions;

import Type.Type;
import Values.BasicBlock;
import Values.Value;
import Type.IntegerType;

public class BinaryOperator extends Instruction{
    private BinaryOpType binaryOpType;

    public BinaryOperator(String name, Type type, BasicBlock parent,
                           BinaryOpType binaryOpType, Value... ops) {
        super("%" + name, type, parent, ops);
        if (binaryOpType == BinaryOpType.ICMP_EQ ||
        binaryOpType == BinaryOpType.ICMP_NE ||
        binaryOpType == BinaryOpType.ICMP_SGE ||
        binaryOpType == BinaryOpType.ICMP_SGT ||
        binaryOpType == BinaryOpType.ICMP_SLE ||
        binaryOpType == BinaryOpType.ICMP_SLT) {
            this.type = new IntegerType(1);
        }
        this.binaryOpType = binaryOpType;
    }

    public String toString() {
        switch (this.binaryOpType) {
            case ADD:
                return this.getName() + " = add " + this.getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SUB:
                return this.getName() + " = sub " + this.getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case MUL:
                return this.getName() + " = mul " + this.getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SDIV:
                return this.getName() + " = sdiv " + this.getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SREM:
                return this.getName() + " = srem " + this.getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_EQ:
                return this.getName() + " = icmp eq " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_NE:
                return this.getName() + " = icmp ne " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SGT:
                return this.getName() + " = icmp sgt " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SGE:
                return this.getName() + " = icmp sge " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SLT:
                return this.getName() + " = icmp slt " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SLE:
                return this.getName() + " = icmp sle " + this.getUsedValue(0).getType().toString()
                        + " " + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            default:
                return null;
        }
    }

}
