package EntryType;

import java.util.ArrayList;

public class FunctionInt {
    private ArrayList<FuncParam> funcParams;

    public FunctionInt() {
        funcParams = new ArrayList<>();
    }

    public void addVarParam(Var var) {
        funcParams.add(var);
    }

    public void addArray1Param(Array1 array1) {
        funcParams.add(array1);
    }

    public void addArray2Param(Array2 array2) {
        funcParams.add(array2);
    }

    public int getParamsNum() {
        return funcParams.size();
    }

    public ArrayList<FuncParam> getFuncParams() {
        return funcParams;
    }
}
