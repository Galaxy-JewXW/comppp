package LLVM;

import LLVM.Instructions.Instruction;
import Types.Label;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private final ArrayList<Instruction> instructions = new ArrayList<>();

    public BasicBlock(String name, Value parent) {
        super("%" + name, new Label(), parent);
    }

    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName().substring(1)).append(":\n");
        for (Instruction instruction : instructions) {
            sb.append("\t").append(instruction).append("\n");
        }
        if (!instructions.isEmpty()) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }
}
