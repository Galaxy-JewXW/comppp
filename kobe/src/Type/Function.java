package Type;

import java.util.ArrayList;

public class Function implements Type {
    private final ArrayList<Type> argsType;
    private final Type returnType;

    public Function(ArrayList<Type> argsType, Type returnType) {
        this.argsType = new ArrayList<>(argsType);
        this.returnType = returnType;
    }
}
