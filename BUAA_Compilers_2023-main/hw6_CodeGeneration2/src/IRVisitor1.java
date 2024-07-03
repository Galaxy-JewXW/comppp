import Instructions.*;
import Lexer.Symbol;
import Parser.ASTNode;
import Parser.GrammerSymbol;
import Type.IntegerType;
import Type.PointerType;
import Type.Type;
import Type.FunctionType;
import Type.VoidType;
import Type.ArrayType;
import Values.BasicBlock;
import Values.Constants.Constant;
import Values.Constants.ConstantInt;
import Values.Constants.Function;
import Values.Constants.GlobalVariable;
import Values.Module;
import Values.Value;
import Values.Constants.ConstantArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class IRVisitor1 {
    // 用于生成IR的Visitor，不进行错误处理操作
    private ASTNode root;
    private Value curValue; // 一切皆Value(ConstValue, Var, Array1,
    // Array2, Instruction, Function, BasicBlock)
    private Function curFunction; // 指示当前函数
    private BasicBlock curBasicBlock; // 指示当前基本块
    private Module module;
    private boolean irDebug; // 中间代码生成debug
    private boolean isGlobalInit; // 判断是不是全局常量初始化
    private ValueTable curValueTable; // 当前值表
    private boolean isCallingFunc; // 是否在调用函数
    private int curInt; // 用于解析最后是常数的表达式
    private boolean isConstant; // 判断是否是常数
    private ArrayList<Integer> curDimensions; // 当前维度列表(visitConstDef用)
    private Type curType;
    private ArrayList<Type> curTypeArray;
    private BasicBlock curIfBasicBlock;
    private BasicBlock curElseBasicBlock;
    private BasicBlock curBreakBasicBlock;
    private BasicBlock curContinueBasicBlock;
    private boolean isSingleJudge;
    private boolean isArrayInit;
    private boolean isMultiArrayInit;
    private ArrayList<Value> curArray; // 当前数组，构建数组信息时用
    private int curArrayIndex;
    private boolean isEnterCurTable;

    public IRVisitor1(ASTNode root, boolean irDebug, Module module) {
        this.curValue = null;
        this.curFunction = null;
        this.curBasicBlock = null;
        this.module = module;
        this.irDebug = irDebug;
        this.isGlobalInit = false;
        this.curValueTable = new ValueTable(null, true);
        this.isCallingFunc = false;
        this.isConstant = false;
        this.curDimensions = new ArrayList<>();
        this.isGlobalInit = false;
        this.curTypeArray = new ArrayList<>();
        isArrayInit = false;
        isMultiArrayInit = false;
        this.curArray = new ArrayList<>();
        this.isEnterCurTable = false;
    }

    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter CompUnit");
        }

        for (ASTNode child : node.getChildren()) {
            if (child.getGrammerSymbol() == GrammerSymbol.MainFuncDef) {
                visitMainFuncDef(child);
            } else if (child.getGrammerSymbol() == GrammerSymbol.Decl) {
                visitDecl(child);
            } else if (child.getGrammerSymbol() == GrammerSymbol.FuncDef) {
                visitFuncDef(child);
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
    public void visitConstDecl(ASTNode node) {
        int i = 2;
        while (i < node.getChildren().size() - 1) {
            visitConstDef(node.getChildren().get(i));
            i += 2;
        }
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    public void visitVarDecl(ASTNode node) {
        int i = 1;
        while (i < node.getChildren().size() - 1) {
            visitVarDef(node.getChildren().get(i));
            i += 2;
        }
    }

    // VarDef → Ident { '[' ConstExp ']' }
    //    | Ident { '[' ConstExp ']' } '=' InitVal
    public void visitVarDef(ASTNode node) {
        if(irDebug) {
            System.out.println("Enter VarDef!");
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
        if (d == 0) { // 0维，需要判断全局或者局部
            if(irDebug) {
                System.out.println("Enter VarDef 0 dimension!");
            }
            if (curValueTable.isRoot()) {
                if(irDebug) {
                    System.out.println("Enter VarDef 0 dimension root! " + ident.getToken().getValue());
                }
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
                } else {
                    GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), new IntegerType(32),
                            module);
                    if (irDebug) {
                        System.out.println("globalVal2: " + globalVariable.toString());
                    }
                    curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                }
            }
            else { // 普通变量定义
                // 局部变量在赋值前需要申请一块内存
                if(irDebug) {
                    System.out.println("Enter VarDef 0 dimension normal! " + ident.getToken().getValue());
                }
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                curValueTable.addEntry(ident.getToken().getValue(), alloca);
                if (Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    visitInitVal(node.getChild(-1));
                    MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                            curValue, alloca);
                }
            }
        } else { // 数组
            Type arrayType = new IntegerType(32);
            // 数组是一种层叠的模式
            for (int j = curDimensions.size() - 1; j >= 0; j--) {
                arrayType =new ArrayType(arrayType, curDimensions.get(j));
            }
            if (curValueTable.isRoot()) { // 全局变量
                if (!Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    // 没有initVal的情况
                    GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), arrayType, module);
                    curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                    // module.addGlobalVariable(globalVariable);
                } else {
                    isGlobalInit = true;
                    ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
                    visitInitVal(node.getChild(-1));
                    isGlobalInit = false;

                    ArrayList<Constant> constantArrayList = new ArrayList<>();

                    // 全零初始化
                    boolean isZero = true;
                    for (Value value : curArray) {
                        if (((ConstantInt) value).getVal() != 0) {
                            isZero = false; // 数组中的元素有非零值
                        }
                        constantArrayList.add((ConstantInt) value);
                    }

                    int arraySize = 1;
                    for (Integer dimension : dimensions) {
                        arraySize *= dimension;
                    }

                    GlobalVariable globalVariable;
                    if (isZero) {
                        globalVariable = new GlobalVariable(ident.getToken().getValue(),
                                arrayType, module);
                    } else {
                        for (int j = dimensions.size() - 1; j > 0; j--) {
                            int nowDimension = dimensions.get(j);
                            ArrayList<Constant> nowConstantArray = new ArrayList<>();

                            arraySize /= nowDimension;
                            for (int k = 0; k < arraySize; k++) {
                                ArrayList<Constant> array = new ArrayList<>();
                                for (int m = 0; m < nowDimension; m++) {
                                    array.add(constantArrayList.get(k * nowDimension + m));
                                }
                                nowConstantArray.add(new ConstantArray(array));
                            }
                            constantArrayList = nowConstantArray;
                        }

                        ConstantArray initVal = new ConstantArray(constantArrayList);
                        globalVariable = new GlobalVariable(ident.getToken().getValue(), initVal, false, module);
                        // 要标示为global的 => isConst == false
                    }

                    curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                    // module.addGlobalVariable(globalVariable);
                }
            } else { // 局部数组变量
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, arrayType, MemoryType.Alloca);
                curValueTable.addEntry(ident.getToken().getValue(), alloca);

                if (Objects.equals(node.getChild(-1).getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    // 局部数组变量初始化
                    ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
                    visitInitVal(node.getChild(-1));

                    ArrayList<Value> valArray = new ArrayList<>();
                    for (Value value : curArray) {
                        valArray.add(value);
                    }

                    // getElementptr语句
                    curArrayIndex = 0;
                    constArrayInit(arrayType, alloca, dimensions, valArray, 1);
                    curArrayIndex = 0;
                }
            }
            // visitConstInitVal(node.getChild(-1));
        }
    }

    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // 注意这里和ConstInitVal的区别: 这里不一定能算出来初值，所以不需要在这里进行赋值
    public void visitInitVal(ASTNode node) {
        if (irDebug) {
            System.out.println("Enter visitInitVal");
            node.printAllChildren();
        }
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
            if (irDebug) {
                System.out.println("Enter visitInitVal array");
                node.printAllChildren();
            }
            //  InitVal -> '{' [ InitVal { ',' InitVal } ] '}'
            ArrayList<Value> array = new ArrayList<>();
//            int arraySize = 1;
//            for (int i = 0; i < curDimensions.size(); i++) {
//                arraySize *= curDimensions.get(i);
//            }
            for (ASTNode child : node.getChildren()) {
                if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.InitVal)) {
                    if (child.getChildrenSize() == 1) {
                        // Exp
                        if (isGlobalInit) {
                            isConstant = true;
                            visitInitVal(child);
                            isConstant = false;
                        } else {
                            visitInitVal(child);
                        }
                        array.add(curValue);
                    } else {
//                        if (irDebug) {
//                            System.out.println("Prove curDimensions");
//                            for (int k = 0; k < curDimensions.size(); k++) {
//                                System.out.println(curDimensions.get(k) + " ");
//                            }
//                        }
                        // 这里是只保留首元素成为单独的元素数组
                        // curDimensions = new ArrayList<>(Collections.singleton(curDimensions.remove(0)));
                        visitInitVal(child);
                        array.addAll(curArray);
                    }
                }
            }
            curArray = array;
        }
    }

    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    public void visitFuncDef(ASTNode node) {
        ASTNode funcType = node.getChild(0).getChild(0);
        ASTNode ident = node.getChild(1);

        // FuncFParams
        ArrayList<Type> funcFParams = new ArrayList<>();
        if (Objects.equals(node.getChild(3).getGrammerSymbol(), GrammerSymbol.FuncFParams)) {
            visitFuncFParams(node.getChild(3));
            funcFParams.addAll(curTypeArray);
        }

        if (irDebug) {
            for (Type type : funcFParams) {
                System.out.println("funcFParams type is " + type.toString());
            }
        }

        Function function = null;
        // Build Function
        if (funcType.getToken().getSymbol().equals(Symbol.INTTK)) {
            function = IRPort.buildNewFunctionInt(ident.getToken().getValue(), false, module, funcFParams);
        } else {
            function = IRPort.buildNewFunctionVoid(ident.getToken().getValue(), false, module, funcFParams);
        }

        curFunction = function;

        // Build BasicBlock
        BasicBlock basicBlock = IRPort.buildBasicBlock(function);
        curBasicBlock = basicBlock;

        ValueTable valueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(valueTable);

        // build new value table
        curValueTable.addEntry(ident.getToken().getValue(), null);
        curValueTable = valueTable;
        isEnterCurTable = true;

        int funcFParamCnt = 0;
        ASTNode funcFParamsNode = node.getChild(3);
        for (int i = 0; i < funcFParams.size(); i++) {
            if (funcFParamsNode.getChild(funcFParamCnt).getChildrenSize() == 2) { // int
                if (irDebug) {
                    System.out.println("Enter FuncFParam int");
                }
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                        curFunction.getArguments().get(i), alloca);
                // Value value = new ConstantInt(32, 0);
                curValueTable.addEntry(funcFParamsNode.getChild(funcFParamCnt).getChild(1).getToken().getValue()
                        , alloca);
                funcFParamCnt += 2;
            } else { // arrayType 二维数组 或 一维数组
//                Type arrayType = new IntegerType(32);
//                for (int j = funcFParamsNode.getChildrenSize() - 1; j >= 0; j--) {  // 对多维数组的处理
//                    ASTNode child = funcFParamsNode.getChild(j);
//                    if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.ConstExp)) {
//                        isConstant = true;
//                        visitConstExp(child);
//                        isConstant = false;
//                        arrayType = new ArrayType(arrayType, curInt);
//                    }
//                }
                // 数组alloca的时候要用指针类型
                // 一维数组 存为i32，前面加*即为i32*
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, funcFParams.get(i), MemoryType.Alloca);
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                        curFunction.getArguments().get(i), alloca);
                curValueTable.addEntry(funcFParamsNode.getChild(funcFParamCnt).getChild(1).getToken().getValue()
                        , alloca);
                funcFParamCnt += 2;
            }
        }

        visitBlock(node.getChild(-1));

        // Recover 需要放到visitBlock中
        // curValueTable = curValueTable.getParent();

        if (curFunction.getFunctionType() == 1) { // void function
            if (curBasicBlock.getInstructions().size() == 0) {
                IRPort.buildRetNoReturn(curBasicBlock);
            } else {
                Instruction lastInstr = curBasicBlock.getInstructions().
                        get(curBasicBlock.getInstructions().size() - 1);
                if (!((lastInstr instanceof TerminatorInstr) && ((TerminatorInstr)
                lastInstr).isRet())) {
                    IRPort.buildRetNoReturn(curBasicBlock);
                }
            }
        }
    }

    // FuncFParams -> FuncFParam { ',' FuncFParam }
    public void visitFuncFParams(ASTNode node) {
        int i = 0;
        ArrayList<Type> funcFParamsArray = new ArrayList<>();
        while (i < node.getChildrenSize()) {
            visitFuncFParam(node.getChild(i));
            funcFParamsArray.add(curType);
            i += 2;
        }
        curTypeArray = funcFParamsArray;
    }

    //  FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    public void visitFuncFParam(ASTNode node) {
        ASTNode ident = node.getChild(1);
        if (node.getChildrenSize() == 2) { // 形参是int，放进去的也是int
            curType = new IntegerType(32);
        } else { // 数组 放进去的是int[]*或[][]*
            if (irDebug) {
                System.out.println("Enter FuncFParam array");
            }
            Type type = new IntegerType(32);
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                ASTNode child = node.getChild(i);
                if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.ConstExp)) {
                    isConstant = true;
                    visitConstExp(child);
                    isConstant = false;
                    type = new ArrayType(type, curInt);
                }
            }
            curType = new PointerType(type);
        }
    }

    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    public void visitConstDef(ASTNode node) {
        // Ident
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

        // ConstInitVal
        if (curDimensions.size() == 0) {
            if (curValueTable.isRoot()) { // 全局变量
                isGlobalInit = true;
                visitConstInitVal(node.getChild(-1));
                isGlobalInit = false;
                GlobalVariable globalVariable = new GlobalVariable(ident.getToken().getValue(), (Constant) curValue,
                        true, module);
                curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
            } else { // 局部常量
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, new IntegerType(32), MemoryType.Alloca);
                visitConstInitVal(node.getChild(-1));
                MemoryInstr store = IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store,
                        curValue, alloca);
                curValueTable.addEntry(ident.getToken().getValue(), curValue);
            }
        } else { // 数组
            Type arrayType = new IntegerType(32);
            // 数组是一种层叠的模式
            for (int j = curDimensions.size() - 1; j >= 0; j--) {
                arrayType =new ArrayType(arrayType, curDimensions.get(j));
            }
            if (curValueTable.isRoot()) { // 全局变量
                isGlobalInit = true;
                isArrayInit = true;
                ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
                visitConstInitVal(node.getChild(-1));
                isArrayInit = false;
                isGlobalInit = false;

                ArrayList<Constant> constantArrayList = new ArrayList<>();

                // 全零初始化
                boolean isZero = true;
                for (Value value : curArray) {
                    if (((ConstantInt) value).getVal() != 0) {
                        isZero = false; // 数组中的元素有非零值
                    }
                    constantArrayList.add((ConstantInt) value);
                }

                int arraySize = 1;
                for (Integer dimension : dimensions) {
                    arraySize *= dimension;
                }

                GlobalVariable globalVariable;
                if (isZero) {
                    globalVariable = new GlobalVariable(ident.getToken().getValue(),
                            arrayType, module);
                } else {
                    for (int j = dimensions.size() - 1; j > 0; j--) {
                        int nowDimension = dimensions.get(j);
                        ArrayList<Constant> nowConstantArray = new ArrayList<>();

                        arraySize /= nowDimension;
                        for (int k = 0; k < arraySize; k++) {
                            ArrayList<Constant> array = new ArrayList<>();
                            for (int m = 0; m < nowDimension; m++) {
                                array.add(constantArrayList.get(k * nowDimension + m));
                            }
                            nowConstantArray.add(new ConstantArray(array));
                        }
                        constantArrayList = nowConstantArray;
                    }

                    ConstantArray initVal = new ConstantArray(constantArrayList);
                    globalVariable = new GlobalVariable(ident.getToken().getValue(), initVal, true, module);
                }

                curValueTable.addEntry(ident.getToken().getValue(), globalVariable);
                // module.addGlobalVariable(globalVariable);
            } else { // 局部数组变量
                MemoryInstr alloca = IRPort.buildMemoryInstr(curBasicBlock, arrayType, MemoryType.Alloca);
                curValueTable.addEntry(ident.getToken().getValue(), alloca);

                // 局部数组变量初始化
                ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
                isArrayInit = true;
                visitConstInitVal(node.getChild(-1));
                isArrayInit = false;

                ArrayList<Value> constantArray = new ArrayList<>();
                for (Value value : curArray) {
                    constantArray.add(value);
                }

                // getElementptr语句
                curArrayIndex = 0;
                constArrayInit(arrayType, alloca, dimensions, constantArray, 1);
                curArrayIndex = 0;

            }

            // visitConstInitVal(node.getChild(-1));
        }
    }

    private void constArrayInit(Type arrayType, Value base, ArrayList<Integer> dimensions, ArrayList<Value> constantArray, int depth) {
        for (int i = 0; i < dimensions.get(depth - 1); i++) {
            MemoryInstr getElementptr = IRPort.buildGetElementptr(((ArrayType) arrayType), curBasicBlock,
                    base, new ConstantInt(32, 0), new ConstantInt(32, i));
            if (depth == dimensions.size()) {
                IRPort.buildMemoryInstr(curBasicBlock, new VoidType(),
                        MemoryType.Store, constantArray.get(curArrayIndex++), getElementptr);
            } else {
                constArrayInit(((ArrayType)arrayType).getElementType(), getElementptr, dimensions, constantArray, depth + 1);
            }
        }
    }

    // ConstInitVal -> ConstExp
    //    | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public void visitConstInitVal(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter ConstInitVal");
            // System.out.println("dimension is " + dimension);
            if (node.getGrammerSymbol() != null) {
                System.out.println("ConstInitVal Node is " + node.getGrammerSymbol());
            } else {
                System.out.println("ConstInitVal Node is " + node.getToken().getSymbol());
            }
            node.printAllChildren();
        }
        if (node.getChildrenSize() == 1) {
            // ConstExp
            if (isGlobalInit || !isArrayInit) {
                isConstant = true;
                visitConstExp(node.getChild(0));
                isConstant = false;
            } else {
                visitConstExp(node.getChild(0));
            }

            curValue = new ConstantInt(32, curInt);
        } else {
            //  ConstInitVal -> '{' ConstInitVal '}'
            ArrayList<Value> array = new ArrayList<>();
            int arraySize = 1;
            for (int i = 0; i < curDimensions.size(); i++) {
                arraySize *= curDimensions.get(i);
            }
            for (ASTNode child : node.getChildren()) {
                if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.ConstInitVal)) {
                    if (child.getChildrenSize() == 1) {
                        visitConstInitVal(child);
                        array.add(curValue);
                    } else {
                        curDimensions = new ArrayList<>(Collections.singleton(curDimensions.remove(0)));
                        visitConstInitVal(child);
                        array.addAll(curArray);
                    }
                }
            }
            curArray = array;
        }
    }

    // ConstExp -> AddExp
    public void visitConstExp(ASTNode node) {
        visitAddExp(node.getChild(0));

        if (isConstant) {
            curValue = new ConstantInt(32, curInt);
        }
    }

    // MainFuncDef -> 'int' 'main' '(' ')' Block
    public void visitMainFuncDef(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter MainFuncDef");
        }

        ValueTable mainValueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(mainValueTable);
        curValueTable = mainValueTable;

        // Build Values.Constants.Function
        Function function = IRPort.buildFunctionInt("main", false, module);
        curFunction = function;

        // Build Values.BasicBlock
        BasicBlock basicBlock = IRPort.buildBasicBlock(function);
        curBasicBlock = basicBlock;

        // Block
        visitBlock(node.getChild(-1));

        // recover
        curValueTable = curValueTable.getParent();
    }

    // Block -> '{' { BlockItem } '}'
    public void visitBlock(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter Block");
        }
        if (isEnterCurTable) {
            isEnterCurTable = false;
        } else {
            ValueTable valueTable = new ValueTable(curValueTable, false);
            curValueTable.addChildTable(valueTable);
            curValueTable = valueTable;
        }

        int i = 1;
        while (i < node.getChildrenSize() - 1) {
            visitBlockItem(node.getChild(i));
            i++;
        }
        curValueTable = curValueTable.getParent();
    }

    // BlockItem -> Decl | Stmt
    public void visitBlockItem(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter BlockItem");
        }

        if (node.getChild(0).getGrammerSymbol() == GrammerSymbol.Decl) {
            visitDecl(node.getChild(0));
        } else {
            visitStmt(node.getChild(0));
        }
    }

    // private void visit

    // Stmt -> 'return' [Exp] ';'
    // | LVal '=' Exp ';' | [Exp] ';'
    public void visitStmt(ASTNode node) {
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
        if (first.getToken() != null && Objects.equals(first.getToken().getSymbol(), Symbol.RETURNTK)) {
            if (Objects.equals(node.getChild(1).getGrammerSymbol(), GrammerSymbol.Exp)) {
                visitExp(node.getChild(1));
                // 返回值是curInt(curTableEntry) ==> curValue
                curValue = IRPort.buildRetWithReturn(curBasicBlock, curValue);
            } else {
                // 返回值是void ==> null
                curValue = IRPort.buildRetNoReturn(curBasicBlock);
            }
        } else if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.LVal)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter LVal Branch");
            }
            // Stmt -> LVal '=' Exp ';'
            // Stmt -> LVal '=' 'getint''('')'';'
            visitLval(first);

            Value leftOp = curValue;
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
            visitBlock(first);
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
            visitIfStmt(node);
        } else if (first.getToken().getSymbol().equals(Symbol.FORTK)) {
            visitForBranchStmt(node);
        } else if (first.getToken().getSymbol().equals(Symbol.BREAKTK)) {
            visitBreakStmt(node);
        } else if (first.getToken().getSymbol().equals(Symbol.CONTINUETK)) {
            visitContinueStmt(node);
        }
        else if (first.getToken().getSymbol().equals(Symbol.PRINTFTK)) {
            if (irDebug) {
                System.out.println("Visitor From Stmt Enter Printf Branch");
            }
            // Stmt -> 'printf''('FormatString{,Exp}')'';'
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
        }
    }

    // LVal → Ident {'[' Exp ']'}
    public void visitLval(ASTNode node) {
        ASTNode ident = node.getChild(0);
        int length = node.getChildren().size(); // 1, 4, or 7

        Value value = curValueTable.getEntry(ident.getToken().getValue());
        if (irDebug) {
            curValueTable.printEntriesName();
            System.out.println("init LVal Type is " + value.getType());
        }
        if (value.getType() instanceof IntegerType) {
            // Ident
            curValue = value;
        } else if (value.getType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) value.getType();
            for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                ASTNode child = node.getChild(i);
                if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.Exp)) {
                    visitExp(child);
                    Value index = curValue;
                    curValue = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), index);

                    if (arrayType.getElementType() instanceof ArrayType) {
                        arrayType = (ArrayType) arrayType.getElementType();
                        if (i == node.getChildrenSize() - 2) {
                            value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), new ConstantInt(32, 0));
                        }
                    }
                }
            }

            curValue = value;
        } else if (value.getType() instanceof PointerType) { // 处理函数参数
            Type type = ((PointerType) value.getType()).getPointToType();
            if (type instanceof IntegerType) { // 函数参数是int*一维数组
                if (irDebug) {
                    System.out.println("Enter LVal PointerType IntegerType");
                }
                curValue = value;

            } else if (type instanceof ArrayType) {
                if (irDebug) {
                    System.out.println("Enter LVal PointerType ArrayType");
                }
                ArrayType arrayType = (ArrayType) type;
                if (isGlobalInit || isConstant) {
                    // 直接取值
                    value = ((GlobalVariable) value).getVal();
                    for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                        ASTNode child = node.getChild(i);
                        if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.Exp)) {
                            visitExp(child);
                            value = ((ConstantArray) value).getUsedValue(curInt);
                        }
                    }
                } else {
                    if (irDebug) {
                        System.out.println("Enter LVal PointerType ArrayType !isConstant");
                    }
                    // 二维数组取基址
                    if (node.getChildrenSize() == 1) {
                        value = IRPort.buildGetElementptr((ArrayType) type, curBasicBlock, value,
                                new ConstantInt(32, 0), new ConstantInt(32, 0));
                    } else {
                        for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                            ASTNode child = node.getChild(i);
                            if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.Exp)) {
                                visitExp(child);
                                Value index = curValue;
                                value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), index);

                                if (arrayType.getElementType() instanceof ArrayType) {
                                    arrayType = (ArrayType) arrayType.getElementType();
                                    if (i == node.getChildrenSize() - 2) {
                                        value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), new ConstantInt(32, 0));
                                    }
                                }
                            }
                        }
                    }
                }
                curValue = value;
            } else { // PointerType
                if (irDebug) {
                    System.out.println("Enter LVal PointerType PointerType");
                    System.out.println("type is " + type);
                }
                PointerType pointerType = (PointerType) type;
                MemoryInstr load = IRPort.buildMemoryInstr(curBasicBlock, pointerType, MemoryType.Load, value);
                value = load;
                ArrayType arrayType = null;

                if (node.getChildrenSize() >= 3 && Objects.equals(node.getChild(2).getGrammerSymbol(), GrammerSymbol.Exp)) {
                    visitExp(node.getChild(2));
                    Value index = curValue;
                    value = IRPort.buildPointerGetElementptr(pointerType, curBasicBlock, load, index);

                    if (pointerType.getPointToType() instanceof ArrayType) {
                        arrayType = (ArrayType) pointerType.getPointToType();

                        // LVal → Ident {'[' Exp ']'}
                        // 二维数组a[2][2]取a[2]的情况
                        if (node.getChildrenSize() == 4) {
                            value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), new ConstantInt(32, 0));
                        }
                    }

                    for (int i = 4; i < node.getChildrenSize() - 1; i++) {
                        ASTNode child = node.getChild(i);

                        if (Objects.equals(child.getGrammerSymbol(), GrammerSymbol.Exp)) {
                            visitExp(child);
                            index = curValue;
                            value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), index);

                            if (arrayType.getElementType() instanceof ArrayType) {
                                arrayType = (ArrayType) arrayType.getElementType();

                                if (i == node.getChildrenSize() - 2) {
                                    value = IRPort.buildGetElementptr(arrayType, curBasicBlock, value, new ConstantInt(32, 0), new ConstantInt(32, 0));
                                }
                            }
                        }
                    }
                }
                curValue = value;
            }
        }

    }

    // Exp -> AddExp
    public void visitExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter Exp");
        }
        visitAddExp(node.getChild(0));
    }

    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // AddExp -> MulExp { ('+' | '-') MulExp }
    public void visitAddExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter AddExp");
            System.out.println("AddExp children size is " + node.getChildrenSize());
            System.out.println("node is " + node.getGrammerSymbol());
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
                if (irDebug) {
                    System.out.println("Enter AddExp isConstant!");
                    System.out.println("curInt is: " + curInt);
                }
                if (node.getChild(1).getToken().getSymbol() == Symbol.PLUS) {
                    curInt = leftNum + curInt;
                } else {
                    curInt = leftNum - curInt;
                }
            } else {
                // 生成IR
                if (irDebug) {
                    System.out.println("Enter AddExp !isConstant!");
                }
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
            System.out.println("node is " + node.getGrammerSymbol());
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

    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')'
    //        | UnaryOp UnaryExp
    public void visitUnaryExp(ASTNode node) {
        if (irDebug) {
            System.out.println("Enter UnaryExp");
            System.out.println("node is " + node.getGrammerSymbol());
        }
        ASTNode first = node.getChild(0);
        if (irDebug) {
            if (first.getGrammerSymbol() != null) {
                System.out.println("first node is " + first.getGrammerSymbol());
            } else if (first.getToken() != null) {
                System.out.println("first node is " + first.getToken().getSymbol());
            }

        }
        if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.PrimaryExp)) {
            visitPrimaryExp(first);
        } else if (Objects.equals(first.getGrammerSymbol(), GrammerSymbol.UnaryOp)) {
            // UnaryOp -> '+' | '−' | '!'
            int op = first.getChild(0).getToken().getSymbol().equals(Symbol.PLUS) ? 1 :
                    first.getChild(0).getToken().getSymbol().equals(Symbol.MINU) ? -1 : 2;
            visitUnaryExp(node.getChild(1));
            if (isConstant) { // 无函数调用
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
            Function callee = module.getFunction("@" + first.getToken().getValue());
            ArrayList<Value> args = new ArrayList<>();
            ArrayList<Type> argTypes = callee.getParamsType(); // 处理函数参数是否匹配
            if (Objects.equals(node.getChild(2).getGrammerSymbol(), GrammerSymbol.FuncRParams)) { // 如果有实参
                visitFuncRParams(node.getChild(2), args, argTypes);
            }

            if (callee.getFunctionType() == 1) { // void function
                args.add(0, callee);
                curValue = IRPort.buildCallWithNoReturn(curBasicBlock, callee, args.toArray(new Value[args.size()]));
            } else { // int function
                args.add(0, callee);
                curValue = IRPort.buildCallWithReturn(curBasicBlock, callee, args.toArray(new Value[args.size()]));
            }
        }
    }

    // FuncRParams -> Exp { ',' Exp }
    public void visitFuncRParams(ASTNode node, ArrayList<Value> args, ArrayList<Type> argTypes) {
        int argCnt = 0;
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            if (!(argTypes.get(argCnt) instanceof  IntegerType)) { // 说明是数组
                isCallingFunc = true;
            }
            visitExp(node.getChild(i));
            isCallingFunc = false;
            args.add(curValue);
            argCnt++;
        }
    }

    // PrimaryExp -> '(' Exp ')' | LVal | Number
    public void visitPrimaryExp(ASTNode node) {
        if (node.getChildrenSize() > 1) {
            // '(' Exp ')'
            visitExp(node.getChild(1));
        } else if (Objects.equals(node.getChild(0).getGrammerSymbol(), GrammerSymbol.Number)) {
            visitNumber(node.getChild(0));
        } else if (Objects.equals(node.getChild(0).getGrammerSymbol(), GrammerSymbol.LVal)) {
            if (isConstant) { // 说明是全局变量，需要改变curInt 或者局部常量，进行分类讨论
                visitLval(node.getChild(0));
                // curInt = ((ConstantInt) curValue).getVal();
                if (curValue instanceof  GlobalVariable) {
                    curInt = ((ConstantInt)(((GlobalVariable)curValue).getVal())).getVal();
                } else {
                    curInt = ((ConstantInt) curValue).getVal();
                }

            } else {
                boolean nowIsCallingFunc = isCallingFunc;
                if (isCallingFunc) {
                    isCallingFunc = false;
                }
                visitLval(node.getChild(0));
                if (!nowIsCallingFunc) { // 数组作为函数参数不应load
                    if (!(curValue.getType() instanceof IntegerType)) { // 全局变量在函数内被调用
                        if (((PointerType) curValue.getType()).getPointToType() instanceof IntegerType) {
                            curValue = IRPort.buildMemoryInstr(curBasicBlock,new IntegerType(32),
                                    MemoryType.Load, curValue);
                        }
                    }
                }
            }
        }
    }

    // Number -> IntConst
    public void visitNumber(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter Number");
        }
        curInt = Integer.parseInt(node.getChild(0).getToken().getValue());
        curValue = new ConstantInt(32, curInt);
    }

    // Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public void visitIfStmt(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter IfStmt");
        }
        ASTNode cond = node.getChild(2);
        ASTNode stmt = node.getChild(4);
        ASTNode elseStmt = null;
        if (Objects.equals(node.getChild(-2).getToken().getSymbol(), Symbol.ELSETK)) {
            elseStmt = node.getChild(-1);
        }
        // ASTNode elseStmt = node.getChild(6);
        BasicBlock thenBlock = IRPort.buildBasicBlock(curFunction);
        BasicBlock elseBlock = IRPort.buildBasicBlock(curFunction);
        BasicBlock mergeBlock = null;

        if (elseStmt != null) {
            mergeBlock = IRPort.buildBasicBlock(curFunction);
        } else {
            mergeBlock = elseBlock;
        }

        curIfBasicBlock = thenBlock;
        curElseBasicBlock = elseBlock;

        visitCond(cond);

        curBasicBlock = thenBlock;

        boolean ifHasReturn = false;
        boolean elseHasReturn = false;
        // 指示if, else 语句是否含有return或分支等终结句
        if(irDebug) {
            System.out.println("ifStmt is " + stmt.getGrammerSymbol());
        }
        visitStmt(stmt);

        if (!curBasicBlock.getInstructions().isEmpty()) {
            Instruction lastInstr = curBasicBlock.getInstructions().get(curBasicBlock.getInstructions().size() - 1);
            if (lastInstr instanceof TerminatorInstr && ((TerminatorInstr) lastInstr).isBrOrRet()) {
                ifHasReturn = true;
            } else {
                IRPort.buildBrNoCondition(curBasicBlock, mergeBlock);
            }
        } else  {
            IRPort.buildBrNoCondition(curBasicBlock, mergeBlock);
        }

        if (elseStmt != null) {
            curBasicBlock = elseBlock;
            visitStmt(elseStmt);

            if (!curBasicBlock.getInstructions().isEmpty()) {
                Instruction lastInstr = curBasicBlock.getInstructions().get(curBasicBlock.getInstructions().size() - 1);
                if (lastInstr instanceof TerminatorInstr && ((TerminatorInstr) lastInstr).isBrOrRet()) {
                    elseHasReturn = true;
                } else {
                    IRPort.buildBrNoCondition(curBasicBlock, mergeBlock);
                }
            } else  {
                IRPort.buildBrNoCondition(curBasicBlock, mergeBlock);
            }
        }

        if (ifHasReturn && elseHasReturn) {
            curFunction.getBasicBlocks().remove(mergeBlock);
        } else {
            curBasicBlock = mergeBlock;
        }
    }

    // Cond -> LOrExp
    public void visitCond(ASTNode node) {
        visitLOrExp(node.getChild(0), true);
    }

    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    // 注意要短路求值
    // 例如 A || B || C
    // LOrExp -> LOrExp || LAndExp -> LOrExp || LAndExp || LAndExp -> LAndExp '||' LAndExp '||' LAndExp
    public void visitLOrExp(ASTNode node, boolean first) {// 标识是否是与Cond相连的第一个LOrExp
        BasicBlock ifBlock = curIfBasicBlock; // 条件为真时需要跳转的目标基本块
        BasicBlock elseBlock = curElseBasicBlock; // 条件为假时需要跳转的目标基本块

        if(node.getChildrenSize() == 1) { // 只有一个LAndExp
            if (!first) { // first = false 意味着正在处理第一个LAndExp
                BasicBlock nextBlock = IRPort.buildBasicBlock(curFunction);
                curElseBasicBlock = nextBlock;
                visitLAndExp(node.getChild(0), true);
                curBasicBlock = nextBlock;
            } else {
                visitLAndExp(node.getChild(0), true);
            }
        } else {
            visitLOrExp(node.getChild(0), false);

            if (!first) {
                BasicBlock nextBlock = IRPort.buildBasicBlock(curFunction);
                curElseBasicBlock = nextBlock;
                visitLAndExp(node.getChild(2), true);
                curBasicBlock = nextBlock;
            } else {
                visitLAndExp(node.getChild(2), true);
            }
        }

        curIfBasicBlock = ifBlock;
        curElseBasicBlock = elseBlock;
    }
    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    // 注意要短路求值
