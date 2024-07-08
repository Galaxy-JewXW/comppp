package LLVM.Instructions;

import LLVM.BasicBlock;
import LLVM.Value;
import Types.Int;
import Types.Type;

public class Operator extends Instruction {
    private OperatorType operatorType;

    public Operator(String name, Type type, BasicBlock parent, OperatorType operatorType, Value... values) {
        super("%_" + name, type, parent, values);
        this.operatorType = operatorType;
        if (operatorType == OperatorType.ICMP_EQ || operatorType == OperatorType.ICMP_NE
                || operatorType == OperatorType.ICMP_SGT || operatorType == OperatorType.ICMP_SGE
                || operatorType == OperatorType.ICMP_SLT || operatorType == OperatorType.ICMP_SLE) {
            this.type = new Int(1);
        }
    }

    public String toString() {
        return switch (this.operatorType) {
            case ADD -> this.getName() + " = add " + this.getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SUB -> this.getName() + " = sub " + this.getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case MUL -> this.getName() + " = mul " + this.getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SDIV -> this.getName() + " = sdiv " + this.getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case SREM -> this.getName() + " = srem " + this.getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_EQ -> this.getName() + " = icmp eq " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_NE -> this.getName() + " = icmp ne " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SGT -> this.getName() + " = icmp sgt " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SGE -> this.getName() + " = icmp sge " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SLT -> this.getName() + " = icmp slt " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
            case ICMP_SLE -> this.getName() + " = icmp sle " + this.getUsedValue(0).getType() + " "
                    + this.getUsedValue(0).getName() + ", " + this.getUsedValue(1).getName();
        };
    }
}
