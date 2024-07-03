package Values;

import Type.IntegerType;
import Type.PointerType;
import Type.VoidType;
import Values.Constants.Function;
import Values.Constants.FunctionInt;
import Values.Constants.GlobalVariable;

import java.util.ArrayList;

public class Module {
    // 顶层模块
    private ArrayList<Function> functions;
    private ArrayList<GlobalVariable> globalVariables;

    public Module () {
        functions = new ArrayList<>();
        globalVariables = new ArrayList<>();

        addFunction(new Function("getint", new IntegerType(32), true));
        addFunction(new Function("putint", new VoidType(), true, new IntegerType(32)));
        addFunction(new Function("putch", new VoidType(), true, new IntegerType(32)));
        addFunction(new Function("putstr", new VoidType(), true, new PointerType(new IntegerType(8))));
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

    public void addGlobalVariable(GlobalVariable globalVariable) {
        globalVariables.add(globalVariable);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (GlobalVariable globalVariable : globalVariables) {
            sb.append(globalVariable.toString());
            sb.append("\n");
        }

        sb.append("\n");

        for (Function function : functions) {
            sb.append(function.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
