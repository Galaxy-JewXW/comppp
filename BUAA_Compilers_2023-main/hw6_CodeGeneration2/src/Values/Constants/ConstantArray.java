package Values.Constants;

import Type.ArrayType;
import Values.Value;

import java.util.ArrayList;

public class ConstantArray extends Constant{
    private boolean isZeroInitializer; // 判断是否全零优化

    public ConstantArray(ArrayType arrayType) {
        super(null, arrayType, null);
        this.isZeroInitializer = true;
    }

    public ConstantArray(ArrayList<Constant> elements) {
        super(null, new ArrayType(elements.get(0).getType(), elements.size()), null,
                elements.toArray(new Value[0]));
        this.isZeroInitializer = false;
    }

    public ArrayList<Integer> getVal() {
        ArrayList<Integer> val = new ArrayList<>();
        for (int i = 0; i < ((ArrayType)this.getType()).getElementNum(); i++) {
            if (this.getUsedValue(i) instanceof ConstantArray) {
                val.addAll(((ConstantArray) this.getUsedValue(i)).getVal());
            } else {
                val.add(((ConstantInt) this.getUsedValue(i)).getVal());
            }
        }
        return val;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isZeroInitializer) {
            sb.append("zeroinitializer");
        } else {
            sb.append("[");
            for (int i = 0; i < ((ArrayType)this.getType()).getElementNum(); i++) {
                sb.append(this.getUsedValue(i).getType());
                sb.append(" ");
                sb.append(this.getUsedValue(i));
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append("]");
        }
        return sb.toString();
    }
}
