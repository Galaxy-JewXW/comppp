package LLVM.Elements;

import EntryType.Array1;
import EntryType.Array2;
import EntryType.FuncParam;
import EntryType.Var;
import LLVM.Argument;
import LLVM.BasicBlock;
import Types.*;

import java.util.ArrayList;
import java.util.Collections;

public class FunctionInt {
    private ArrayList<FuncParam> params = new ArrayList<>();
    private ArrayList<Type> paramsType = new ArrayList<>();
    private String name;
    private boolean isBuiltIn;
    private Type type;
    private ArrayList<BasicBlock> blocks = new ArrayList<>();
    private ArrayList<Argument> arguments = new ArrayList<>();
    private Function parent;

    public FunctionInt() {

    }

    public FunctionInt(String name, Type type, boolean isBuiltIn, Function parent, Type... params) {
        this.name = "@" + name;
        this.type = type;
        this.isBuiltIn = isBuiltIn;
        Collections.addAll(paramsType, params);
        this.parent = parent;
        int cnt = 0;
        for (Type arg : params) {
            arguments.add(new Argument("_" + cnt, arg, parent));
            cnt++;
        }
    }

    public FunctionInt(String name, Type type, boolean isBuiltIn, ArrayList<Type> params) {
        this.name = "@" + name;
        this.type = type;
        this.isBuiltIn = isBuiltIn;
        this.paramsType = new ArrayList<>(params);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getParamsSize() {
        return params.size();
    }

    public ArrayList<FuncParam> getFuncParams() {
        return params;
    }

    public ArrayList<Argument> getArguments() {
        return arguments;
    }

    public ArrayList<FuncParam> getParams() {
        return params;
    }

    public ArrayList<BasicBlock> getBlocks() {
        return blocks;
    }

    public ArrayList<Type> getParamsType() {
        return paramsType;
    }

    public String getName() {
        return name;
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        blocks.add(basicBlock);
    }

    public void setBuiltIn(boolean builtIn) {
        isBuiltIn = builtIn;
    }

    public void addParam(Var var) {
        params.add(var);
        arguments.add(new Argument("_" + (params.size() - 1), new Int(32), null));
    }

    public void addParam(Array1 array1) {
        params.add(array1);
    }

    public void addParam(Array2 array2) {
        params.add(array2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String pre = isBuiltIn ? "declare" : "define";
        sb.append(pre).append(" ");
        sb.append(type).append(" ").append(name).append("(");
        if (isBuiltIn) {
            for (int i = 0; i < paramsType.size(); i++) {
                sb.append(paramsType.get(i));
                if (i < paramsType.size() - 1) {
                    sb.append(", ");
                }
            }
        } else {
            for (int i = 0; i < arguments.size(); i++) {
                sb.append(arguments.get(i).getType()).append(" ");
                sb.append(arguments.get(i));
                if (i < arguments.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        sb.append(")");
        if (!isBuiltIn) {
            sb.append(" {\n");
            for (BasicBlock block : blocks) {
                sb.append(block).append("\n");
            }
            sb.append("}");
        }

        return sb.toString();
    }
}
