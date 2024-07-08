package LLVM;

import LLVM.Elements.Function;
import Parser.ASTNode;
import Types.Type;

import java.util.ArrayList;

public class IRBuilder {
    private final ASTNode root;
    private final Module module = new Module();
    private Value curValue = null;
    private Function curFunction = null;
    private BasicBlock curBasicBlock = null;
    private boolean isGlobalInit = false;
    private ValueTable curValueTable;
    private boolean isCallingFunc = false;
    private int curInt = 0;
    private boolean isConstant = false;
    private ArrayList<Integer> curDimensions = new ArrayList<>();
    private Type curType = null;
    private ArrayList<Type> curTypeArray = new ArrayList<>();
    private BasicBlock curIfBasicBlock = null;
    private BasicBlock curElseBasicBlock = null;
    private BasicBlock curBreakBasicBlock = null;
    private BasicBlock curContinueBasicBlock = null;
    private boolean isSingle = false;
    private boolean isArrayInit = false;
    private boolean isMultiArrayInit = false;
    private ArrayList<Value> curArray = new ArrayList<>();
    private int curArrayIndex = -1;
    private boolean isEnterCurTable = false;

    public IRBuilder(ASTNode root) {
        this.root = root;
    }

    public String toString() {
        return module.toString();
    }
}