//    public void visitLOrExp(ASTNode node) {
//        BasicBlock trueBlock = curIfBasicBlock; // 条件为真时需要跳转的目标基本块
//        BasicBlock falseBlock = curElseBasicBlock; // 条件为假时需要跳转的目标基本块
//
//        if (node.getChildrenSize() == 1) {
//            visitLAndExp(node.getChild(0));
//        } else {
//            BasicBlock newBasicBlock = IRPort.buildBasicBlock(curFunction); // 为 <LAndExp> 新建一个基本块
//            curElseBasicBlock = newBasicBlock;
//            visitLOrExp(node.getChild(0));
//            curElseBasicBlock = falseBlock;
//            curBasicBlock = newBasicBlock;
//            visitLAndExp(node);
//        }
//    }

    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public void visitRelExp(ASTNode node, boolean first) {
        if (irDebug) {
            System.out.println("Visitor Enter RelExp");
            System.out.println("node.childrenSize() is " + node.getChildrenSize());
            System.out.println("node is " + node.getGrammerSymbol());
        }
        if (node.getChildrenSize() == 1) { // AddExp
            if (!first) {
                isSingleJudge = false;
            }
            visitAddExp(node.getChild(0));
        } else {
            visitRelExp(node.getChild(0), false);
            Value leftOp = curValue;
            visitAddExp(node.getChild(2));

            Symbol op = node.getChild(1).getToken().getSymbol();
            if (op.equals(Symbol.LSS)) { // <
                curValue = IRPort.buildIcmp(curBasicBlock, "slt", leftOp, curValue);
            } else if (op.equals(Symbol.GRE)) { // >
                curValue = IRPort.buildIcmp(curBasicBlock, "sgt", leftOp, curValue);
            } else if (op.equals(Symbol.LEQ)) { // <=
                curValue = IRPort.buildIcmp(curBasicBlock, "sle", leftOp, curValue);
            } else { // >=
                curValue = IRPort.buildIcmp(curBasicBlock, "sge", leftOp, curValue);
            }
        }
    }

    // EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    public void visitEqExp(ASTNode node, boolean first) {
        if (irDebug) {
            System.out.println("Enter eqExp");
            System.out.println("node.childrenSize() is " + node.getChildrenSize());
            System.out.println("node is " + node.getGrammerSymbol());
        }
        if (node.getChildrenSize() == 1) {
            if (!first) {
                isSingleJudge = false;
            }
            visitRelExp(node.getChild(0), true);
        } else {
            visitEqExp(node.getChild(0), false);
            Value leftOp = curValue;
            visitRelExp(node.getChild(2), true);

            Symbol op = node.getChild(1).getToken().getSymbol();
            if (op.equals(Symbol.EQL)) {
                curValue = IRPort.buildIcmp(curBasicBlock, "eq", leftOp, curValue);
            } else {
                curValue = IRPort.buildIcmp(curBasicBlock, "ne", leftOp, curValue);
            }
        }
    }

    // LAndExp -> EqExp || LAndExp '&&' EqExp
    // 注意短路求值
    public void visitLAndExp(ASTNode node, boolean first) { // 标识是否是与LOrExp直接相连的第一个LAndExp
        BasicBlock ifBlock = curIfBasicBlock; // 条件为真时需要跳转的目标基本块
        BasicBlock elseBlock = curElseBasicBlock; // 条件为假时需要跳转的目标基本块

        if (node.getChildrenSize() == 1) { // LAndExp -> EqExp
            if (first) { // first = true 意味着正在处理第一个EqExp 结果为真时跳转到ifBlock
                isSingleJudge = true;
                visitEqExp(node.getChild(0), true);

                if (isSingleJudge) {
                    curValue = IRPort.buildIcmp(curBasicBlock, "ne", curValue, new ConstantInt(32, 0));
                    isSingleJudge = false;
                }


                IRPort.buildBrWithCondition(curBasicBlock, curValue, ifBlock, elseBlock);
                curBasicBlock = ifBlock;
            } else {
                BasicBlock nextBlock = IRPort.buildBasicBlock(curFunction);

                isSingleJudge = true;
                visitEqExp(node.getChild(0), true);

                if (isSingleJudge) {
                    curValue = IRPort.buildIcmp(curBasicBlock, "ne", curValue, new ConstantInt(32, 0));
                    isSingleJudge = false;
                }

                IRPort.buildBrWithCondition(curBasicBlock, curValue, nextBlock, elseBlock);
                curBasicBlock = nextBlock;
            }
        } else {
            visitLAndExp(node.getChild(0), false);

            BasicBlock nextBlock = IRPort.buildBasicBlock(curFunction);
            isSingleJudge = true;
            visitEqExp(node.getChild(2), true);

            if (isSingleJudge) {
                curValue = IRPort.buildIcmp(curBasicBlock, "ne", curValue, new ConstantInt(32, 0));
                isSingleJudge = false;
            }

            IRPort.buildBrWithCondition(curBasicBlock, curValue, nextBlock, elseBlock);
            curBasicBlock = nextBlock;
            if (first) {
                IRPort.buildBrNoCondition(curBasicBlock, ifBlock);
            }
        }
    }

    /*public void visitEqExp(ASTNode node, boolean first) {

    }*/
    // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    // ForStmt -> LVal '=' Exp // 直接进Stmt不行差个分号
    public void visitForBranchStmt(ASTNode node) {
        BasicBlock breakBasicBlock = curBreakBasicBlock;
        BasicBlock continueBasicBlock = curContinueBasicBlock;

        BasicBlock condBlock = IRPort.buildBasicBlock(curFunction);
        BasicBlock bodyBlock = IRPort.buildBasicBlock(curFunction);
        BasicBlock stepBlock = IRPort.buildBasicBlock(curFunction);
        BasicBlock mergeBlock = IRPort.buildBasicBlock(curFunction);

        if (Objects.equals(node.getChild(2).getGrammerSymbol(), GrammerSymbol.ForStmt)) {
            visitForStmt(node.getChild(2));
        }

        IRPort.buildBrNoCondition(curBasicBlock, condBlock);
//        if (Objects.equals(node.getChild(4).getGrammerSymbol(), GrammerSymbol.Cond) ||
//                Objects.equals(node.getChild(3).getGrammerSymbol(), GrammerSymbol.Cond)) {
//            condBlock = IRPort.buildBasicBlock(curFunction);
//        }
//
//        if (condBlock != null) {
//            IRPort.buildBrNoCondition(curBasicBlock, condBlock);
//        } else {
//            IRPort.buildBrNoCondition(curBasicBlock, bodyBlock);
//        }

        curIfBasicBlock = bodyBlock;
        curElseBasicBlock = mergeBlock;


        curBasicBlock = condBlock;


        if (Objects.equals(node.getChild(4).getGrammerSymbol(), GrammerSymbol.Cond)) {
            visitCond(node.getChild(4));
        } else if (Objects.equals(node.getChild(3).getGrammerSymbol(), GrammerSymbol.Cond)) {
            visitCond(node.getChild(3));
        } else {
            if (curBasicBlock.getInstructions().isEmpty()) {
                IRPort.buildBrNoCondition(curBasicBlock, bodyBlock);
            }
        }

//        if (curBasicBlock.getInstructions().isEmpty()) {
//            IRPort.buildBrNoCondition(curBasicBlock, bodyBlock);
//        }

        curBreakBasicBlock = mergeBlock;
        curContinueBasicBlock = stepBlock;

        curBasicBlock = bodyBlock;
        visitStmt(node.getChild(-1));

        if (curBasicBlock.getInstructions().isEmpty()) {
            IRPort.buildBrNoCondition(curBasicBlock, stepBlock);
        } else {
            Instruction lastInstr = curBasicBlock.getInstructions().get(curBasicBlock.getInstructions().size() - 1);
            if (!(lastInstr instanceof TerminatorInstr && ((TerminatorInstr) lastInstr).isBrOrRet())) {
                IRPort.buildBrNoCondition(curBasicBlock, stepBlock);
            }
        }

        curBasicBlock = stepBlock;
        if (node.getChildrenSize() >= 7 && Objects.equals(node.getChild(6).getGrammerSymbol(), GrammerSymbol.ForStmt)) {
            visitForStmt(node.getChild(6));
        } else if (node.getChildrenSize() >= 6 && Objects.equals(node.getChild(5).getGrammerSymbol(), GrammerSymbol.ForStmt)) {
            visitForStmt(node.getChild(5));
        } else if (Objects.equals(node.getChild(4).getGrammerSymbol(), GrammerSymbol.ForStmt)) {
            visitForStmt(node.getChild(4));
        }

        if (curBasicBlock.getInstructions().isEmpty()) {
            IRPort.buildBrNoCondition(curBasicBlock, condBlock);
        } else {
            Instruction lastInstr = curBasicBlock.getInstructions().get(curBasicBlock.getInstructions().size() - 1);
            if (!(lastInstr instanceof TerminatorInstr && ((TerminatorInstr) lastInstr).isBrOrRet())) {
                IRPort.buildBrNoCondition(curBasicBlock, condBlock);
            }
        }

        curBreakBasicBlock = breakBasicBlock;
        curContinueBasicBlock = continueBasicBlock;

        curBasicBlock = mergeBlock;
    }

    // ForStmt -> LVal '=' Exp
    public void visitForStmt(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter ForStmt");
            System.out.println("node is " + node.getGrammerSymbol());
        }

        ASTNode first = node.getChild(0);
        visitLval(first);
        Value leftOp = curValue;
        visitExp(node.getChild(2));
        Value rightOp = curValue;
        IRPort.buildMemoryInstr(curBasicBlock, new VoidType(), MemoryType.Store, rightOp, leftOp);
    }

    // Stmt -> 'break' ';'
    public void visitBreakStmt(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter BreakStmt");
        }
        IRPort.buildBrNoCondition(curBasicBlock, curBreakBasicBlock);
    }

    // Stmt -> 'continue' ';'
    public void visitContinueStmt(ASTNode node) {
        if (irDebug) {
            System.out.println("Visitor Enter ContinueStmt");
        }
        IRPort.buildBrNoCondition(curBasicBlock, curContinueBasicBlock);
    }

}
