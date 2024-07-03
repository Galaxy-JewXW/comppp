package Values.Constants;

import Values.Module;
import Type.Type;
import Type.PointerType;

public class GlobalVariable extends Constant {
    // 全局变量，全局数组，全局字符串
    private boolean isConst;
    private boolean isInit;
    private boolean isString;
    private Module parent;


    // 默认初值为零或者零数组
    public GlobalVariable(String name, Type type, Module module) {
        super("@" + name, new PointerType(type), null, Constant.getZeroConstant(type));
        this.parent = module;
        module.addGlobalVariable(this);
        isConst = false;
        isInit = false;
        isString = false;
    }

    public GlobalVariable(String name, Constant initVal, boolean isConst, Module module) {
        super("@" + name, new PointerType(initVal.getType()), null, initVal);
        this.parent = module;
        module.addGlobalVariable(this);
        this.isConst = isConst;
        isInit = true;
        isString = false;
    }

    public String toString() {
        return this.getName()
                + " = dso_local "
//                + " = "
                + (isConst ? "constant" : "global") + " "
                + ((PointerType) this.getType()).getPointToType().toString() + " "
                + getUsedValue(0).toString();
    }
}
