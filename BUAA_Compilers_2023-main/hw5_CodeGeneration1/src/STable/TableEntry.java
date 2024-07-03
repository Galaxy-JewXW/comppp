package STable;

import EntryType.*;
import Parser.ASTNode;
import Values.Constants.FunctionInt;
import Values.Constants.FunctionVoid;

import java.util.ArrayList;

public class TableEntry {
    private ASTNode node;
    private boolean isFuncParam; // 是否是函数的形参,注意是形参
    private int funcDefLine; // 函数定义所在行号
    private TableEntryType tableEntryType; // 指示符号表项种类

    private Var var;
    private ConstVar constVar;
    private Array1 array1;
    private ConstArray1 constArray1;
    private Array2 array2;
    private ConstArray2 constArray2;
    private FunctionVoid functionVoid;
    private FunctionInt functionInt;
    private ReferencedEntry referencedEntry;

    private TableEntry definedEntry; // 形参对应的真正定义的表项
    private ArrayList<TableEntry> funcRParams; // 所用的函数实参

    public TableEntry() {

    }

    public TableEntry(TableEntry defineEntry, TableEntryType actualType, int d1) {
        this.tableEntryType = TableEntryType.ReferencedEntry;
        // this.defLineNum = -1;
        this.isFuncParam = false;
        this.referencedEntry = new ReferencedEntry(actualType, defineEntry.getTableEntryType(), d1);
        this.definedEntry = defineEntry;
    }

    public TableEntry(TableEntry defineEntry, TableEntryType actualType, int d1, int d2) {
        this.tableEntryType = TableEntryType.ReferencedEntry;
        // this.defLineNum = -1;
        this.isFuncParam = false;
        this.referencedEntry = new ReferencedEntry(actualType, defineEntry.getTableEntryType(), d1, d2);
        this.definedEntry = defineEntry;
    }

