package LLVM.Elements;

import Types.*;

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
    }
}
