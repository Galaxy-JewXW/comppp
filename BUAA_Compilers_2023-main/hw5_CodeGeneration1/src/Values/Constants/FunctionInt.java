package Values.Constants;

import EntryType.Array1;
import EntryType.Array2;
import EntryType.FuncParam;
import EntryType.Var;
import Type.Type;
import Values.Argument;
import Values.BasicBlock;
import Type.IntegerType;

import java.util.ArrayList;
import java.util.Collections;

public class FunctionInt {
    private ArrayList<FuncParam> funcParams;
    private String name;
    private boolean isBuiltIn;
    private Type type; // 指示整数的位数
    private ArrayList<BasicBlock> basicBlocks;
    private ArrayList<Type> paramsType;

    public void setName(String name) {
        this.name = name;
    }

    public void setBasicBlocks(ArrayList<BasicBlock> basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    private ArrayList<Argument> arguments = new ArrayList<>();

    public FunctionInt() {
        funcParams = new ArrayList<>();
    }

    public FunctionInt(String name, boolean isBuiltIn, Type type, Type... paramType) {
        this.name = "@" + name;
        this.isBuiltIn = isBuiltIn;
        funcParams = new ArrayList<>();
        basicBlocks = new ArrayList<>();
        this.type = type;
        this.paramsType = new ArrayList<>();
        Collections.addAll(this.paramsType, paramType);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void addVarParam(Var var) {
        funcParams.add(var);
        arguments.add(new Argument("_" + Integer.toString(funcParams.size() - 1),
                new IntegerType(32), null));
    }

    public ArrayList<Argument> getArguments() {
        return arguments;
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

    public void addBasicBlock(BasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String getName() {
        return name;
    }

    public void setBuiltIn(boolean builtIn) {
        isBuiltIn = builtIn;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isBuiltIn ? "declare " : "define ");
        sb.append(type.toString()).append(" ");
        sb.append(name);
        sb.append("(");
        if (!isBuiltIn) {
            for (int i = 0; i < funcParams.size(); i++) {
                if (funcParams.get(i) instanceof Var) {
                    sb.append(new IntegerType(32).toString()).append(" ");
                }
                sb.append(arguments.get(i));
                if (i != funcParams.size() - 1) {
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
