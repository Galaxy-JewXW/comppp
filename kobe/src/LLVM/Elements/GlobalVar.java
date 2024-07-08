package LLVM.Elements;

import LLVM.Value;
import Types.*;
import LLVM.Module;

public class GlobalVar extends Element {
    private boolean isConst = false;
    private Module parent;

    private static Value getZeroElement(Type type) {
        if (type instanceof Int) {
            return new ConstInt(32, 0);
        } else {
            return new ConstArray((Array) type);
        }
    }

    public GlobalVar(String name, Type type, Module module) {
        super("@" + name, new Pointer(type), null, getZeroElement(type));
        this.parent = module;
        module.addGlobalVar(this);
    }

    public GlobalVar(String name, Element initVal, boolean isConst, Module module) {
        super("@" + name, new Pointer(initVal.getType()), null, initVal);
        this.parent = module;
        module.addGlobalVar(this);
        this.isConst = isConst;
    }

    public Element getValue() {
        return (Element) this.getUsedValue(0);
    }

    @Override
    public String toString() {
        String pre = isConst ? "constant" : "global";
        return getName() + " = dso_local " +
                pre + " " +
                ((Pointer) getType()).getObjectType() + " " +
                getValue();
    }
}
