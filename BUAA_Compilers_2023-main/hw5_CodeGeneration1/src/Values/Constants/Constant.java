package Values.Constants;

import Type.Type;
import Values.User;
import Values.Value;
import Type.IntegerType;
import Type.ArrayType;

public class Constant extends User {
    public Constant(String name, Type type, Value parent, Value... values) {
        super(name, type, parent, values);
    }

    public static Value getZeroConstant(Type type) {
        if (type instanceof IntegerType) {
            return new ConstantInt(32, 0);
        } else {  // ArrayType
            return new ConstantArray((ArrayType) type);
        }
    }
}
