package Types;

public class Pointer implements Type {
    private final Type objectType;

    public Pointer(Type objectType) {
        this.objectType = objectType;
    }

    public Type getObjectType() {
        return objectType;
    }

    @Override
    public String toString() {
        return objectType.toString() + "*";
    }
}
