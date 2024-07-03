import EntryType.*;
import Instructions.MemoryInstr;
import Instructions.MemoryType;
import Instructions.TerminatorInstr;
import Instructions.TerminatorType;
import Lexer.Symbol;
import Parser.ASTNode;
import Parser.ErrorNode;
import Parser.ErrorType;
import Parser.GrammerSymbol;
import STable.SymbolTable;
import STable.TableEntry;
import STable.TableEntryType;
import Type.ArrayType;
import Type.IntegerType;
import Type.PointerType;
import Type.VoidType;
import Values.BasicBlock;
import Values.Constants.*;
import Values.Module;
import Values.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class IRVisitor {
    private ASTNode root;
    private SymbolTable curSymbolTable; // 当前符号表
    private HashMap<ErrorType, String> errorType2Symbol; // 错误类型到符号的映射
    private ArrayList<String> errorList; // 错误列表
    private ArrayList<Integer> curDimensions; // 当前维度列表(visitConstDef用)
    private ConstValue curConstValue; // 当前需要赋值的常量值(visitConstInitVal用)
    // private Values.Value curValue; // 当前需要赋值的变量值(visitInitVal用)
    private boolean isMultiArrayInit; // 多维数组初始化(visitConstInitVal和visitInitVal用)
    private boolean isConstant; // 当前是否为常量(visitConstExp用)
    private int curInt; // 当前得到的整数(visitConstExp用)
    private boolean receiveReturn; // 当前函数是否有返回值(visitBlock & visitFuncDef用)
    private boolean createSTableBeforeBlock; // 是否在Block前创建符号表(visitBlock & visitFuncDef用)
    private FuncType curFuncType; // 当前函数的返回值类型(visitFuncDef用)
    private int funcEndLineNum; // 函数结束的行号(visitFuncDef用)
    private boolean debug; // 是否开启debug模式
    private TableEntry curTableEntry; // 分析左值的时候用
    private int inFor; // 解析for循环的时候用
    private ArrayList<TableEntry> funcRParams; // 分析函数实参时用

    // 用于生成IR
    private Value curValue; // 一切皆Value(ConstValue, Var, Array1,
    // Array2, Instruction, Function, BasicBlock)
    private Function curFunction; // 指示当前函数
    private BasicBlock curBasicBlock; // 指示当前基本块
    private Module module;
    private boolean irDebug; // 中间代码生成debug
    private boolean isGlobalInit; // 判断是不是全局常量初始化
    private ValueTable curValueTable; // 当前值表
    private boolean isCallingFunc; // 是否在调用函数

    // 中间代码生成的构造函数
    public IRVisitor(ASTNode root, boolean irDebug, Module module) {
        this.root = root;
        this.curSymbolTable = new SymbolTable(null, true);
        this.errorList = new ArrayList<>();

        this.curDimensions = new ArrayList<>();
        this.isConstant = false;
        this.isMultiArrayInit = false;
        this.receiveReturn = false;
        this.createSTableBeforeBlock = false;
        this.curFuncType = FuncType.NotFunc;
        this.funcEndLineNum = 0;
        this.irDebug = irDebug;
        this.inFor = 0;
        this.curTableEntry = null;
        this.funcRParams = new ArrayList<>();
        this.debug = false;

        this.module = module;
        this.isGlobalInit = false;
        isCallingFunc = false;
        this.curValueTable = new ValueTable(null, true);
    }

    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter CompUnit");
        }
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammerSymbol() == GrammerSymbol.Decl) {
                visitDecl(child);
            } else if (child.getGrammerSymbol() == GrammerSymbol.FuncDef) {
                visitFuncDef(child);
            } else if (child.getGrammerSymbol() == GrammerSymbol.MainFuncDef) {
                visitMainFuncDef(child);
            }
        }
    }

    // Decl -> ConstDecl | VarDecl
    public void visitDecl(ASTNode node) {
        // Actually the size of children is 1
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammerSymbol() == GrammerSymbol.ConstDecl) {
                visitConstDecl(child);
            } else {
                visitVarDecl(child);
            }
        }
    }

    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    // i => Parser.ErrorType: MissingSEMICN
    public void visitConstDecl(ASTNode node) {
        int i = 2;
        while (i < node.getChildren().size() - 1) {
            visitConstDef(node.getChildren().get(i));
            i += 2;
        }
    }

    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    // b => Parser.ErrorType: IdentRedefined
    // k => Parser.ErrorType: RBRACKMissing
    public void visitConstDef(ASTNode node) {
        // Ident
        ASTNode ident = node.getChild(0);
        int i = 1;
        int length = node.getChildren().size();
        curDimensions.clear();
        while (i < length - 2 &&
                node.getChild(i).getToken().getSymbol().equals(Symbol.LBRACK)) {

            if (debug) {
                System.out.println("Visitor Enter ConstDef");
            }
            isConstant = true;
            visitConstExp(node.getChild(i + 1));
            isConstant = false;
            curDimensions.add(curInt);

            if (node.getChild(i + 2) instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }
            i += 3;
        }

        // ConstInitVal
        if (curDimensions.size() == 0) {
            if (curSymbolTable.isRoot()) {
                isGlobalInit = true;
                visitConstInitVal(node.getChild(-1), curDimensions.size());
                isGlobalInit = false;
                GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), (Constant) curValue,
                        true, module);
                if (irDebug) {
                    System.out.println("globalVal: " + globalVariable.toString());
                }
                curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
            } else { // ???
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                visitConstInitVal(node.getChild(-1), curDimensions.size());
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                        curValue, alloca);
            }
        } else {
            visitConstInitVal(node.getChild(-1), curDimensions.size());
        }


        switch (curConstValue.getType()) {
            case 0:
                TableEntry varEntry = new TableEntry(ident, curConstValue.getVar(), false);
                curSymbolTable.addEntry(ident.getToken().getValue(), varEntry);
                break;
            case 1:
                TableEntry array1Entry = new TableEntry(ident, curConstValue.getArray1(), false);
                curSymbolTable.addEntry(ident.getToken().getValue(), array1Entry);
                break;
            case 2:
                TableEntry array2Entry = new TableEntry(ident, curConstValue.getArray2(), false);
                curSymbolTable.addEntry(ident.getToken().getValue(), array2Entry);
                break;
            default:
                break;
        }
    }

    // ConstInitVal -> ConstExp
    //    | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public void visitConstInitVal(ASTNode node, int dimension) {
        if (irDebug) {
            System.out.println("Visitor Enter ConstInitVal");
            System.out.println("dimension is " + dimension);
            if (node.getGrammerSymbol() != null) {
                System.out.println("ConstInitVal Node is " + node.getGrammerSymbol());
            } else {
                System.out.println("ConstInitVal Node is " + node.getToken().getSymbol());
            }
            node.printAllChildren();
        }
        switch (dimension) {
            case 0: // ConstExp
                isConstant = true;
                visitConstExp(node.getChild(0));
                isConstant = false;

                ConstVar constVar = new ConstVar(curInt);
                curConstValue = new ConstValue(0, constVar);
                curValue = new ConstantInt(32, curInt);
                break;
            case 1:
                if (!isMultiArrayInit) {
                    int d1 = curDimensions.get(0);
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 1; i < d1; i+=2) {
                        isConstant = true;
                        visitConstExp(node.getChild(i).getChild(0));
                        isConstant = false;
                        values.add(curInt);
                    }
                    ConstArray1 constArray1 = new ConstArray1(d1, values);
                    curConstValue = new ConstValue(1, constArray1);
                } else {
                    int d1 = (node.getChildrenSize() - 1) / 2;
                    for (int i = 1; i < d1; i+=2) {
                        isConstant = true;
                        visitConstExp(node.getChild(i).getChild(0));
                        isConstant = false;
                        curConstValue.addValues(curInt);
                    }
                }
                break;

            case 2:
                isMultiArrayInit = true;
                int d1 = curDimensions.get(0);
                int d2 = curDimensions.get(1);
                curConstValue = new ConstValue(2, new ConstArray2(d1, d2));
                for (int i = 1; i < d1; i+=2) {
                    if (debug) {
                        System.out.println("child(i) is " + node.getChild(i));
                    }
                    visitConstInitVal(node.getChild(i), 1);
                }
                isMultiArrayInit = false;
                break;
            default:
                break;
        }
    }

    // VarDecl → BType VarDef { ',' VarDef } ';' // i
    // i => Parser.ErrorType: SEMICNMissing
    public void visitVarDecl(ASTNode node) {
        int i = 1;
        while (i < node.getChildren().size() - 1) {
            visitVarDef(node.getChildren().get(i));
            i += 2;
        }
    }

    // VarDef → Ident { '[' ConstExp ']' } // b
    //    | Ident { '[' ConstExp ']' } '=' InitVal // k
    // b => Parser.ErrorType: IdentRedefined
    // k => Parser.ErrorType: RBRACKMissing
    public void visitVarDef(ASTNode node) {
        if (irDebug) {
            System.out.println("Enter VisitVarDef1!!");
        }
        ASTNode ident = node.getChild(0);
        int i = 1;
        int length = node.getChildren().size();
        curDimensions.clear();
        while (i < length - 2 &&
                node.getChild(i).getToken().getSymbol().equals(Symbol.LBRACK)) {

            isConstant = true;
            visitConstExp(node.getChild(i + 1));
            isConstant = false;
            curDimensions.add(curInt);

            i += 3;
        }

        int d = curDimensions.size();
        if (d == 0) { // 常量(判断全局或局部)
            if (curSymbolTable.isRoot()) { // 全局初始化
                if (Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    isGlobalInit = true;
                    visitInitVal(node.getChild(-1));
                    isGlobalInit = false;
                    GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), (Constant) curValue,
                            false, module);
                    if (irDebug) {
                        System.out.println("globalVal1: " + globalVariable.toString());
                    }
                    curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                    // module.addGlobalVariable(globalVariable);
                } else {
                    GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), new IntegerType(32),
                            module);
                    if (irDebug) {
                        System.out.println("globalVal2: " + globalVariable.toString());
                    }
                    curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                    // module.addGlobalVariable(globalVariable);
                }
            }
            else { // 普通变量定义
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                curValueTable.addEntry(ident.getToken().getValue(), alloca);
                if (Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    visitInitVal(node.getChild(-1));
                    MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                            curValue, alloca);
                }
            }
        }
        else {
            if (Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                visitInitVal(node.getChild(-1));
            }
        }


        if (curDimensions.size() == 0) { // VarDef -> Ident
            // VarDef -> Ident '=' InitVal
            TableEntry varEntry = new TableEntry(ident, new Var(ident.getToken().getValue()), false);
            curSymbolTable.addEntry(ident.getToken().getValue(), varEntry);
        } else if (curDimensions.size() == 1){ // VarDef -> Ident [] '=' InitVal
            TableEntry array1Entry = new TableEntry(ident, new Array1(curDimensions.get(0)), false);;
            curSymbolTable.addEntry(ident.getToken().getValue(), array1Entry);
        } else if (curDimensions.size() == 2) {
            TableEntry array2Entry = new TableEntry(ident, new Array2(curDimensions.get(0),
                    curDimensions.get(1)), false);
            curSymbolTable.addEntry(ident.getToken().getValue(), array2Entry);
        }
    }

    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // 注意这里和ConstInitVal的区别: 这里不一定能算出来初值，所以不需要在这里进行赋值
    public void visitInitVal(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            if (isGlobalInit) {
                isConstant = true;
                visitExp(node.getChild(0));
                isConstant = false;
                curValue = new ConstantInt(32, curInt);
            } else {
                visitExp(node.getChild(0));
            }
            // visitExp(node.getChild(0));
        } else {
            int i = 1;
            while (i < node.getChildrenSize() - 1) {
                visitInitVal(node.getChild(i));
                i += 2;
            }
        }
    }

    // FuncDef -> EntryType.FuncType Ident '(' [FuncFParams] ')' Block // b g j
    // b => Parser.ErrorType: IdentRedefined
    // g => Parser.ErrorType: RETURNMissing
    // j => Parser.ErrorType: RPARENTMissing
    public void visitFuncDef(ASTNode node) {
        // EntryType.FuncType -- Process Directly
        ASTNode funcType = node.getChild(0).getChild(0);
        // Ident
        ASTNode ident = node.getChild(1);
        // New Entry
        TableEntry funcEntry = new TableEntry();
        // Create a new func symbol table
        SymbolTable funcSymbolTable = new SymbolTable(curSymbolTable, false);
        curSymbolTable.addChildTable(funcSymbolTable);

        ValueTable valueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(valueTable);

        Function function = null;
        // Discuss the type of function
        if (funcType.getToken().getSymbol().equals(Symbol.VOIDTK)) {
            FunctionVoid functionVoid = new FunctionVoid();
            funcEntry = new TableEntry(ident, functionVoid);
            curFuncType = FuncType.VoidFunc;
        } else if (funcType.getToken().getSymbol().equals(Symbol.INTTK)) {
            FunctionInt functionInt = new FunctionInt();
            funcEntry = new TableEntry(ident, functionInt);
            curFuncType = FuncType.IntFunc;
        }


        curSymbolTable.addEntry(ident.getToken().getValue(), funcEntry);
        // curValueTable.addEntry(ident.getToken().getValue(), function);
        // change curSymbolTable
        curSymbolTable = funcSymbolTable;

        curValueTable.addEntry(ident.getToken().getValue(), function);
        curValueTable = valueTable;
        // FuncFParams
        if (Objects.equals(node.getChild(3).getGrammerSymbol(), GrammerSymbol.FuncFParams)) {
            visitFuncFParams(node.getChild(3), funcEntry);
        }

        Function function1 = null;

        if (curFuncType == FuncType.VoidFunc) {
            function1 = new Function("@" + ident.getToken().getValue(), false, funcEntry.getFunctionVoid());
            function = IRPort.buildFunctionVoid(ident.getToken().getValue(), false,
                    module, function1);
            System.out.println("function is " + function.toString());
        } else if (curFuncType == FuncType.IntFunc) {
            System.out.println("ident is " + ident.getToken().getValue());
            function1 = new Function("@" + ident.getToken().getValue(), false, funcEntry.getFunctionInt());
            System.out.println("function1 is " + function1.toString());
            function = IRPort.buildFunctionInt(ident.getToken().getValue(), false, module,
                    function1);
            System.out.println("function is " + function.toString());
        }
        curFunction = function;


        BasicBlock basicBlock = IRPort.buildBasicBlock(function);
        curBasicBlock = basicBlock;

        // 遍历函数参数
        int i = 0;
        for (FuncParam funcParam : funcEntry.getParams()) {
            if (funcParam.getType() == 0) {
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                        curFunction.getArguments().get(i), alloca);
                curValueTable.addEntry(((Var) funcParam).getName(), alloca);
            }
            i++;
        }

        // Bool change
        receiveReturn = false;
        createSTableBeforeBlock = true;

        // Block
        visitBlock(node.getChild(-1), true);

        // recover curSymbolTable
        curSymbolTable = curSymbolTable.getParent();
        curValueTable = curValueTable.getParent();
        assert curSymbolTable.isRoot();

        receiveReturn = false;
        curFuncType = FuncType.NotFunc;
        createSTableBeforeBlock = false;

        if (curFunction.getFunctionType() == 1) {
            if (curBasicBlock.getInstructions().size() == 0) {
                IRPort.buildRetNoReturn(curBasicBlock);
            } else {
                if (!(curBasicBlock.getInstructions().
                        get(curBasicBlock.getInstructions().size() - 1) instanceof TerminatorInstr)) {
                    IRPort.buildRetNoReturn(curBasicBlock);
                }
            }
        }

    }

    // MainFuncDef -> 'int' 'main' '(' ')' Block // g j
    // g => Parser.ErrorType: ReturnMissing
    // j => Parser.ErrorType: RPARENTMissing
    public void visitMainFuncDef(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter MainFuncDef");
        }


        SymbolTable mainSymbolTable = new SymbolTable(curSymbolTable, false);
        curSymbolTable.addChildTable(mainSymbolTable);
        curSymbolTable = mainSymbolTable;

        ValueTable mainValueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(mainValueTable);
        curValueTable = mainValueTable;

        // Bool change
        receiveReturn = false;
        createSTableBeforeBlock = true;
        curFuncType = FuncType.MainFunc;

        // Build Values.Constants.Function
        Function function = IRPort.buildFunctionInt("main", false, module);
        curFunction = function;

        // Build Values.BasicBlock
        BasicBlock basicBlock = IRPort.buildBasicBlock(function);
        curBasicBlock = basicBlock;

        // Block
        visitBlock(node.getChild(-1), true);

        // recover
        receiveReturn = false;
        curFuncType = FuncType.NotFunc;
        createSTableBeforeBlock = false;
        curSymbolTable = curSymbolTable.getParent();
        curValueTable = curValueTable.getParent();
    }

    // FuncFParams -> FuncFParam { ',' FuncFParam }
    // 函数形式参数同名在此解决
    // Waiting to solve
    public void visitFuncFParams(ASTNode node, TableEntry funcEntry) {
        int i = 0;
        TableEntry funcFParam;
        while (i < node.getChildrenSize()) {
            funcFParam = visitFuncFParam(node.getChild(i));
//           if (curSymbolTable.containsEntry(funcFParam.getName())) {
//               errorList.add(new ErrorNode(ErrorType.IdentRedefined, funcFParam.getNode()
//                       .getToken().getLineNumber(), null, 0).toString(errorType2Symbol));
//           } else {
            // 要改 code generation2 TODO
            curSymbolTable.addEntry(funcFParam.getName(), funcFParam);
            curValueTable.addEntry(funcFParam.getName(), funcFParam.getTableEntryType() == TableEntryType.Var ?
                    new ConstantInt(32, 0) : new ConstantArray(new ArrayType()));
//            }
            funcEntry.addParamForFuncEntry(funcFParam);
            i += 2;
        }
    }

    //  FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]  //   b k
    // b => Parser.ErrorType: IdentRedefined
    // k => Parser.ErrorType: RBRACKMissing
    public TableEntry visitFuncFParam(ASTNode node) {
        ASTNode ident = node.getChild(1);

        if (node.getChildrenSize() == 2) { // BType Ident
            return new TableEntry(ident, new Var(ident.getToken().getValue()), true);
        }
        else if (node.getChildrenSize() == 4) {
            if (node.getChild(3) instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }

            return new TableEntry(ident, new Array1(), true);
        }
        else if (node.getChildrenSize() == 7) {
            // 获取第二维数
            visitConstExp(node.getChild(5));
            if (node.getChild(3) instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }

            if (node.getChild(6) instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }

            return new TableEntry(ident, new Array2(curInt), true);
        } else {
            throw new RuntimeException("FuncFParam has no match condition!!!!");
        }
    }

    // Block -> '{' { BlockItem } '}'
    // 调用前新建符号表
    public void visitBlock(ASTNode node, boolean inFuncBlock) {
        if (irDebug) {
            System.out.println("Visitor Enter Block");
        }

        assert createSTableBeforeBlock;
        createSTableBeforeBlock = false;

        int i = 1;
        while (i < node.getChildrenSize() - 1) {
            visitBlockItem(node.getChild(i), inFuncBlock);
            i++;
        }

        funcEndLineNum = node.getChild(-1).getToken().getLineNumber(); // 函数结尾的右括号所在的行号
    }

    // BlockItem -> Decl | Stmt
    public void visitBlockItem(ASTNode node, boolean inFuncBlock) {
        if (irDebug) {
            System.out.println("Visitor Enter BlockItem");
        }

        if (Objects.equals(node.getChild(0).getGrammerSymbol(), GrammerSymbol.Decl)) {
            visitDecl(node.getChild(0));
        } else {
            visitStmt(node.getChild(0), inFuncBlock);
        }
    }

    // Stmt -> LVal '=' Exp ';' | [Exp] ';' | Block // h i
    //    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
    //    | 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    //    | 'break' ';' | 'continue' ';' // i m
    //    | 'return' [Exp] ';' // f i
    //    | LVal '=' 'getint''('')'';' // h i j
    //    | 'printf''('FormatString{,Exp}')'';' // i j l
    public void visitStmt(ASTNode node, boolean inFuncBlock) {
        ASTNode first = node.getChild(0);
        ASTNode last = node.getChild(-1);
        if (irDebug) {
            System.out.println("Visitor Enter Stmt");
            System.out.println("node is " + node.getGrammerSymbol());
            if (first.getGrammerSymbol() != null) {
                System.out.println("first node is " + first.getGrammerSymbol());
            }
            else {
                System.out.println("first node is " + first.getToken().getSymbol());
            }
        }

        if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.LVal)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter LVal Branch");
            }
            // Stmt -> LVal '=' Exp ';'
            // Stmt -> LVal '=' 'getint''('')'';'
            visitLval(first);

            Value leftOp = curValue;
            if (curTableEntry == null) return; // 名字未定义
            if (node.getChild(2).getToken() != null &&
                    Objects.equals(node.getChild(2).getToken().getSymbol(), Symbol.GETINTTK)) {
                Function function = module.getFunction("@getint");
                curValue = IRPort.buildCallWithReturn(curBasicBlock, function, function);
                if (irDebug) {
                    System.out.println("curValue is " + curValue.toString());
                }
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store
                        , curValue, leftOp);
                if (irDebug) {
                    System.out.println("store is " + store.toString());
                }
            }

            if (Objects.equals(node.getChild(2).getGrammerSymbol(), GrammerSymbol.Exp)) {
                // LVal '=' Exp ';' LVal '=' 'getint''('')'';'
                visitExp(node.getChild(-2));
                Value rightOp = curValue;
                IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store, rightOp, leftOp);
            }

        } else if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.Block)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter Block");
            }
            // Stmt -> Block
            SymbolTable blockTable = new SymbolTable(curSymbolTable, false);
            curSymbolTable = blockTable;
            createSTableBeforeBlock = true;
            visitBlock(first, inFuncBlock);
            curSymbolTable = curSymbolTable.getParent();
        } else if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.Exp)
                || node.getChildrenSize() == 1) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter Exp Branch");
            }
            // Stmt -> [Exp] ';'
            if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.Exp)) {
                visitExp(first);
            }

        } else if (first.getToken().getSymbol().equals(Symbol.IFTK)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter If Branch");
            }
            // Stmt -> 'if' '(' Cond ')' Stmt ['else' Stmt]
            visitCond(node.getChild(2));

            visitStmt(node.getChild(4), inFuncBlock);
            if (node.getChildrenSize() > 5) {
                visitStmt(node.getChild(6), inFuncBlock);
            }
        } else if (first.getToken().getSymbol().equals(Symbol.FORTK)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter For Branch");
            }
            // Stmt -> 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            inFor++;
            for (int i = 2; i < node.getChildrenSize(); i++) {
                if (Objects.equals(node.getChild(i).getGrammerSymbol(), GrammerSymbol.ForStmt)) {
                    visitForStmt(node.getChild(i));
                }
                else if (Objects.equals(node.getChild(i).getGrammerSymbol(), GrammerSymbol.Cond)) {
                    visitCond(node.getChild(i));
                }
                else if (Objects.equals(node.getChild(i).getGrammerSymbol(), GrammerSymbol.Stmt)) {
                    visitStmt(node.getChild(i), inFuncBlock);
                }
            }
            inFor--;
        } else if (first.getToken().getSymbol().equals(Symbol.BREAKTK) ||
                first.getToken().getSymbol().equals(Symbol.CONTINUETK)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter BreakContinue Branch");
            }
            // Stmt -> 'break' ';' | 'continue' ';' // i m

        } else if (first.getToken().getSymbol().equals(Symbol.RETURNTK)) {
            if (debug) {
                System.out.println("Visitor From Stmt Enter Return Branch");
            }
            // Stmt -> 'return' [Exp] ';' // f i
            receiveReturn = inFuncBlock;
            if (Objects.equals(node.getChild(1).getGrammerSymbol(), GrammerSymbol.Exp)) {
                visitExp(node.getChild(1));
                // 返回值是curInt(curTableEntry) ==> curValue
                curValue = IRPort.buildRetWithReturn(curBasicBlock, curValue);
            } else {
                // 返回值是void ==> null
                curValue = IRPort.buildRetNoReturn(curBasicBlock);
            }

        } else if (first.getToken().getSymbol().equals(Symbol.PRINTFTK)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter Printf Branch");
            }
            // Stmt -> 'printf''('FormatString{,Exp}')'';' // i j l
            ASTNode formatStr = node.getChild(2);
            ArrayList<Value> expNodes = new ArrayList<>();

            for (int i = 4; i < node.getChildrenSize() - 2; i += 2) {
                visitExp(node.getChild(i));
                expNodes.add(curValue);
            }

            Function callee = module.getFunction("@putch");
            Function callInt = module.getFunction("@putint");
            String format = formatStr.getToken().getValue();
            int l = format.length();
            for (int i = 1; i < l - 1; i++) {
                char c = format.charAt(i);
                if (c == '%' && format.charAt(i + 1) == 'd') {
                    Value expNode = expNodes.remove(0);
                    IRPort.buildCallWithNoReturn(curBasicBlock, callInt, callInt, expNode);
                    i++;
                } else {
                    if (c == 92 && format.charAt(i + 1) == 'n') {
                        i++;
                        Value constNode = new ConstantInt(32, 10);
                        IRPort.buildCallWithNoReturn(curBasicBlock, callee, callee, constNode);
                    } else {
                        Value constNode = new ConstantInt(32, (int) c);
                        IRPort.buildCallWithNoReturn(curBasicBlock, callee, callee, constNode);
                    }
                }
            }


            // '('
            if (node.getChild(-2) instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }

            // ';'
            if (last instanceof ErrorNode errorNode) {
                errorList.add(errorNode.toString(errorType2Symbol));
            }
        } else {
            throw new RuntimeException("Stmt No Match Condition!!!!");
        }
    }

    /**
     * 对格式字符串进行检查
     // * @param format 格式字符串
     // * @return -1 if 格式字符串格式错误，>= 0 if 格式字符串合法，返回 "%d" 的个数
     */
    private int checkFormatString(ASTNode node) {
        String format = node.getToken().getValue();
        if (irDebug) {
            System.out.println("checkFormatString format is " + format);
        }
        int l = format.length();
        int count = 0;
        for (int i = 1; i < l - 1; i++) {
            char c = format.charAt(i);
            if (c != 32 && c != 33 && !(c >= 40 && c <= 126)) {
                // (space) / ! / ( == 40 / ` == 126 40-126之间是所有正常字符
                if (c == '%') {
                    if (i < l - 1 && format.charAt(i + 1) == 'd') {
                        count = count + 1;
                        continue;
                    } else {
                        return -1;
                    }
                }
                return -1;
            }
            if (c == 92 && (i >= l - 1 || format.charAt(i + 1) != 'n')) {
                // \
                return -1;
            }
        }
        return count;
    }

    // ForStmt -> LVal '=' Exp   //h
    public void visitForStmt(ASTNode node) {
        ASTNode first = node.getChild(0);
        visitLval(first);
        if (curTableEntry == null) return; // 名字未定义
        if (curTableEntry.isConst()) {
            // h ConstantAssign
            // LineNumber: LVal -> Ident {'[' Exp ']'}
            errorList.add(new ErrorNode(ErrorType.ConstAssign, first.getChild(0).getToken().getLineNumber(),
                    null, 0).toString(errorType2Symbol));
        }
        visitExp(node.getChild(-1));
    }

    // Cond -> LOrExp
    public void visitCond(ASTNode node) {
        visitLOrExp(node.getChild(0));
    }

    // PrimaryExp -> '(' Exp ')' | LVal | Number
    public void visitPrimaryExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter PrimaryExp");
            if (node.getGrammerSymbol() != null) {
                System.out.println("PrimaryExp Node is " + node.getGrammerSymbol());
            } else {
                System.out.println("PrimaryExp Node is " + node.getToken().getSymbol());
            }
        }
        if (node.getChildrenSize() > 1) {
            // '(' Exp ')'
            visitExp(node.getChild(1));
        }
        else if (Objects.equals(node.getChild(0).getGrammerSymbol(), GrammerSymbol.LVal)) {
            if (isConstant) {
                visitLval(node.getChild(0));
            } else {
                boolean nowIsCallingFunc = isCallingFunc;
                if (isCallingFunc) {
                    isCallingFunc = false;
                }
                visitLval(node.getChild(0));

//                if (curValue instanceof GlobalVariable) {
//                    curValue = IRPort.buildMemoryInstr(curBasicBlock,new IntegerType(32),
//                            MemoryType.Load, curValue);
//                }
                if (!nowIsCallingFunc) {
                    if (!(curValue.getType() instanceof IntegerType)) { // 全局变量在函数内被调用
                        if (((PointerType) curValue.getType()).getPointToType() instanceof IntegerType) {
                            curValue = IRPort.buildMemoryInstr(curBasicBlock,new IntegerType(32),
                                    MemoryType.Load, curValue);
                        }
                    }
                }
            }

        } else if (Objects.equals(node.getChild(0).getGrammerSymbol(), GrammerSymbol.Number)) {
            visitNumber(node.getChild(0));
        }
        else {
            throw new RuntimeException("PrimaryExp no match condition!!!!");
        }
    }

    // FuncRParams -> Exp { ',' Exp }
    public void visitFuncRParams(ASTNode node, TableEntry tableEntry, ArrayList<Value> args) { // 分函数名记录实参
        tableEntry.clearFuncRParams();
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            visitExp(node.getChild(i)); // 改变了curTableEntry
            args.add(curValue);
            if (curTableEntry != null) {
                if (irDebug) {
                    System.out.println("visitFuncRParams curTableEntry Type.Type is "
                            + curTableEntry.getTableEntryType());
                    System.out.println("visitFuncRParams curInt EntryType.Values.Value is "
                            + curInt);
                }
                tableEntry.addFuncRParam(curTableEntry);
            } else { // 函数实参为常数
                tableEntry.addFuncRParam(new TableEntry(curInt));
            }
        }
    }

    // LVal → Ident {'[' Exp ']'} // c k
    // c => Parser.ErrorType: IdentUndefined
    // k => Parser.ErrorType: RBRACKMissing
    // Lval 一定是之前已经定义过的变量
    public void visitLval(ASTNode node) { // 该函数的返回值传回curTableEntry
        ASTNode ident = node.getChild(0);
//        if (!curSymbolTable.nameExisted(ident.getToken().getValue())) {
//            errorList.add(new ErrorNode(ErrorType.IdentUndefined, ident.getToken()
//                    .getLineNumber(), null, 0).toString(errorType2Symbol));
//            curInt = 0; // ???
//            curTableEntry = null;
//        } else {
        int i = 1;
        int length = node.getChildren().size(); // 1 or 4 or 7
        if (irDebug) {
            System.out.println("first of Lval is " + ident.getToken().getValue());
        }
        TableEntry tableEntry = curSymbolTable.getEntry(ident.getToken().getValue());
        Value value = curValueTable.getEntry(ident.getToken().getValue());
        // definedEntry
        switch (length) {
            case 1: // ident
                if (isConstant) { // 常量赋值时
                    assert tableEntry.isConst();
                    curInt = tableEntry.getVarValue();
                }
                curTableEntry = tableEntry;
                if (irDebug) {
                    System.out.println("visitLval True!!!");
                    System.out.println("curValueTable.size is: " + curValueTable.getSize());
                }
                curValue = value;
                if (irDebug) {
                    System.out.println("curTableEntry: " + curTableEntry.toString());
                    System.out.println("curValue: " + curValue.toString());
                }
                break;

            case 4: // ident[exp]
            case 7: // ident[exp][exp]
                ASTNode exp1 = node.getChild(2);
                visitExp(exp1);

                int v1;

                if (node.getChild(3) instanceof ErrorNode errorNode) { // k mistake
                    errorList.add(errorNode.toString(errorType2Symbol));
                }

                TableEntry referencedTableEntry = null;
                // 计算实际类型
                // 若原符号表中为a[2][3]，则可以引用a[0]但实际上a[0]的actualType为array1 ??? 不太懂
                if (length == 4) {
                    if (tableEntry.getTableEntryType() == TableEntryType.Array1 ||
                            tableEntry.getTableEntryType() == TableEntryType.ConstArray1) {
                        // 实际传进去的是Var
                        referencedTableEntry = new TableEntry(tableEntry, TableEntryType.Var, curInt);

                        if (isConstant) {
                            curInt = referencedTableEntry.getValueFromReferencedArray1(curInt);
                            curTableEntry = null;
                        } else {
                            curTableEntry = referencedTableEntry;
                        }
                    }

                    else {
                        // 实际传进去的是Array1
                        referencedTableEntry = new TableEntry(tableEntry, TableEntryType.Array1, curInt);
                        curTableEntry = referencedTableEntry;
                    }


                }
                else {
                    referencedTableEntry = new TableEntry(tableEntry,
                            TableEntryType.Var, curInt, 0);
                    v1 = curInt;
                    visitExp(node.getChild(5));
                    int v2 = curInt;

                    if (node.getChild(6) instanceof ErrorNode errorNode) {
                        errorList.add(errorNode.toString(errorType2Symbol));
                    }

                    if (isConstant) {
                        curInt = referencedTableEntry.getValueFromReferencedArray2(v1, v2);
                        curTableEntry = null;
                    } else {
                        curTableEntry = referencedTableEntry;
                    }
                }
        }
    // }
    }

    // Number -> IntConst
    public void visitNumber(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter Number");
        }
        curInt = Integer.parseInt(node.getChild(0).getToken().getValue());
        curValue = new ConstantInt(32, curInt);
        curTableEntry = null;
    }

    // Exp -> AddExp
    public void visitExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter Exp");
        }
        visitAddExp(node.getChild(0));
    }

    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' // c d e j
    //        | UnaryOp UnaryExp
    // c ==> Parser.IdentUndefined
    // d ==> Parser.ParaNumNotMatch
    // e ==> Parser.ParaTypeNotMatch
    // j ==> Parser.RPARENTMissing
    public void visitUnaryExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter UnaryExp");
            if (node.getGrammerSymbol() != null) {
                System.out.println("UnaryExp Node is " + node.getGrammerSymbol());
            } else {
                System.out.println("UnaryExp Node is " + node.getToken().getSymbol());
            }
        }
        ASTNode first = node.getChild(0);
        if (irDebug) {
            // System.out.println("Visitor Enter UnaryExp");
            if (first.getGrammerSymbol() != null) {
                System.out.println("UnaryExp First Child Node is " + first.getGrammerSymbol());
            } else {
                System.out.println("UnaryExp First Child Node is " + first.getToken().getSymbol());
            }
        }
        if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.PrimaryExp)) {
            visitPrimaryExp(first);
        } else if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.UnaryOp)) {
            // UnaryOp -> '+' | '−' | '!'
            int op = first.getChild(0).getToken().getSymbol().equals(Symbol.PLUS) ? 1 :
                    first.getChild(0).getToken().getSymbol().equals(Symbol.MINU) ? -1 : 2;
            visitUnaryExp(node.getChild(1));
            if (isConstant) {
                if (op == 1 || op == -1) {
                    curInt *= op;
                } else {
                    if (curInt == 0) {
                        curInt = 1;
                    } else {
                        curInt = 0;
                    }
                }
            } else {
                if (op == -1) { // -
                    System.out.println("processing sub!");
                    curValue = IRPort.buildSub(curBasicBlock, new ConstantInt(32, 0), curValue);
                } else if (op == 2) { // !
                    curValue = IRPort.buildIcmp(curBasicBlock, "eq", new ConstantInt(32, 0), curValue);
                }
            }

        }
        else if (Objects.equals(first.getToken().getSymbol(), Symbol.IDENFR)) {
            TableEntry tableEntry = curSymbolTable.getEntry(first.getToken().getValue());
            System.out.println("callee name is: " + first.getToken().getValue());
            Function callee = module.getFunction("@" + first.getToken().getValue());

            ArrayList<Value> args = new ArrayList<>();
            if (!Objects.equals(node.getChild(2).getGrammerSymbol(), GrammerSymbol.FuncRParams)) {
                // 如果没有参数
                if (tableEntry.funcParamsNum() > 0) {
                    errorList.add(new ErrorNode(ErrorType.ParaNumNotMatch, first.getToken().getLineNumber(),
                            null, 0).toString(errorType2Symbol));
                    curInt = 0;
                    curTableEntry = null;
                    return;
                }
            } else { // 有实参
                visitFuncRParams(node.getChild(2), tableEntry, args);

                // 参数类型不匹配或者数量不匹配
                if (ParamErrorHelper(tableEntry, first.getToken().getLineNumber())) {
                    curInt = 0;
                    curTableEntry = null;
                    return;
                }

            }

            if (callee.getFunctionType() == 1) { // void function
                args.add(0, callee);
                curValue = IRPort.buildCallWithNoReturn(curBasicBlock, callee, args.toArray(new Value[args.size()]));
            } else { // int function
                args.add(0, callee);
                curValue = IRPort.buildCallWithReturn(curBasicBlock, callee, args.toArray(new Value[args.size()]));
            }

            curTableEntry = tableEntry;
        }

        // j mistake
        if (node.getChild(-1) instanceof ErrorNode errorNode) {
            errorList.add(errorNode.toString(errorType2Symbol));
        }

    }

    private boolean ParamErrorHelper(TableEntry tableEntry, int lineNum) {
        int rParamSize = tableEntry.getFuncRParamsNum();
        // d ==> Parser.ParaNumNotMatch
        if (tableEntry.funcParamsNum() != rParamSize) {
            if (debug) {
                System.out.println("ParamErrorHelper tableEntry.funcParamsNum() is " + tableEntry.funcParamsNum());
                System.out.println("ParamErrorHelper rParamSize is " + rParamSize);
            }
            errorList.add(new ErrorNode(ErrorType.ParaNumNotMatch, lineNum,
                    null, 0).toString(errorType2Symbol));
            return true;
        }

        // e ==> Parser.ParaTypeNotMatch
        ArrayList<FuncParam> definedFuncParams = tableEntry.getFuncParams();
        for (int i = 0; i < rParamSize; i++) {
            if (debug) {
                System.out.println("ParamErrorHelper i is " + i);
                System.out.println("ParamErrorHelper funcRParams.size is " + tableEntry.getFuncRParamsNum());
                System.out.println("ParamErrorHelper funcRParams.get(i).STable.TableEntryType is " + tableEntry.getFuncRParam(i).getTableEntryType());
                System.out.println("ParamErrorHelper funcRParams.get(i) is " + tableEntry.getFuncRParam(i));
            }
            if (!tableEntry.getFuncRParam(i).hasSameType(definedFuncParams.get(i))) {
                errorList.add(new ErrorNode(ErrorType.ParaTypeNotMatch, lineNum,
                        null, 0).toString(errorType2Symbol));
                return true;
            }
        }

        return false;
    }

    // ConstExp -> AddExp
    public void visitConstExp(ASTNode node) {
        if (debug) {
            if (node.getGrammerSymbol() != null) {
                System.out.println("ConstExp Node is " + node.getGrammerSymbol());
            } else {
                System.out.println("ConstExp Node is " + node.getToken().getSymbol());
            }
        }
        visitAddExp(node.getChild(0));
    }

    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // AddExp -> MulExp { ('+' | '-') MulExp }
    public void visitAddExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter AddExp");
            System.out.println("AddExp children size is " + node.getChildrenSize());
            // node.printAllChildren();
        }
        if (node.getChildrenSize() == 1) {
            visitMulExp(node.getChild(0));
        } else {
            int leftNum = 0;
            visitAddExp(node.getChild(0));
            leftNum = curInt;
            Value leftOp = curValue;
            visitMulExp(node.getChild(2));
            if (isConstant) { // 只有在为常量时需要做简化
                if (node.getChild(1).getToken().getSymbol() == Symbol.PLUS) {
                    curInt = leftNum + curInt;
                } else {
                    curInt = leftNum - curInt;
                }
                curTableEntry = null;
            } else {
                // 生成IR
                if (node.getChild(1).getToken().getSymbol() == Symbol.PLUS) {
                    curValue = IRPort.buildAdd(curBasicBlock, leftOp, curValue);
                } else {
                    curValue = IRPort.buildSub(curBasicBlock, leftOp, curValue);
                }
            }
        }
    }

    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    public void visitMulExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter MulExp");
        }
        if (node.getChildrenSize() == 1) {
            visitUnaryExp(node.getChild(0));
        } else {
            visitMulExp(node.getChild(0));
            int leftNum = curInt;
            Value leftOp = curValue;

            visitUnaryExp(node.getChild(2));
            if (isConstant) { // 常量的话符号表项就返回null
                if (node.getChild(1).getToken().getSymbol() == Symbol.MULT) {
                    curInt = leftNum * curInt;
                } else if (node.getChild(1).getToken().getSymbol() == Symbol.DIV) {
                    curInt = leftNum / curInt;
                } else {
                    curInt = leftNum % curInt;
                }
                curTableEntry = null;
            } else {
                if (node.getChild(1).getToken().getSymbol() == Symbol.MULT) {
                    curValue = IRPort.buildMul(curBasicBlock, leftOp, curValue);
                } else if (node.getChild(1).getToken().getSymbol() == Symbol.DIV) {
                    curValue = IRPort.buildDiv(curBasicBlock, leftOp, curValue);
                } else {
                    curValue = IRPort.buildMod(curBasicBlock, leftOp, curValue);
                }
            }
        }
    }

    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    public void visitLOrExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitLAndExp(node.getChild(0));
        } else {
            visitLOrExp(node.getChild(0));
            visitLAndExp(node.getChild(2));
        }
    }

    // LAndExp -> EqExp | LAndExp '&&' EqExp
    public void visitLAndExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitEqExp(node.getChild(0));
        } else {
            visitLAndExp(node.getChild(0));
            visitEqExp(node.getChild(2));
        }
    }

    // EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    public void visitEqExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitRelExp(node.getChild(0));
        } else {
            visitEqExp(node.getChild(0));
            visitRelExp(node.getChild(2));
        }
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public void visitRelExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitAddExp(node.getChild(0));
        } else {
            visitRelExp(node.getChild(0));
            visitAddExp(node.getChild(2));
        }
    }

    public void printToFile(BufferedWriter outputFile) throws IOException {
        for (String str : errorList) {
            outputFile.write(str + "\n");
        }
    }

}
