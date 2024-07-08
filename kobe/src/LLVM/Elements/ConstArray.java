package LLVM.Elements;

import LLVM.Value;
import Types.Array;

import java.util.ArrayList;

public class ConstArray extends Element {
    private boolean isZeroInitializer;

    public ConstArray(Array arrayType) {
        super(null, arrayType, null);
        this.isZeroInitializer = true;
    }

    public ConstArray(ArrayList<Element> elements) {
        super(null, new Array(elements.get(0).getType(), elements.size()),
                null, elements.toArray(new Value[0]));
        this.isZeroInitializer = false;
    }

    public ArrayList<Integer> getValue() {
        ArrayList<Integer> value = new ArrayList<>();
        for (int i = 0; i < ((Array)getType()).getSize(); i++) {
            if (getUsedValue(i) instanceof ConstArray array) {
                value.addAll(array.getValue());
            } else if (getUsedValue(i) instanceof ConstInt constInt) {
                value.add(constInt.getValue());
            }
        }
        return value;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isZeroInitializer) {
            sb.append("zeroinitializer");
        } else {
            sb.append("[");
            for (int i = 0; i < ((Array)getType()).getSize(); i++) {
                sb.append(this.getUsedValue(i).getType());
                sb.append(" ");
                sb.append(this.getUsedValue(i));
                if (i < ((Array)getType()).getSize() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
