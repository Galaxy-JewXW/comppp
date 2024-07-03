package Values.Constants;

import Type.Type;
import Values.Argument;
import Values.BasicBlock;
import Type.FunctionType;
import Type.IntegerType;
import Type.VoidType;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class Function extends Constant{
    private FunctionInt functionInt;
    private FunctionVoid functionVoid;
    private int functionType; // 0 for int, 1 for void

    public ArrayList<Argument> getArguments() {
        if (functionType == 0) {
            return functionInt.getArguments();
        } else {
            return functionVoid.getArguments();
        }
    }

    public Function(String name, Type type, boolean isBuiltIn, Type... paramType) {
        super(name, type, null);
        if (type instanceof IntegerType) {
            functionInt = new FunctionInt(name, isBuiltIn, type, paramType);
            functionType = 0;
        } else {
            functionVoid = new FunctionVoid(name, isBuiltIn, paramType);
            functionType = 1;
        }
    }

    public Function(String name, boolean isBuiltIn, FunctionInt functionInt) {
        super(name, new IntegerType(32), null);
        this.functionInt = functionInt;
        this.functionInt.setName(name);
        this.functionType = 0;
        this.functionInt.setType(new IntegerType(32));
        this.functionInt.setBasicBlocks(new ArrayList<>());
        functionInt.setBuiltIn(isBuiltIn);
    }

    public Function(String name, boolean isBuiltIn, FunctionVoid functionVoid) {
        super(name, new VoidType(), null);
        this.functionVoid = functionVoid;
        this.functionVoid.setName(name);
        this.functionType = 1;
        this.functionVoid.setBasicBlocks(new ArrayList<>());
        functionVoid.setBuiltIn(isBuiltIn);
    }

    public int getFunctionType() {
        return functionType;
    }

    public String getName() {
        if (functionType == 0) {
            return functionInt.getName();
        } else {
            return functionVoid.getName();
        }
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        if (functionType == 0) {
            functionInt.addBasicBlock(basicBlock);
        } else {
            functionVoid.addBasicBlock(basicBlock);
        }
    }

    public int getParamsNum() {
        if (functionType == 0) {
            return functionInt.getParamsNum();
        } else {
            return functionVoid.getParamsNum();
        }
    }

    public String toString() {
        if (functionType == 0) {
            return functionInt.toString();
        } else {
            return functionVoid.toString();
        }
    }
}
