package Type;

public class PointerType extends Type{
    private Type pointToType;

    public PointerType(Type pointToType) {
        this.pointToType = pointToType;
    }

    public Type getPointToType() {
        return pointToType;
    }

    @Override
    public String toString() {
        return pointToType.toString() + "*";
    }

}
