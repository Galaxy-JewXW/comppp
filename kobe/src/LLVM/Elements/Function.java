package LLVM.Elements;

import LLVM.Argument;
import LLVM.BasicBlock;
import Types.*;
import Types.Void;

import java.util.ArrayList;

public class Function extends Element {
    private FunctionInt functionInt = null;
    private FunctionVoid functionVoid = null;
    private boolean isFunctionInt;

    public Function(String name, Type type, boolean isBuiltIn, Type... params) {
        super(name, type, null);
        if (type instanceof Int) {
            functionInt = new FunctionInt(name, type, isBuiltIn, this, params);
            isFunctionInt = true;
        } else {
            functionVoid = new FunctionVoid(name, isBuiltIn, this, params);
            isFunctionInt = false;
        }
    }

    public Function(String name, boolean isBuiltIn, FunctionInt functionInt) {
        super(name, new Int(32), null);
        this.functionInt = functionInt;
        this.functionInt.setName(name);
        this.functionInt.setBuiltIn(isBuiltIn);
        this.functionInt.setType(new Int(32));
        this.functionInt.setBlocks(new ArrayList<>());
        this.isFunctionInt = true;
    }

    public Function(String name, boolean isBuiltIn, FunctionVoid functionVoid) {
        super(name, new Void(), null);
        this.functionVoid = functionVoid;
        this.functionVoid.setName(name);
        this.functionVoid.setBuiltIn(isBuiltIn);
        this.functionVoid.setType(new Int(32));
        this.functionVoid.setBlocks(new ArrayList<>());
        this.isFunctionInt = false;
    }

    public boolean isFunctionInt() {
        return isFunctionInt;
    }

    public String getName() {
        return isFunctionInt ? functionInt.getName() : functionVoid.getName();
    }

    public void addBasicBlock(BasicBlock block) {
        if (isFunctionInt) {
            functionInt.addBasicBlock(block);
        } else {
            functionVoid.addBasicBlock(block);
        }
    }

    public int getParamsSize() {
        return isFunctionInt ? functionInt.getParamsSize() : functionVoid.getParamsSize();
    }

    public int getArgsSize() {
        return isFunctionInt ? functionInt.getArgsSize() : functionVoid.getArgsSize();
    }

    public ArrayList<Type> getParamsType() {
        return isFunctionInt ? functionInt.getParamsType() : functionVoid.getParamsType();
    }

    public ArrayList<Argument> getArguments() {
        return isFunctionInt ? functionInt.getArguments() : functionVoid.getArguments();
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return isFunctionInt ? functionInt.getBlocks() : functionVoid.getBlocks();
    }

    @Override
    public String toString() {
        return isFunctionInt ? functionInt.toString() : functionVoid.toString();
    }
}
