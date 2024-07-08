package LLVM;

import LLVM.Elements.Function;
import LLVM.Elements.GlobalVar;
import Types.Int;
import Types.Pointer;
import Types.Void;

import java.util.ArrayList;

public class Module {
    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<GlobalVar> globalVars = new ArrayList<>();

    public Module() {
        addFunction(new Function("getint", new Int(32), true));
        addFunction(new Function("putint", new Void(), true, new Int(32)));
        addFunction(new Function("putch", new Void(), true, new Int(32)));
        addFunction(new Function("putstr", new Void(), true, new Pointer(new Int(8))));
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public Function getFunction(String name) {
        for (Function function : functions) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }

    public void addGlobalVar(GlobalVar globalVar) {
        globalVars.add(globalVar);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (GlobalVar var : globalVars) {
            sb.append(var).append("\n");
        }
        sb.append("\n");
        for (Function function : functions) {
            sb.append(function).append("\n");
        }
        return sb.toString();
    }
}
