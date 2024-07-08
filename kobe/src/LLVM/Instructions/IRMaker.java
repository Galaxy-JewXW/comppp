package LLVM.Instructions;

import LLVM.BasicBlock;
import LLVM.Elements.Function;
import LLVM.Module;
import LLVM.Value;
import Types.Array;
import Types.Int;
import Types.Pointer;
import Types.Type;
import Types.Void;

import java.util.ArrayList;

public class IRMaker {
    private static int cnt = 0;

    public static BasicBlock makeBasicBlock(Function parent) {
        BasicBlock block = new BasicBlock("Func_" + parent.getName().substring(1)
                + "_Block_" + cnt, parent);
        cnt++;
        parent.addBasicBlock(block);
        return block;
    }
    
    public static Function makeNewFunctionInt(String name, boolean isBuiltIn, Module module, ArrayList<Type> args) {
        Function function = new Function(name, new Int(32), isBuiltIn, args.toArray(new Type[0]));
        module.addFunction(function);
        cnt = function.getArgsSize();
        return function;
    }

    public static Function makeNewFunctionVoid(String name, boolean isBuiltIn, Module module, ArrayList<Type> args) {
        Function function = new Function(name, new Void(), isBuiltIn, args.toArray(new Type[0]));
        module.addFunction(function);
        cnt = function.getArgsSize();
        return function;
    }
    
    public static Function makeFunctionInt(String name, boolean isBuiltIn, Module module, Function... funcs) {
        Function function;
        if (funcs != null && funcs.length > 0) {
            function = funcs[0];
        } else {
            function = new Function(name, new Int(32), isBuiltIn);
        }
        module.addFunction(function);
        cnt = function.getArgsSize();
        return function;
    }

    public static Function makeFunctionVoid(String name, boolean isBuiltIn, Module module, Function... funcs) {
        Function function;
        if (funcs != null && funcs.length > 0) {
            function = funcs[0];
        } else {
            function = new Function(name, new Void(), isBuiltIn);
        }
        module.addFunction(function);
        cnt = function.getArgsSize();
        return function;
    }

    public static OperatorInstruction makeAdd(BasicBlock parent, Value a, Value b) {
        OperatorInstruction instruction = new OperatorInstruction(
                Integer.toString(cnt), a.getType(), parent, OperatorType.ADD, a, b
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static OperatorInstruction makeSub(BasicBlock parent, Value a, Value b) {
        OperatorInstruction instruction = new OperatorInstruction(
                Integer.toString(cnt), new Int(32), parent, OperatorType.SUB, a, b
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static OperatorInstruction makeMul(BasicBlock parent, Value a, Value b) {
        OperatorInstruction instruction = new OperatorInstruction(
                Integer.toString(cnt), new Int(32), parent, OperatorType.MUL, a, b
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static OperatorInstruction makeDiv(BasicBlock parent, Value a, Value b) {
        OperatorInstruction instruction = new OperatorInstruction(
                Integer.toString(cnt), new Int(32), parent, OperatorType.SDIV, a, b
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static OperatorInstruction makeMod(BasicBlock parent, Value a, Value b) {
        OperatorInstruction instruction = new OperatorInstruction(
                Integer.toString(cnt), new Int(32), parent, OperatorType.SREM, a, b
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    private static Value checkIntType(BasicBlock parent, Value value) {
        if (((Int)value.getType()).getSize() == 1) {
            return makeZextTo(parent, value, new Int(32));
        } else {
            return value;
        }
    }

    public static OperatorInstruction makeIcmp(BasicBlock parent, String icmp, Value a, Value b) {
        String name = Integer.toString(cnt);
        cnt++;
        OperatorInstruction instruction = null;
        switch (icmp) {
            case "eq":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_EQ,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            case "ne":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_NE,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            case "sgt":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_SGT,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            case "sge":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_SGE,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            case "slt":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_SLT,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            case "sle":
                instruction = new OperatorInstruction(name, a.getType(), parent, OperatorType.ICMP_SLE,
                        checkIntType(parent, a), checkIntType(parent, b));
                break;
            default:
                break;
        }
        if (instruction != null) {
            parent.addInstruction(instruction);
        }
        return instruction;
    }

    public static MemoryInstruction makeZextTo(BasicBlock parent, Value zextVal, Type toType) {
        MemoryInstruction instruction = new MemoryInstruction(
                Integer.toString(cnt), toType, parent, MemoryType.ZextTo, zextVal
        );
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }
    
    public static MemoryInstruction makeMemoryInstruction(BasicBlock parent, Type type, 
                                                          MemoryType memoryType, Value... values) {
        int l = cnt;
        if (memoryType == MemoryType.Alloca || memoryType == MemoryType.Load) {
            cnt++;
        }
        MemoryInstruction instruction = new MemoryInstruction(Integer.toString(l), type, parent, memoryType, values);
        parent.addInstruction(instruction);
        return instruction;
    }

    public static MemoryInstruction makeGetElementptr(Array baseType, BasicBlock parent, Value base,
                                                      Value pointIndex, Value arrayIndex) {
        MemoryInstruction instruction = new MemoryInstruction(Integer.toString(cnt), baseType, parent,
                MemoryType.Getelementptr, base, pointIndex, arrayIndex);
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static MemoryInstruction makePointerGetElementptr(Pointer baseType, BasicBlock parent,
                                                             Value base, Value pointIndex) {
        MemoryInstruction instruction = new MemoryInstruction(Integer.toString(cnt), baseType, parent,
                MemoryType.Getelementptr, base, pointIndex);
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeRetNoReturn(BasicBlock parent) {
        TerminatorInstruction instruction = new TerminatorInstruction("", new Void(),
                parent, TerminatorType.RetNoVal);
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeRetWithReturn(BasicBlock parent, Value value) {
        TerminatorInstruction instruction = new TerminatorInstruction("", value.getType(),
                parent, TerminatorType.RetWithVal, value);
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeCallNoReturn(BasicBlock parent, Function function, Value... args) {
        TerminatorInstruction instruction = new TerminatorInstruction("", function.getType(),
                parent, TerminatorType.CallNoVal, args);
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeCallWithReturn(BasicBlock parent, Function function, Value... args) {
        TerminatorInstruction instruction = new TerminatorInstruction("_" + cnt, function.getType(),
                parent, TerminatorType.CallWithVal, args);
        cnt++;
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeBranchNoCond(BasicBlock parent, BasicBlock target) {
        TerminatorInstruction instruction = new TerminatorInstruction("", new Void(), parent,
                TerminatorType.BrNoCondition, target);
        parent.addInstruction(instruction);
        return instruction;
    }

    public static TerminatorInstruction makeBranchWithCond(BasicBlock parent, Value condition,
                                                           BasicBlock target1, BasicBlock target2) {
        TerminatorInstruction instruction = new TerminatorInstruction("", new Void(), parent,
                TerminatorType.BrWithCondition, condition, target1, target2);
        parent.addInstruction(instruction);
        return instruction;
    }
}