    public int funcParamsNum() {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            return functionVoid.getParamsNum();
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            return functionInt.getParamsNum();
        } else {
            throw new RuntimeException("STable.TableEntry funcParamsNum is null!!!");
        }
    }

    public ArrayList<FuncParam> getFuncParams() {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            return functionVoid.getFuncParams();
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            return functionInt.getFuncParams();
        } else {
            throw new RuntimeException("STable.TableEntry getFuncParams is null!!!");
        }
    }

    public TableEntry(ASTNode node, Var var, boolean isFuncParam) {
        this.node = node;
        this.var = var;
        this.isFuncParam = isFuncParam;
        this.tableEntryType = TableEntryType.Var;
    }

    public TableEntry(ASTNode node, ConstVar constVar, boolean isFuncParam) {
        this.node = node;
        this.constVar = constVar;
        this.isFuncParam = isFuncParam;
        this.tableEntryType = TableEntryType.ConstVar;
    }

    public TableEntry(ASTNode node, Array1 array1, boolean isFuncParam) {
        this.node = node;
        this.array1 = array1;
        this.isFuncParam = isFuncParam;
        this.tableEntryType = TableEntryType.Array1;
    }

    public TableEntry(ASTNode node, ConstArray1 constArray1, boolean isFuncParam) {
        this.node = node;
        this.constArray1 = constArray1;
        this.isFuncParam = isFuncParam;
        this.tableEntryType = TableEntryType.ConstArray1;
    }

    public TableEntry(ASTNode node, Array2 array2, boolean isFuncParam) {
        this.node = node;
        this.array2 = array2;
        this.isFuncParam = isFuncParam;
        this.tableEntryType = TableEntryType.Array2;
    }

    public TableEntry(ASTNode node, ConstArray2 constArray2, boolean isFuncParam) {
        this.node = node;
        this.constArray2 = constArray2;
        this.isFuncParam = isFuncParam;
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

    public TableEntry(int value) { // 在visitFuncRParams中解析实参为常数的情况用
        this.isFuncParam = false;
        this.tableEntryType = TableEntryType.ConstVar;
        this.constVar = new ConstVar(value);
    }

    public void clearFuncRParams() {
        this.funcRParams.clear();
    }

    public int getFuncRParamsNum() {
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
        } else if (tableEntryType == TableEntryType.ConstVar) {
            return constVar.getValue();
        } else {
            throw new RuntimeException("STable.TableEntry getVarValue is null!!!");
        }
    }

    public TableEntryType getActualType() {
        if (tableEntryType == TableEntryType.ReferencedEntry) {
            return referencedEntry.getActualType();
        } else {
            return tableEntryType;
        }
    }

    public boolean hasSameType(FuncParam funcParam) {
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
        else {
            throw new RuntimeException("STable.TableEntry hasSameType is null!!!");
        }
    }

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
            return getName() + " Values.Constants.FunctionVoid";
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            return getName() + " Values.Constants.FunctionInt";
        } else if (tableEntryType == TableEntryType.ReferencedEntry) {
            return getName() + " STable.ReferencedEntry ActualType is " + referencedEntry.getActualType();
        } else {
            throw new RuntimeException("STable.TableEntry toString is null!!!");
        }
    }

    public String getName() {
        if (this.tableEntryType == TableEntryType.ReferencedEntry) {
            return this.definedEntry.getName();
        }
        return this.node.getToken().getValue();
    }

    public ASTNode getNode() {
        return node;
    }

    public TableEntryType getTableEntryType() {
        return tableEntryType;
    }

    public int getD2ForArray2() {
        if (tableEntryType == TableEntryType.Array2) {
            return array2.getD2();
        } else if (tableEntryType == TableEntryType.ConstArray2) {
            return constArray2.getD2();
        } else {
            throw new RuntimeException("STable.TableEntry getD2ForArray2 is null!!!");
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

    public void addParamForFuncEntry(TableEntry param) {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            if (param.getTableEntryType().equals(TableEntryType.Var) ||
            param.getTableEntryType().equals(TableEntryType.ConstVar)) {
                functionVoid.addVarParam(new Var(param.getName()));
            } else if (param.getTableEntryType().equals(TableEntryType.Array1) ||
            param.getTableEntryType().equals(TableEntryType.ConstArray1)) {
                functionVoid.addArray1Param(new Array1());
            } else if (param.getTableEntryType().equals(TableEntryType.Array2) ||
                    param.getTableEntryType().equals(TableEntryType.ConstArray2)) {
                functionVoid.addArray2Param(new Array2(param.getD2ForArray2()));
            }
        }

        else if (tableEntryType == TableEntryType.FunctionInt) {
            if (param.getTableEntryType().equals(TableEntryType.Var) ||
                    param.getTableEntryType().equals(TableEntryType.ConstVar)) {
                functionInt.addVarParam(new Var(param.getName()));
            } else if (param.getTableEntryType().equals(TableEntryType.Array1) ||
                    param.getTableEntryType().equals(TableEntryType.ConstArray1)) {
                functionInt.addArray1Param(new Array1());
            } else if (param.getTableEntryType().equals(TableEntryType.Array2) ||
                    param.getTableEntryType().equals(TableEntryType.ConstArray2)) {
                functionInt.addArray2Param(new Array2(param.getD2ForArray2()));
            }
        }

        else {
            throw new RuntimeException("STable.TableEntry addParamForFuncEntry is null!!!");
        }
    }

    public FunctionVoid getFunctionVoid() {
        return functionVoid;
    }

    public FunctionInt getFunctionInt() {
        return functionInt;
    }

    public int getValueFromReferencedArray2(int d1, int d2) {
        if (definedEntry.getTableEntryType() == TableEntryType.Array2) {
            return definedEntry.getArray2().getValue(d1, d2);
        } else if (definedEntry.getTableEntryType() == TableEntryType.ConstArray2) {
            return definedEntry.getArray2().getValue(d1, d2);
        } else {
            throw new RuntimeException("No match for getValueFromReferencedArray1!!!");
        }
    }

    public int getValueFromReferencedArray1(int d1) {
        if (definedEntry.getTableEntryType() == TableEntryType.Array1) {
            return definedEntry.getArray1().getValue(d1);
        } else if (definedEntry.getTableEntryType() == TableEntryType.ConstArray1) {
            return definedEntry.getArray1().getValue(d1);
        } else {
            throw new RuntimeException("No match for getValueFromReferencedArray1!!!");
        }
    }

    public ArrayList<FuncParam> getParams() {
        if (tableEntryType == TableEntryType.FunctionVoid) {
            return functionVoid.getFuncParams();
        } else if (tableEntryType == TableEntryType.FunctionInt) {
            return functionInt.getFuncParams();
        } else {
            throw new RuntimeException("STable.TableEntry getParams is null!!!");
        }
    }
}

