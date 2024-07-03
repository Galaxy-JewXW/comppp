package Type;

public class ArrayType extends Type {

    // 数组降一维之后类型
    // 数组最多“一维”
    private Type elementType; // 数组中的元素只能一个类型
    private int elementNum; // 数组中元素的个数

    public ArrayType(Type elementType, int elementNum) {
        this.elementType = elementType;
        this.elementNum = elementNum;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getElementNum() {
        return elementNum;
    }

    public String toString() {
        return "[" + this.elementNum + " x " + this.elementType.toString() + "]";
    }

}
