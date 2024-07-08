package LLVM.Instructions;

import LLVM.BasicBlock;
import LLVM.Value;
import Types.Type;

public class TerminatorInstruction extends Instruction {
    private TerminatorType terminatorType;

    public TerminatorInstruction(String name, Type type, BasicBlock parent,
                                 TerminatorType terminatorType, Value... values) {
        super(name, type, parent, values);
        this.terminatorType = terminatorType;
        if (terminatorType == TerminatorType.CallWithVal) {
            this.name = "%" + name;
        }
    }

    public boolean isBrOrRet() {
        return this.terminatorType == TerminatorType.BrNoCondition ||
                this.terminatorType == TerminatorType.BrWithCondition ||
                this.terminatorType == TerminatorType.RetWithVal ||
                this.terminatorType == TerminatorType.RetNoVal;
    }

    public boolean isRet() {
        return this.terminatorType == TerminatorType.RetWithVal ||
                this.terminatorType == TerminatorType.RetNoVal;
    }

    @Override
    public String toString() {
        if (terminatorType == TerminatorType.RetNoVal || terminatorType == TerminatorType.RetWithVal) {
            return retToString();
        } else if (terminatorType == TerminatorType.CallNoVal || terminatorType == TerminatorType.CallWithVal) {
            return callToString();
        } else if (terminatorType == TerminatorType.BrNoCondition || terminatorType == TerminatorType.BrWithCondition) {
            return branchToString();
        } else {
            return terminatorType.name();
        }
    }

    private String retToString() {
        if (terminatorType == TerminatorType.RetNoVal) {
            return "ret void";
        } else {
            return "ret " + getUsedValue(0).getType().toString() + " " + getUsedValue(0).getName();
        }
    }

    private String callToString() {
        StringBuilder sb = new StringBuilder();
        if (terminatorType == TerminatorType.CallNoVal) {
            sb.append("call void ");
        } else {
            sb.append(getName()).append(" = call ").append(getType()).append(" ");
        }
        sb.append(getUsedValue(0).getName());
        sb.append("(");
        for (int i = 1; i < getValuesSize(); i++) {
            sb.append(getUsedValue(i).getType()).append(" ").append(getUsedValue(i).getName());
            if (i < getValuesSize() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String branchToString() {
        if (terminatorType == TerminatorType.BrNoCondition) {
            return "br " + getUsedValue(0).getType() + " " + getUsedValue(0).getName();
        } else if (terminatorType == TerminatorType.BrWithCondition) {
            return "br " + getUsedValue(0).getType() + " " + getUsedValue(0).getName() +
                    ", " + getUsedValue(1).getType() + " " + getUsedValue(1).getName() +
                    ", " + getUsedValue(2).getType() + " " + getUsedValue(2).getName();
        } else {
            return "";
        }
    }
}
