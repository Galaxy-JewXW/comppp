package Type;

public class Array implements Type {
    private final Type elementsType;
    private final int size;

    public Array(Type elementsType, int size) {
        this.elementsType = elementsType;
        this.size = size;
    }

    public Type getElementsType() {
        return elementsType;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "[" + size + " x " + elementsType.toString() + "]";
    }
}
