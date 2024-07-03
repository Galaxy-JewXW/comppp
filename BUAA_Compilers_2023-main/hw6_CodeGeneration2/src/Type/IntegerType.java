package Type;

public class IntegerType extends Type{
    private int bits;

    public IntegerType(int bits){
        this.bits = bits;
    }

    public int getBits(){
        return bits;
    }

    @Override
    public String toString(){
        return "i" + bits;
    }
}
