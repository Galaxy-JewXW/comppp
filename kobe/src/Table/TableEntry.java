package Table;

import EntryType.*;
import Parser.ASTNode;

import java.util.ArrayList;

public class TableEntry {
    private ASTNode node;
    private TableEntryType tableEntryType;

    private Var var;
    private ConstVar constVar;
    private Array1 array1;
    private ConstArray1 constArray1;
    private Array2 array2;
    private ConstArray2 constArray2;
    private FunctionVoid functionVoid;
    private FunctionInt functionInt;
    private ReferencedEntry referencedEntry;

    private TableEntry definedEntry;
    private ArrayList<TableEntry> funcRParams = null;

    public TableEntry() {

    }

    public TableEntry(TableEntry defineEntry, TableEntryType actualType, int d1) {
        this.tableEntryType = TableEntryType.ReferencedEntry;
        this.referencedEntry = new ReferencedEntry(actualType, defineEntry.getTableEntryType(), d1);
        this.definedEntry = defineEntry;
    }

    public TableEntry(TableEntry defineEntry, TableEntryType actualType, int d1, int d2) {
        this.tableEntryType = TableEntryType.ReferencedEntry;
        this.referencedEntry = new ReferencedEntry(actualType, defineEntry.getTableEntryType(), d1, d2);
        this.definedEntry = defineEntry;
    }

    public TableEntry(ASTNode node, Var var) {
        this.node = node;
        this.var = var;
        this.tableEntryType = TableEntryType.Var;
    }

    public TableEntry(ASTNode node, ConstVar constVar) {
        this.node = node;
        this.constVar = constVar;
        this.tableEntryType = TableEntryType.ConstVar;
    }

    public TableEntry(ASTNode node, Array1 array1) {
        this.node = node;
        this.array1 = array1;
        this.tableEntryType = TableEntryType.Array1;
    }

    public TableEntry(ASTNode node, ConstArray1 constArray1) {
        this.node = node;
        this.constArray1 = constArray1;
        this.tableEntryType = TableEntryType.ConstArray1;
    }

    public TableEntry(ASTNode node, Array2 array2) {
        this.node = node;
        this.array2 = array2;
        this.tableEntryType = TableEntryType.Array2;
    }

    public TableEntry(ASTNode node, ConstArray2 constArray2) {
        this.node = node;
        this.constArray2 = constArray2;
        this.tableEntryType = TableEntryType.ConstArray2;
    }

    public TableEntry(ASTNode node, FunctionVoid functionVoid) {
        this.node = node;
        this.functionVoid = functionVoid;
        this.tableEntryType = TableEntryType.FunctionVoid;
        this.funcRParams = new ArrayList<>();
    }

    public TableEntry(ASTNode node, FunctionInt functionInt) {
        this.node = node;
        this.functionInt = functionInt;
        this.tableEntryType = TableEntryType.FunctionInt;
        this.funcRParams = new ArrayList<>();
    }

    public TableEntry(int value) {
        this.tableEntryType = TableEntryType.ConstVar;
        this.constVar = new ConstVar(value);
    }

