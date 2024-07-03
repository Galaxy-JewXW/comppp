package Values;

import Instructions.Instruction;
import Instructions.TerminatorInstr;
import Type.LabelType;
import Values.Value;

import java.util.ArrayList;
import java.util.Collection;

public class BasicBlock extends Value {
    // 基本块中含有指令的列表
    private ArrayList<Instruction> instructions = new ArrayList<>();

    public BasicBlock(String name, Value parent) {
        super("%" + name, new LabelType(), parent);
    }

    public void addInstruction(Instruction ans) {
        this.instructions.add(ans);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName().substring(1));
        sb.append(":");
        sb.append("\n");
        for (Instruction instruction : instructions) {
            sb.append("\t");
            sb.append(instruction.toString());
            sb.append("\n");
        }
        if (!instructions.isEmpty()) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }
}
