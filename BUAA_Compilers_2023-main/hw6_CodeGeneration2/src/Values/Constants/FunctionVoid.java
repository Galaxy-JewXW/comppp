package Values.Constants;

import EntryType.Array1;
import EntryType.Array2;
import EntryType.FuncParam;
import EntryType.Var;
import Type.Type;
import Values.Argument;
import Values.BasicBlock;

import java.util.ArrayList;
import java.util.Collections;
import Type.IntegerType;
import Type.VoidType;

// 三种类型参数：int param, int param[], int param[][3]
public class FunctionVoid {
    private ArrayList<FuncParam> funcParams;
    private String name;
    private boolean isBuiltIn;
    private ArrayList<BasicBlock> basicBlocks;
    private ArrayList<Type> paramsType;
    private ArrayList<Argument> arguments = new ArrayList<>();
    private Function parent;

    public FunctionVoid() {
        funcParams = new ArrayList<>();
    }

    public void setBuiltIn(boolean builtIn) {
        isBuiltIn = builtIn;
    }

    public FunctionVoid(String name, boolean isBuiltIn, Function parent, Type... paramType) {
        this.name = "@" + name;
        this.isBuiltIn = isBuiltIn;
        funcParams = new ArrayList<>();
        basicBlocks = new ArrayList<>();
        this.parent = parent;
        this.paramsType = new ArrayList<>();
        Collections.addAll(this.paramsType, paramType);
        int argCnt = 0;
        for (Type arg : paramType) {
            arguments.add(new Argument("_" + argCnt,
                    arg, this.parent));
            argCnt++;
        }
    }

    public void addVarParam(Var var) {
        funcParams.add(var);
        arguments.add(new Argument("_" + Integer.toString(funcParams.size() - 1),
                new IntegerType(32), null));
    }

    public void addArray1Param(Array1 array1) {
        funcParams.add(array1);
    }

    public void addArray2Param(Array2 array2) {
        funcParams.add(array2);
    }

    public int getParamsNum() {
        return funcParams.size();
    }

    public ArrayList<FuncParam> getFuncParams() {
        return funcParams;
    }

    public ArrayList<Type> getParamsType () {
        return paramsType;
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return new VoidType();
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Argument> getArguments() {
        return arguments;
    }

    public int getArgsNum() {
        return arguments.size();
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public void setBasicBlocks(ArrayList<BasicBlock> basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isBuiltIn ? "declare " : "define ");
        sb.append("void").append(" ");
        sb.append(name);
        sb.append("(");
        if (!isBuiltIn) {
            for (int i = 0; i < arguments.size(); i++) {
//                if (funcParams.get(i) instanceof Var) {
//                    sb.append(new IntegerType(32).toString()).append(" ");
//                }
                sb.append(arguments.get(i).getType().toString()).append(" ");
                sb.append(arguments.get(i));
                if (i != arguments.size() - 1) {
                    sb.append(", ");
                }
            }
        } else {
            for (int i = 0; i < paramsType.size(); i++) {
                sb.append(paramsType.get(i).toString());
                if (i != paramsType.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        sb.append(")");
        if (!isBuiltIn) {
            sb.append(" {");
            sb.append("\n");
            for (BasicBlock basicBlock : basicBlocks) {
                sb.append(basicBlock.toString());
                sb.append("\n");
            }
            sb.append("}");
        }
        return sb.toString();
    }
}
