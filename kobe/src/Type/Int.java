package Type;

public class Int implements Type {
    private final int size;

    public Int(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "i" + size;
    }
}
