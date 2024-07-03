import Instructions.*;
import Type.IntegerType;
import Type.Type;
import Type.VoidType;
import Values.BasicBlock;
import Values.Constants.*;
import Values.Module;
import Values.Value;

import java.util.ArrayList;
import java.util.Objects;

public class IRPort {
    // private static Module module;
    public static int nameNumCounter = 0;

    public static TerminatorInstr buildCallWithReturn(BasicBlock parent, Function callee, Value... args) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        TerminatorInstr ans = new TerminatorInstr("_" + nameNum, callee.getType(), parent,
                TerminatorType.CallWithVal, args);
        parent.addInstruction(ans);
        return ans;
    }

    public static TerminatorInstr buildCallWithNoReturn(BasicBlock parent, Function callee, Value... args) {
        TerminatorInstr ans = new TerminatorInstr("", callee.getType(), parent,
                TerminatorType.CallNoVal, args);
        parent.addInstruction(ans);
        return ans;
    }

    public static BasicBlock buildBasicBlock(Function parent) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BasicBlock basicBlock = new BasicBlock("Function_" +
                parent.getName().substring(1) + "_BasicBlock_" + nameNum, parent);
        parent.addBasicBlock(basicBlock);
        return basicBlock;
    }

    public static Function buildFunctionInt(String name, boolean isBuiltIn, Module module, Function... existedFunc) {
        Function ans = new Function(name,new IntegerType(32), isBuiltIn);
        if (existedFunc != null && existedFunc.length > 0) {
            ans = existedFunc[0];
        }
        module.addFunction(ans);
        nameNumCounter = ans.getParamsNum();
        return ans;
    }

    public static Function buildFunctionVoid(String name, boolean isBuiltIn, Module module, Function... existedFunc) {
        Function ans = new Function(name, new VoidType(), isBuiltIn);
        if (existedFunc != null && existedFunc.length > 0) {
            ans = existedFunc[0];
        }
        module.addFunction(ans);
        nameNumCounter = ans.getParamsNum();
        return ans;
    }

    public static TerminatorInstr buildRetWithReturn(BasicBlock parent, Value returnVal) {
        TerminatorInstr ans = new TerminatorInstr("", returnVal.getType(), parent,
                TerminatorType.RetWithVal, returnVal);
        parent.addInstruction(ans);
        return ans;
    }

    public static TerminatorInstr buildRetNoReturn(BasicBlock parent) {
        TerminatorInstr ans = new TerminatorInstr("", new VoidType(), parent,
                TerminatorType.RetNoVal);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildAdd(BasicBlock parent, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ADD, lhs, rhs);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildSub(BasicBlock parent, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BinaryOperator ans = new BinaryOperator("_" + nameNum, new IntegerType(32), parent, BinaryOpType.SUB, lhs, rhs);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildMul(BasicBlock parent, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BinaryOperator ans = new BinaryOperator("_" + nameNum, new IntegerType(32), parent, BinaryOpType.MUL, lhs, rhs);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildDiv(BasicBlock parent, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BinaryOperator ans = new BinaryOperator("_" + nameNum, new IntegerType(32), parent, BinaryOpType.SDIV, lhs, rhs);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildMod(BasicBlock parent, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        BinaryOperator ans = new BinaryOperator("_" + nameNum, new IntegerType(32), parent, BinaryOpType.SREM, lhs, rhs);
        parent.addInstruction(ans);
        return ans;
    }

    public static BinaryOperator buildIcmp(BasicBlock parent, String icmpType, Value lhs, Value rhs) {
        int nameNum = nameNumCounter;
        nameNumCounter++;
        if (Objects.equals(icmpType, "eq")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_EQ, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else if (Objects.equals(icmpType, "ne")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_NE, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else if (Objects.equals(icmpType, "sgt")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_SGT, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else if (Objects.equals(icmpType, "sge")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_SGE, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else if (Objects.equals(icmpType, "slt")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_SLT, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else if (Objects.equals(icmpType, "sle")) {
            BinaryOperator ans = new BinaryOperator("_" + nameNum, lhs.getType(), parent, BinaryOpType.ICMP_SLE, lhs, rhs);
            parent.addInstruction(ans);
            return ans;
        } else {
            return null;
        }
    }

    public static MemoryInstr buildMemoryInstr(BasicBlock parent, Type type, MemoryType memoryOpType, Value... ops) {

        int nameNum = nameNumCounter;
        if (memoryOpType == MemoryType.Alloca || memoryOpType == MemoryType.Load) {
            nameNumCounter++;
        }
        MemoryInstr ans = new MemoryInstr("_" + nameNum, type, parent, memoryOpType, ops);
        parent.addInstruction(ans);
        return ans;
    }
}