    public int funcParamsNum() {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            return functionVoid.getParamsSize();
        } else {
            return functionInt.getParamsSize();
        }
    }

    public ArrayList<FuncParam> getFuncParams() {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            return functionVoid.getFuncParams();
        } else {
            return functionInt.getFuncParams();
        }
    }

    public void clearFuncRParams() {
        this.funcRParams.clear();
    }

    public int getFuncRParamsSize() {
        return this.funcRParams.size();
    }

    public TableEntry getFuncRParam(int index) {
        return this.funcRParams.get(index);
    }

    public void addFuncRParam(TableEntry tableEntry) {
        this.funcRParams.add(tableEntry);
    }

    public boolean isReferencedEntry() {
        return this.tableEntryType == TableEntryType.ReferencedEntry;
    }

    public boolean isConst() {
        return this.tableEntryType == TableEntryType.ConstArray1
                || this.tableEntryType == TableEntryType.ConstArray2
                || this.tableEntryType == TableEntryType.ConstVar
                || (isReferencedEntry() && definedEntry.isConst());
    }

    public int getVarValue() {
        if (tableEntryType == TableEntryType.Var) {
            return var.getValue();
        } else {
            return constVar.getValue();
        }
    }

    public TableEntryType getActualType() {
        if (tableEntryType == TableEntryType.ReferencedEntry) {
            return referencedEntry.getActualType();
        } else {
            return tableEntryType;
        }
    }

    public boolean haveSameType(FuncParam funcParam) {
        if (this.getActualType() == TableEntryType.Var ||
                this.getActualType() == TableEntryType.ConstVar ||
                this.getActualType() == TableEntryType.FunctionInt) {
            return funcParam.getType() == 0;
        } else if (this.getActualType() == TableEntryType.Array1 ||
                this.getActualType() == TableEntryType.ConstArray1) {
            return funcParam.getType() == 1;
        } else if (this.getActualType() == TableEntryType.Array2 ||
                this.getActualType() == TableEntryType.ConstArray2) {
            return funcParam.getType() == 2;
        } else if (this.getActualType() == TableEntryType.FunctionVoid) {
            return false;
        }
        return false;
    }

    @Override
    public String toString() {
        if (tableEntryType == TableEntryType.Var) {
            return getName() + " Var";
        } else if (tableEntryType == TableEntryType.ConstVar) {
            return " ConstVar";
        } else if (tableEntryType == TableEntryType.Array1) {
            return getName() + " Array1";
        } else if (tableEntryType == TableEntryType.ConstArray1) {
            return getName() + " ConstArray1";
        } else if (tableEntryType == TableEntryType.Array2) {
            return getName() + " Array2";
        } else if (tableEntryType == TableEntryType.ConstArray2) {
            return getName() + " ConstArray2";
        } else if (tableEntryType == TableEntryType.FunctionVoid) {
            return getName() + " FunctionVoid";
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            return getName() + " FunctionInt";
        } else {
            return getName() + " Table.ReferencedEntry ActualType is " + referencedEntry.getActualType();
        }
    }

    public String getName() {
        if (this.tableEntryType == TableEntryType.ReferencedEntry) {
            return this.definedEntry.getName();
        }
        return this.node.getToken().getValue();
    }

    public TableEntryType getTableEntryType() {
        return tableEntryType;
    }

    public int getD24Array2() {
        if (tableEntryType == TableEntryType.Array2) {
            return array2.getDimension2();
        } else {
            return constArray2.getDimension2();
        }
    }

    public Array1 getArray1() {
        return array1;
    }

    public ConstArray1 getConstArray1() {
        return constArray1;
    }

    public Array2 getArray2() {
        return array2;
    }

    public ConstArray2 getConstArray2() {
        return constArray2;
    }

    public void addParamForFuncEntry(TableEntry entry) {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            if (entry.getTableEntryType().equals(TableEntryType.Var) ||
            entry.getTableEntryType().equals(TableEntryType.ConstVar)) {
                functionVoid.addVarParam(new Var());
            } else if (entry.getTableEntryType().equals(TableEntryType.Array1) ||
            entry.getTableEntryType().equals(TableEntryType.ConstArray1)) {
                functionVoid.addArray1Param(new Array1());
            } else if (entry.getTableEntryType().equals(TableEntryType.Array2) ||
                    entry.getTableEntryType().equals(TableEntryType.ConstArray2)) {
                functionVoid.addArray2Param(new Array2(entry.getD24Array2()));
            }
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            if (entry.getTableEntryType().equals(TableEntryType.Var) ||
                    entry.getTableEntryType().equals(TableEntryType.ConstVar)) {
                functionInt.addVarParam(new Var());
            } else if (entry.getTableEntryType().equals(TableEntryType.Array1) ||
                    entry.getTableEntryType().equals(TableEntryType.ConstArray1)) {
                functionInt.addArray1Param(new Array1());
            } else if (entry.getTableEntryType().equals(TableEntryType.Array2) ||
                    entry.getTableEntryType().equals(TableEntryType.ConstArray2)) {
                functionInt.addArray2Param(new Array2(entry.getD24Array2()));
            }
        }
    }

    public int getValueFromReferencedArray2(int d1, int d2) {
        if (definedEntry.getTableEntryType() == TableEntryType.Array2) {
            return definedEntry.getArray2().getValue(d1, d2);
        } else {
            return definedEntry.getArray2().getValue(d1, d2);
        }
    }

    public int getValueFromReferencedArray1(int d1) {
        if (definedEntry.getTableEntryType() == TableEntryType.Array1) {
            return definedEntry.getArray1().getValue(d1);
        } else {
            return definedEntry.getArray1().getValue(d1);
        }
    }
}

