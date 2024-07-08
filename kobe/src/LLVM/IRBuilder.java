package LLVM;

import LLVM.Elements.ConstArray;
import LLVM.Elements.ConstInt;
import LLVM.Elements.Element;
import LLVM.Elements.Function;
import LLVM.Elements.GlobalVar;
import LLVM.Instructions.IRMaker;
import LLVM.Instructions.Instruction;
import LLVM.Instructions.MemoryInstruction;
import LLVM.Instructions.MemoryType;
import LLVM.Instructions.TerminatorInstruction;
import Lexer.Token;
import Parser.ASTNode;
import Parser.GrammarSymbol;
import Table.TableEntry;
import Table.ValueTable;
import Types.Array;
import Types.Int;
import Types.Pointer;
import Types.Type;
import Types.Void;

import java.util.ArrayList;
import java.util.Collections;

public class IRBuilder {
    private final ASTNode root;
    private final Module module = new Module();
    private Value curValue = null;
    private Function curFunction = null;
    private BasicBlock curBasicBlock = null;
    private boolean isGlobalInit = false;
    private ValueTable curValueTable = new ValueTable(null, true);
    private boolean isCallingFunc = false;
    private int curInt = 0;
    private boolean parsingConstant = false;
    private ArrayList<Integer> curDimensions = new ArrayList<>();
    private Type curType = null;
    private ArrayList<Type> curTypeArray = new ArrayList<>();
    private BasicBlock curIfBasicBlock = null;
    private BasicBlock curElseBasicBlock = null;
    private BasicBlock curBreakBasicBlock = null;
    private BasicBlock curContinueBasicBlock = null;
    private boolean isSingle = false;
    private boolean isArrayInit = false;
    private ArrayList<Value> curArray = new ArrayList<>();
    private int curArrayIndex = -1;
    private boolean isEnterCurTable = false;

    public IRBuilder(ASTNode root) {
        this.root = root;
    }

    public String toString() {
        return module.toString();
    }

    public void build() {
        visitCompUnit();
    }

    // 编译单元 CompUnit -> {Decl} {FuncDef} MainFuncDef
    private void visitCompUnit() {
        for (ASTNode child : root.getChildren()) {
            switch (child.getGrammarSymbol()) {
                case Decl:
                    visitDecl(child);
                    break;
                case FuncDef:
                    visitFuncDef(child);
                    break;
                case MainFuncDef:
                    visitMainFuncDef(child);
                    break;
                default:
                    break;
            }
        }
    }

    // 声明 Decl -> ConstDecl | VarDecl
    private void visitDecl(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammarSymbol() == GrammarSymbol.ConstDecl) {
                visitConstDecl(child);
            } else {
                visitVarDecl(child);
            }
        }
    }

    // 常量声明 ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private void visitConstDecl(ASTNode node) {
        for (int i = 2; i < node.getChildrenSize() - 1; i += 2) {
            visitConstDef(node.getChild(i));
        }
    }

    // 常数定义 ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private void visitConstDef(ASTNode node) {
        ASTNode ident = node.getChild(0);
        int l = node.getChildrenSize();
        curDimensions.clear();
        for (int i = 1; i < l - 2 && node.getChild(i).getToken().
                getType() == Token.TokenType.LBRACK; i += 3) {
            parsingConstant = true;
            visitConstExp(node.getChild(i + 1));
            parsingConstant = false;
            curDimensions.add(curInt);
        }

        if (curDimensions.isEmpty()) {
            if (curValueTable.isRoot()) {
                isGlobalInit = true;
                visitConstInitVal(node.getChild(-1));
                isGlobalInit = false;
                GlobalVar globalVar = new GlobalVar(ident.getToken().getValue(),
                        (Element) curValue, true, module);
                curValueTable.addEntry(ident.getToken().getValue(), globalVar);
            } else {
                MemoryInstruction alloc = IRMaker.makeMemoryInstruction(curBasicBlock,
                        new Int(32), MemoryType.Alloca);
                visitConstInitVal(node.getChild(-1));
                IRMaker.makeMemoryInstruction(curBasicBlock, new Void(), MemoryType.Store, curValue, alloc);
                curValueTable.addEntry(ident.getToken().getValue(), curValue);
            }
            return;
        }

        Type array = new Int(32);
        for (int i = curDimensions.size() - 1; i >= 0; i--) {
            array = new Array(array, curDimensions.get(i));
        }
        if (curValueTable.isRoot()) {
            isGlobalInit = true;
            isArrayInit = true;
            ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
            visitConstInitVal(node.getChild(-1));
            isGlobalInit = false;
            isArrayInit = false;
            ArrayList<Element> elements = new ArrayList<>();
            boolean allZero = true;

            for (Value value : curArray) {
                ConstInt constInt = (ConstInt) value;
                if (constInt.getValue() != 0) {
                    allZero = false;
                }
                elements.add(constInt);
            }

            int size = 1;
            for (int d : dimensions) {
                size *= d;
            }
            GlobalVar globalVar;
            if (allZero) {
                globalVar = new GlobalVar(ident.getToken().getValue(), array, module);
            } else {
                if (dimensions.size() == 2) {
                    int d = dimensions.get(1);
                    ArrayList<Element> constArray = new ArrayList<>();
                    size /= d;
                    for (int j = 0; j < size; j++) {
                        ArrayList<Element> list = new ArrayList<>();
                        for (int k = 0; k < d; k++) {
                            list.add(elements.get(j * d + k));
                        }
                        constArray.add(new ConstArray(list));
                    }
                    elements = constArray;
                }
                globalVar = new GlobalVar(ident.getToken().getValue(), new ConstArray(elements), true, module);
            }
            curValueTable.addEntry(ident.getToken().getValue(), globalVar);
            return;
        }

        MemoryInstruction alloc = IRMaker.makeMemoryInstruction(curBasicBlock, array, MemoryType.Alloca);
        curValueTable.addEntry(ident.getToken().getValue(), alloc);
        ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
        isArrayInit = true;
        visitConstInitVal(node.getChild(-1));
        isArrayInit = false;

        ArrayList<Value> constantArray = new ArrayList<>(curArray);
        curArrayIndex = 0;
        constArrayInit(array, alloc, dimensions, constantArray, 1);
        curArrayIndex = 0;
    }

    private void constArrayInit(Type arrayType, Value baseInstruction, ArrayList<Integer> dimensions,
                                ArrayList<Value> constantArray, int depth) {
        for (int i = 0; i < dimensions.get(depth - 1); i++) {
            MemoryInstruction instruction = IRMaker.makeGetElementptr(((Array) arrayType), curBasicBlock,
                    baseInstruction, new ConstInt(32, 0), new ConstInt(32, i));
            if (depth == dimensions.size()) {
                IRMaker.makeMemoryInstruction(curBasicBlock, new Void(),
                        MemoryType.Store, constantArray.get(curArrayIndex), instruction);
                curArrayIndex++;
            } else {
                constArrayInit(((Array) arrayType).getElementsType(), instruction,
                        dimensions, constantArray, depth + 1);
            }
        }
    }

    // 常量初值 ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private void visitConstInitVal(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            if (isGlobalInit || !isArrayInit) {
                parsingConstant = true;
                visitConstExp(node.getChild(0));
                parsingConstant = false;
            } else {
                visitConstExp(node.getChild(0));
            }
        } else {
            ArrayList<Value> list = new ArrayList<>();
            for (ASTNode child : node.getChildren()) {
                if (child.getGrammarSymbol() == GrammarSymbol.ConstInitVal) {
                    if (child.getChildrenSize() == 1) {
                        visitConstInitVal(child);
                        list.add(curValue);
                    } else {
                        curDimensions = new ArrayList<>(Collections.singleton(curDimensions.remove(0)));
                        visitConstInitVal(child);
                        list.addAll(curArray);
                    }
                }
            }
            curArray = list;
        }
    }

    // 变量声明 VarDecl -> BType VarDef { ',' VarDef } ';'
    private void visitVarDecl(ASTNode node) {
        for (int i = 1; i < node.getChildrenSize() - 1; i += 2) {
            visitVarDef(node.getChildren().get(i));
        }
    }

    // 变量定义 VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private void visitVarDef(ASTNode node) {
        ASTNode ident = node.getChild(0);
        int l = node.getChildrenSize();
        curDimensions.clear();
        for (int i = 1; i < l - 2 && node.getChild(i).getToken().
                getType() == Token.TokenType.LBRACK; i += 3) {
            parsingConstant = true;
            visitConstExp(node.getChild(i + 1));
            parsingConstant = false;
            curDimensions.add(curInt);
        }

        if (curDimensions.isEmpty()) {
            if (curValueTable.isRoot()) {
                GlobalVar globalVar;
                if (node.getChild(-1).getGrammarSymbol() == GrammarSymbol.InitVal) {
                    isGlobalInit = true;
                    visitInitVal(node.getChild(-1));
                    isGlobalInit = false;
                    globalVar = new GlobalVar(ident.getToken().getValue(),
                            (Element) curValue, false, module);
                } else {
                    globalVar = new GlobalVar(ident.getToken().getValue(), new Int(32), module);
                }
                curValueTable.addEntry(ident.getToken().getValue(), globalVar);
            } else {
                MemoryInstruction instruction = IRMaker.makeMemoryInstruction(curBasicBlock,
                        new Int(32), MemoryType.Alloca);
                curValueTable.addEntry(ident.getToken().getValue(), instruction);
                if (node.getChild(-1).getGrammarSymbol() == GrammarSymbol.InitVal) {
                    visitInitVal(node.getChild(-1));
                    MemoryInstruction store = IRMaker.makeMemoryInstruction(curBasicBlock, new Void(),
                            MemoryType.Store, curValue, instruction);
                }
            }
            return;
        }

        Type array = new Int(32);
        for (int i = curDimensions.size() - 1; i >= 0; i--) {
            array = new Array(array, curDimensions.get(i));
        }
        if (curValueTable.isRoot()) {
            if (node.getChild(-1).getGrammarSymbol() != GrammarSymbol.InitVal) {
                GlobalVar globalVar = new GlobalVar(ident.getToken().getValue(), array, module);
                curValueTable.addEntry(ident.getToken().getValue(), globalVar);
            } else {
                isGlobalInit = true;
                ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
                visitInitVal(node.getChild(-1));
                isGlobalInit = false;
                ArrayList<Element> elements = new ArrayList<>();
                boolean allZero = true;

                for (Value value : curArray) {
                    ConstInt constInt = (ConstInt) value;
                    if (constInt.getValue() != 0) {
                        allZero = false;
                    }
                    elements.add(constInt);
                }

                int size = 1;
                for (int d : dimensions) {
                    size *= d;
                }
                GlobalVar globalVar;
                if (allZero) {
                    globalVar = new GlobalVar(ident.getToken().getValue(), array, module);
                } else {
                    if (dimensions.size() == 2) {
                        int d = dimensions.get(1);
                        ArrayList<Element> constArray = new ArrayList<>();
                        size /= d;
                        for (int j = 0; j < size; j++) {
                            ArrayList<Element> list = new ArrayList<>();
                            for (int k = 0; k < d; k++) {
                                list.add(elements.get(j * d + k));
                            }
                            constArray.add(new ConstArray(list));
                        }
                        elements = constArray;
                    }
                    globalVar = new GlobalVar(ident.getToken().getValue(), new ConstArray(elements), false, module);
                }
                curValueTable.addEntry(ident.getToken().getValue(), globalVar);
            }
            return;
        }

        MemoryInstruction alloc = IRMaker.makeMemoryInstruction(curBasicBlock, array, MemoryType.Alloca);
        curValueTable.addEntry(ident.getToken().getValue(), alloc);
        if (node.getChild(-1).getGrammarSymbol() == GrammarSymbol.InitVal) {
            ArrayList<Integer> dimensions = new ArrayList<>(curDimensions);
            isArrayInit = true;
            visitInitVal(node.getChild(-1));
            isArrayInit = false;

            ArrayList<Value> values = new ArrayList<>(curArray);
            curArrayIndex = 0;
            constArrayInit(array, alloc, dimensions, values, 1);
            curArrayIndex = 0;
        }
    }

    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void visitInitVal(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            if (isGlobalInit) {
                parsingConstant = true;
                visitExp(node.getChild(0));
                parsingConstant = false;
                curValue = new ConstInt(32, curInt);
            } else {
                visitExp(node.getChild(0));
            }
            return;
        }
        ArrayList<Value> array = new ArrayList<>();
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammarSymbol() != GrammarSymbol.InitVal) {
                continue;
            }
            if (child.getChildrenSize() == 1) {
                if (isGlobalInit) {
                    parsingConstant = true;
                    visitInitVal(node.getChild(0));
                    parsingConstant = false;
                } else {
                    visitInitVal(node.getChild(0));
                }
                array.add(curValue);
            } else {
                visitInitVal(child);
                array.addAll(curArray);
            }
        }
        curArray = array;
    }

    // 函数定义 FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    private void visitFuncDef(ASTNode node) {
        ASTNode funcType = node.getChild(0).getChild(0);
        ASTNode ident = node.getChild(1);

        ArrayList<Type> fParams = new ArrayList<>();
        if (node.getChild(3).getGrammarSymbol() == GrammarSymbol.FuncFParams) {
            visitFuncFParams(node.getChild(3));
            fParams.addAll(curTypeArray);
        }

        Function function;
        if (funcType.getToken().getType() == Token.TokenType.INTTK) {
            function = IRMaker.makeNewFunctionInt(ident.getToken().getValue(), false, module, fParams);
        } else {
            function = IRMaker.makeNewFunctionVoid(ident.getToken().getValue(), false, module, fParams);
        }

        curFunction = function;
        curBasicBlock = IRMaker.makeBasicBlock(function);
        ValueTable valueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(valueTable);

        curValueTable.addEntry(ident.getToken().getValue(), null);
        curValueTable = valueTable;
        isEnterCurTable = true;

        ASTNode fParamsNode = node.getChild(3);
        for (int i = 0, cnt = 0; i < fParams.size(); i++, cnt += 2) {
            if (fParamsNode.getChild(cnt).getChildrenSize() == 2) {
                MemoryInstruction alloc = IRMaker.makeMemoryInstruction(curBasicBlock,
                        new Int(32), MemoryType.Alloca);
                MemoryInstruction store = IRMaker.makeMemoryInstruction(curBasicBlock, new Void(),
                        MemoryType.Store, curFunction.getArguments().get(i), alloc);
                curValueTable.addEntry(fParamsNode.getChild(cnt).getChild(1).getToken().getValue(), alloc);
            } else {
                MemoryInstruction alloc = IRMaker.makeMemoryInstruction(curBasicBlock,
                        fParams.get(i), MemoryType.Alloca);
                MemoryInstruction store = IRMaker.makeMemoryInstruction(curBasicBlock, new Void(),
                        MemoryType.Store, curFunction.getArguments().get(i), alloc);
                curValueTable.addEntry(fParamsNode.getChild(cnt).getChild(1).getToken().getValue(), alloc);
            }
        }

        visitBlock(node.getChild(-1));

        if (!curFunction.isFunctionInt()) {
            if (curBasicBlock.getInstructions().isEmpty()) {
                IRMaker.makeRetNoReturn(curBasicBlock);
            } else {
                Instruction lastInstruction = curBasicBlock.getInstructions().
                        get(curBasicBlock.getInstructions().size() - 1);
                if (!((lastInstruction instanceof TerminatorInstruction)
                        && ((TerminatorInstruction) lastInstruction).isRet())) {
                    IRMaker.makeRetNoReturn(curBasicBlock);
                }
            }
        }
    }

    // 主函数定义 MainFuncDef -> 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(ASTNode node) {
        ValueTable mainValueTable = new ValueTable(curValueTable, false);
        curValueTable.addChildTable(mainValueTable);
        curValueTable = mainValueTable;

        Function function = IRMaker.makeFunctionInt("main", false, module);
        curFunction = function;

        curBasicBlock = IRMaker.makeBasicBlock(function);
        visitBlock(node.getChild(-1));
        curValueTable = (ValueTable) curValueTable.getParent();
    }

    // 函数形参表 FuncFParams -> FuncFParam { ',' FuncFParam }
    private void visitFuncFParams(ASTNode node) {
        ArrayList<Type> fParamsType = new ArrayList<>();
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            visitFuncFParam(node.getChild(i));
            fParamsType.add(curType);
        }
        curTypeArray = fParamsType;
    }

    // 函数形参 FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    private void visitFuncFParam(ASTNode node) {
        if (node.getChildrenSize() == 2) {
            curType = new Int(32);
        } else {
            Type type = new Int(32);
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                ASTNode child = node.getChild(i);
                if (child.getGrammarSymbol() == GrammarSymbol.ConstExp) {
                    parsingConstant = true;
                    visitConstExp(child);
                    parsingConstant = false;
                    type = new Array(type, curInt);
                }
            }
            curType = new Pointer(type);
        }
    }

    // 语句块 Block -> '{' { BlockItem } '}'
    private void visitBlock(ASTNode node) {
        if (isEnterCurTable) {
            isEnterCurTable = false;
        } else {
            ValueTable valueTable = new ValueTable(curValueTable, false);
            curValueTable.addChildTable(valueTable);
            curValueTable = valueTable;
        }
        for (int i = 1; i < node.getChildrenSize() - 1; i++) {
            visitBlockItem(node.getChild(i));
        }
        curValueTable = (ValueTable) curValueTable.getParent();
    }

    // 语句块项 BlockItem -> Decl | Stmt
    private void visitBlockItem(ASTNode node) {
        if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.Decl) {
            visitDecl(node.getChild(0));
        } else {
            visitStmt(node.getChild(0));
        }
    }

    // 语句 Stmt -> LVal '=' Exp ';' | [Exp] ';' | Block
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    // | 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    // | 'break' ';' | 'continue' ';'
    // | 'return' [Exp] ';'
    // | LVal '=' 'getint''('')'';'
    // | 'printf''('FormatString{,Exp}')'';'
    private void visitStmt(ASTNode node) {
        ASTNode first = node.getChild(0);
        ASTNode last = node.getChild(-1);
        if (first.getToken() != null && first.getToken().getType() == Token.TokenType.RETURNTK) {
            if (node.getChild(1).getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(node.getChild(1));
                curValue = IRMaker.makeRetWithReturn(curBasicBlock, curValue);
            } else {
                curValue = IRMaker.makeRetNoReturn(curBasicBlock);
            }
        } else if (first.getGrammarSymbol() == GrammarSymbol.LVal) {
            visitLVal(first);
            Value left = curValue;
            if (node.getChild(2).getToken() != null
                    && node.getChild(2).getToken().getType() == Token.TokenType.GETINTTK) {
                Function function = module.getFunction("@getint");
                curValue = IRMaker.makeCallWithReturn(curBasicBlock, function, function);
                MemoryInstruction instruction = IRMaker.makeMemoryInstruction(curBasicBlock, new Void(),
                        MemoryType.Store, curValue, left);
            } else if (node.getChild(2).getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(node.getChild(2));
                Value right = curValue;
                IRMaker.makeMemoryInstruction(curBasicBlock, new Void(), MemoryType.Store, right, left);
            }
        } else if (first.getGrammarSymbol() == GrammarSymbol.Block) {
            visitBlock(first);
        } else if (first.getGrammarSymbol() == GrammarSymbol.Exp || node.getChildrenSize() == 1) {
            if (first.getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(first);
            }
        } else if (first.getToken().getType() == Token.TokenType.IFTK) {
            visitIfStmt(node);
        } else if (first.getToken().getType() == Token.TokenType.FORTK) {
            visitForBranchStmt(node);
        } else if (first.getToken().getType() == Token.TokenType.BREAKTK) {
            visitBreakStmt(node);
        } else if (first.getToken().getType() == Token.TokenType.CONTINUETK) {
            visitContinueStmt(node);
        } else if (first.getToken().getType() == Token.TokenType.PRINTFTK) {
            ASTNode formatStr = node.getChild(2);
            ArrayList<Value> expNodes = new ArrayList<>();

            for (int i = 4; i < node.getChildrenSize() - 2; i += 2) {
                visitExp(node.getChild(i));
                expNodes.add(curValue);
            }

            Function callch = module.getFunction("@putch");
            Function callInt = module.getFunction("@putint");
            String format = formatStr.getToken().getValue();
            int l = format.length();
            for (int i = 1; i < l - 1; i++) {
                char c = format.charAt(i);
                if (c == '%' && format.charAt(i + 1) == 'd') {
                    IRMaker.makeCallWithReturn(curBasicBlock, callInt, callInt, expNodes.remove(0));
                    i++;
                } else if (c == '\\' && format.charAt(i + 1) == 'n') {
                    i++;
                    IRMaker.makeCallNoReturn(curBasicBlock, callch, callch, new ConstInt(32, 10));
                } else {
                    IRMaker.makeCallNoReturn(curBasicBlock, callch, callch, new ConstInt(32, c));
                }
            }
        }
    }

    // 语句 ForStmt -> LVal '=' Exp
    // h -> ConstAssign
    private void visitForStmt(ASTNode node) {
        ASTNode first = node.getChild(0);
        visitLVal(first);
        Value left = curValue;
        visitExp(node.getChild(-1));
        Value right = curValue;
        IRMaker.makeMemoryInstruction(curBasicBlock, new Void(), MemoryType.Store, right, left);
    }

    // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    private void visitForBranchStmt(ASTNode node) {
        BasicBlock breakBasicBlock = curBreakBasicBlock;
        BasicBlock continueBasicBlock = curContinueBasicBlock;

        BasicBlock condBlock = IRMaker.makeBasicBlock(curFunction);
        BasicBlock bodyBlock = IRMaker.makeBasicBlock(curFunction);
        BasicBlock stepBlock = IRMaker.makeBasicBlock(curFunction);
        BasicBlock mergeBlock = IRMaker.makeBasicBlock(curFunction);
        if (node.getChild(2).getGrammarSymbol() == GrammarSymbol.ForStmt) {
            visitForStmt(node.getChild(2));
        }

        IRMaker.makeBranchNoCond(curBasicBlock, condBlock);
        curIfBasicBlock = bodyBlock;
        curElseBasicBlock = mergeBlock;
        curBasicBlock = condBlock;

        if (node.getChild(3).getGrammarSymbol() == GrammarSymbol.Cond) {
            visitCond(node.getChild(3));
        } else if (node.getChild(4).getGrammarSymbol() == GrammarSymbol.Cond) {
            visitCond(node.getChild(4));
        } else if (curBasicBlock.getInstructions().isEmpty()) {
            IRMaker.makeBranchNoCond(curBasicBlock, bodyBlock);
        }

        curBreakBasicBlock = mergeBlock;
        curContinueBasicBlock = stepBlock;
        curBasicBlock = bodyBlock;
        visitStmt(node.getChild(-1));

        if (curBasicBlock.getInstructions().isEmpty()) {
            IRMaker.makeBranchNoCond(curBasicBlock, stepBlock);
        } else {
            Instruction lastInstruction = curBasicBlock.getInstructions().
                    get(curBasicBlock.getInstructions().size() - 1);
            if (!((lastInstruction instanceof TerminatorInstruction)
                    && ((TerminatorInstruction) lastInstruction).isBrOrRet())) {
                IRMaker.makeBranchNoCond(curBasicBlock, stepBlock);
            }
        }

        curBasicBlock = stepBlock;
        if (node.getChildrenSize() >= 7 && node.getChild(6).getGrammarSymbol() == GrammarSymbol.ForStmt) {
            visitForStmt(node.getChild(6));
        } else if (node.getChildrenSize() >= 6 && node.getChild(5).getGrammarSymbol() == GrammarSymbol.ForStmt) {
            visitForStmt(node.getChild(5));
        } else if (node.getChild(4).getGrammarSymbol() == GrammarSymbol.ForStmt) {
            visitForStmt(node.getChild(4));
        }

        if (curBasicBlock.getInstructions().isEmpty()) {
            IRMaker.makeBranchNoCond(curBasicBlock, condBlock);
        } else {
            Instruction lastInstruction = curBasicBlock.getInstructions().
                    get(curBasicBlock.getInstructions().size() - 1);
            if (!((lastInstruction instanceof TerminatorInstruction)
                    && ((TerminatorInstruction) lastInstruction).isBrOrRet())) {
                IRMaker.makeBranchNoCond(curBasicBlock, condBlock);
            }
        }
        curBreakBasicBlock = breakBasicBlock;
        curContinueBasicBlock = continueBasicBlock;

        curBasicBlock = mergeBlock;
    }

    private void visitIfStmt(ASTNode node) {
        ASTNode cond = node.getChild(2);
        ASTNode stmt = node.getChild(4);
        ASTNode elseStmt = node.getChild(-2).getToken().getType() == Token.TokenType.ELSETK
                ? node.getChild(-1) : null;
        BasicBlock thenBlock = IRMaker.makeBasicBlock(curFunction);
        BasicBlock elseBlock = IRMaker.makeBasicBlock(curFunction);
        BasicBlock mergeBlock = elseStmt != null ? IRMaker.makeBasicBlock(curFunction) : elseBlock;

        curIfBasicBlock = thenBlock;
        curElseBasicBlock = elseBlock;
        visitCond(cond);
        curBasicBlock = thenBlock;

        boolean ifHasReturn = false;
        boolean elseHasReturn = false;
        visitStmt(stmt);

        if (curBasicBlock.getInstructions().isEmpty()) {
            IRMaker.makeBranchNoCond(curBasicBlock, mergeBlock);
        } else {
            Instruction lastInstruction = curBasicBlock.getInstructions().
                    get(curBasicBlock.getInstructions().size() - 1);
            if (!((lastInstruction instanceof TerminatorInstruction)
                    && ((TerminatorInstruction) lastInstruction).isBrOrRet())) {
                IRMaker.makeBranchNoCond(curBasicBlock, mergeBlock);
            } else {
                ifHasReturn = true;
            }
        }

        if (elseStmt != null) {
            curBasicBlock = elseBlock;
            visitStmt(elseStmt);
            if (curBasicBlock.getInstructions().isEmpty()) {
                IRMaker.makeBranchNoCond(curBasicBlock, mergeBlock);
            } else {
                Instruction lastInstruction = curBasicBlock.getInstructions().
                        get(curBasicBlock.getInstructions().size() - 1);
                if (!((lastInstruction instanceof TerminatorInstruction)
                        && ((TerminatorInstruction) lastInstruction).isBrOrRet())) {
                    IRMaker.makeBranchNoCond(curBasicBlock, mergeBlock);
                } else {
                    elseHasReturn = true;
                }
            }
        }

        if (ifHasReturn && elseHasReturn) {
            curFunction.getBasicBlocks().remove(mergeBlock);
        } else {
            curBasicBlock = mergeBlock;
        }
    }

    private void visitBreakStmt(ASTNode node) {
        IRMaker.makeBranchNoCond(curBasicBlock, curBreakBasicBlock);
    }

    private void visitContinueStmt(ASTNode node) {
        IRMaker.makeBranchNoCond(curBasicBlock, curContinueBasicBlock);
    }

    // 表达式 Exp -> AddExp
    private void visitExp(ASTNode node) {
        visitAddExp(node.getChild(0));
    }

    // 条件表达式 Cond -> LOrExp
    private void visitCond(ASTNode node) {
        visitLOrExp(node.getChild(0), true);
    }

    // 左值表达式 LVal -> Ident {'[' Exp ']'}
    private void visitLVal(ASTNode node) {
        ASTNode ident = node.getChild(0);
        Value value = curValueTable.getEntry(ident.getToken().getValue());
        if (value.getType() instanceof Int) {
            curValue = value;
        } else if (value.getType() instanceof Array arrayType) {
            for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                ASTNode child = node.getChild(i);
                if (child.getGrammarSymbol() == GrammarSymbol.Exp) {
                    visitExp(child);
                    Value index = curValue;
                    curValue = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                            new ConstInt(32, 0), index);
                    if (arrayType.getElementsType() instanceof Array) {
                        arrayType = (Array) arrayType.getElementsType();
                        if (i == node.getChildrenSize() - 2) {
                            value = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                                    new ConstInt(32, 0), new ConstInt(32, 0));
                        }
                    }
                }
            }
            curValue = value;
        } else if (value.getType() instanceof Pointer pointer) {
            Type type = pointer.getObjectType();
            if (type instanceof Int) {
                curValue = value;
            } else if (type instanceof Array arrayType) {
                if (isGlobalInit || parsingConstant) {
                    value = ((GlobalVar)value).getValue();
                    for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                        ASTNode child = node.getChild(i);
                        if (child.getGrammarSymbol() == GrammarSymbol.Exp) {
                            visitExp(child);
                            value = ((ConstArray) value).getUsedValue(curInt);
                        }
                    }
                } else {
                    if (node.getChildrenSize() == 1) {
                        value = IRMaker.makeGetElementptr((Array) type, curBasicBlock, value,
                                new ConstInt(32, 0), new ConstInt(32, 0));
                    } else {
                        for (int i = 0; i < node.getChildrenSize() - 1; i++) {
                            ASTNode child = node.getChild(i);
                            if (child.getGrammarSymbol() == GrammarSymbol.Exp) {
                                visitExp(child);
                                Value index = curValue;
                                value = IRMaker.makeGetElementptr(arrayType, curBasicBlock,
                                        value, new ConstInt(32, 0), index);

                                if (arrayType.getElementsType() instanceof Array) {
                                    arrayType = (Array) arrayType.getElementsType();
                                    if (i == node.getChildrenSize() - 2) {
                                        value = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                                                new ConstInt(32, 0), new ConstInt(32, 0));
                                    }
                                }
                            }
                        }
                    }
                }
                curValue = value;
            } else {
                Pointer pointerType = (Pointer) type;
                MemoryInstruction load = IRMaker.makeMemoryInstruction(curBasicBlock, pointerType,
                        MemoryType.Load, value);
                value = load;
                Array arrayType = null;

                if (node.getChildrenSize() >= 3 && node.getChild(2).getGrammarSymbol() == GrammarSymbol.Exp) {
                    visitExp(node.getChild(2));
                    Value index = curValue;
                    value = IRMaker.makePointerGetElementptr(pointerType, curBasicBlock, load, index);

                    if (pointerType.getObjectType() instanceof Array) {
                        arrayType = (Array) pointerType.getObjectType();

                        if (node.getChildrenSize() == 4) {
                            value = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                                    new ConstInt(32, 0), new ConstInt(32, 0));
                        }
                    }

                    for (int i = 4; i < node.getChildrenSize() - 1; i++) {
                        ASTNode child = node.getChild(i);

                        if (child.getGrammarSymbol() == GrammarSymbol.Exp) {
                            visitExp(child);
                            index = curValue;
                            value = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                                    new ConstInt(32, 0), index);
                            if (arrayType != null && arrayType.getElementsType() instanceof Array) {
                                arrayType = (Array) arrayType.getElementsType();
                                if (i == node.getChildrenSize() - 2) {
                                    value = IRMaker.makeGetElementptr(arrayType, curBasicBlock, value,
                                            new ConstInt(32, 0), new ConstInt(32, 0));
                                }
                            }
                        }
                    }
                }
                curValue = value;
            }
        }
    }

    // 基本表达式 PrimaryExp -> '(' Exp ')' | LVal | Number
    private void visitPrimaryExp(ASTNode node) {
        if (node.getChildrenSize() > 1) {
            // '(' Exp ')'
            visitExp(node.getChild(1));
        } else if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.Number) {
            visitNumber(node.getChild(0));
        } else if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.LVal) {
            if (parsingConstant) {
                visitLVal(node.getChild(0));
                if (curValue instanceof GlobalVar) {
                    curInt = ((ConstInt)(((GlobalVar)curValue).getValue())).getValue();
                } else {
                    curInt = ((ConstInt) curValue).getValue();
                }

            } else {
                boolean nowIsCallingFunc = isCallingFunc;
                if (isCallingFunc) {
                    isCallingFunc = false;
                }
                visitLVal(node.getChild(0));
                if (!nowIsCallingFunc && !(curValue.getType() instanceof Int)
                        && (((Pointer) curValue.getType()).getObjectType() instanceof Int)) {
                    curValue = IRMaker.makeMemoryInstruction(curBasicBlock,new Int(32),
                            MemoryType.Load, curValue);
                }
            }
        }
    }

    // 数值 Number -> IntConst
    private void visitNumber(ASTNode node) {
        curInt = Integer.parseInt(node.getChild(0).getToken().getValue());
        curValue = new ConstInt(32, curInt);
    }


    // 一元表达式 UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private void visitUnaryExp(ASTNode node) {
        ASTNode first = node.getChild(0);
        if (first.getGrammarSymbol() == GrammarSymbol.PrimaryExp) {
            visitPrimaryExp(first);
        } else if (first.getGrammarSymbol() == GrammarSymbol.UnaryOp) {
            int op = (first.getChild(0).getToken().getType() == Token.TokenType.PLUS) ? 1 :
                    (first.getChild(0).getToken().getType() == Token.TokenType.MINU) ? -1 : 0;
            visitUnaryExp(node.getChild(1));
            if (parsingConstant) {
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
                if (op == -1) {
                    curValue = IRMaker.makeSub(curBasicBlock, new ConstInt(32, 0), curValue);
                } else if (op == 0) {
                    curValue = IRMaker.makeIcmp(curBasicBlock, "eq", new ConstInt(32, 0), curValue);
                }
            }
        } else if (first.getToken().getType() == Token.TokenType.IDENFR) {
            Function callee = module.getFunction("@" + first.getToken().getValue());
            ArrayList<Value> args = new ArrayList<>();
            ArrayList<Type> argTypes = callee.getParamsType();
            if (node.getChild(2).getGrammarSymbol() == GrammarSymbol.FuncRParams) {
                visitFuncRParams(node.getChild(2), args, argTypes);
            }

            if (callee.isFunctionInt()) { // void function
                args.add(0, callee);
                curValue = IRMaker.makeCallWithReturn(curBasicBlock, callee, args.toArray(new Value[0]));
            } else { // int function
                args.add(0, callee);
                curValue = IRMaker.makeCallNoReturn(curBasicBlock, callee, args.toArray(new Value[0]));
            }
        }
    }

    // 函数实参表 FuncRParams -> Exp { ',' Exp }
    public void visitFuncRParams(ASTNode node, ArrayList<Value> args, ArrayList<Type> argTypes) {
        int cnt = 0;
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            if (!(argTypes.get(cnt) instanceof Int)) {
                isCallingFunc = true;
            }
            visitExp(node.getChild(i));
            isCallingFunc = false;
            args.add(curValue);
            cnt++;
        }
    }

    // 乘除模表达式 MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private void visitMulExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitUnaryExp(node.getChild(0));
        } else {
            visitMulExp(node.getChild(0));
            int left = curInt;
            Value leftValue = curValue;
            visitUnaryExp(node.getChild(2));
            if (parsingConstant) {
                if (node.getChild(1).getToken().getType() == Token.TokenType.MULT) {
                    curInt = left * curInt;
                } else if (node.getChild(1).getToken().getType() == Token.TokenType.DIV) {
                    curInt = left / curInt;
                } else {
                    curInt = left % curInt;
                }
            } else {
                if (node.getChild(1).getToken().getType() == Token.TokenType.MULT) {
                    curValue = IRMaker.makeMul(curBasicBlock, leftValue, curValue);
                } else if (node.getChild(1).getToken().getType() == Token.TokenType.DIV) {
                    curValue = IRMaker.makeDiv(curBasicBlock, leftValue, curValue);
                } else {
                    curValue = IRMaker.makeMod(curBasicBlock, leftValue, curValue);
                }
            }
        }
    }

    // 加减表达式 AddExp -> MulExp | AddExp ('+' | '−') MulExp
    private void visitAddExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitMulExp(node.getChild(0));
        } else {
            visitAddExp(node.getChild(0));
            int left = curInt;
            Value leftValue = curValue;
            visitMulExp(node.getChild(2));
            if (parsingConstant) {
                if (node.getChild(1).getToken().getType() == Token.TokenType.PLUS) {
                    curInt = left + curInt;
                } else {
                    curInt = left - curInt;
                }
            } else {
                if (node.getChild(1).getToken().getType() == Token.TokenType.PLUS) {
                    curValue = IRMaker.makeAdd(curBasicBlock, leftValue, curValue);
                } else {
                    curValue = IRMaker.makeSub(curBasicBlock, leftValue, curValue);
                }
            }
        }
    }

    // 关系表达式 RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private void visitRelExp(ASTNode node, boolean first) {
        if (node.getChildrenSize() == 1) { // AddExp
            if (!first) {
                isSingle = false;
            }
            visitAddExp(node.getChild(0));
        } else {
            visitRelExp(node.getChild(0), false);
            Value leftOp = curValue;
            visitAddExp(node.getChild(2));

            Token.TokenType op = node.getChild(1).getToken().getType();
            if (op == Token.TokenType.LSS) { // <
                curValue = IRMaker.makeIcmp(curBasicBlock, "slt", leftOp, curValue);
            } else if (op == Token.TokenType.GRE) { // >
                curValue = IRMaker.makeIcmp(curBasicBlock, "sgt", leftOp, curValue);
            } else if (op == Token.TokenType.LEQ) { // <=
                curValue = IRMaker.makeIcmp(curBasicBlock, "sle", leftOp, curValue);
            } else { // >=
                curValue = IRMaker.makeIcmp(curBasicBlock, "sge", leftOp, curValue);
            }
        }
    }

    // 相等性表达式 EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    private void visitEqExp(ASTNode node, boolean first) {
        if (node.getChildrenSize() == 1) {
            if (!first) {
                isSingle = false;
            }
            visitRelExp(node.getChild(0), true);
        } else {
            visitEqExp(node.getChild(0), false);
            Value leftOp = curValue;
            visitRelExp(node.getChild(2), true);

            Token.TokenType op = node.getChild(1).getToken().getType();
            if (op == Token.TokenType.EQL) {
                curValue = IRMaker.makeIcmp(curBasicBlock, "eq", leftOp, curValue);
            } else {
                curValue = IRMaker.makeIcmp(curBasicBlock, "ne", leftOp, curValue);
            }
        }
    }

    // 逻辑与表达式 LAndExp -> EqExp | LAndExp '&&' EqExp
    private void visitLAndExp(ASTNode node, boolean first) {
        BasicBlock ifBlock = curIfBasicBlock;
        BasicBlock elseBlock = curElseBasicBlock;

        if (node.getChildrenSize() == 1) { // LAndExp -> EqExp
            if (first) { // first = true 意味着正在处理第一个EqExp 结果为真时跳转到ifBlock
                isSingle = true;
                visitEqExp(node.getChild(0), true);
                if (isSingle) {
                    curValue = IRMaker.makeIcmp(curBasicBlock, "ne", curValue, new ConstInt(32, 0));
                    isSingle = false;
                }
                IRMaker.makeBranchWithCond(curBasicBlock, curValue, ifBlock, elseBlock);
                curBasicBlock = ifBlock;
            } else {
                BasicBlock nextBlock = IRMaker.makeBasicBlock(curFunction);
                isSingle = true;
                visitEqExp(node.getChild(0), true);
                if (isSingle) {
                    curValue = IRMaker.makeIcmp(curBasicBlock, "ne", curValue, new ConstInt(32, 0));
                    isSingle = false;
                }
                IRMaker.makeBranchWithCond(curBasicBlock, curValue, nextBlock, elseBlock);
                curBasicBlock = nextBlock;
            }
        } else {
            visitLAndExp(node.getChild(0), false);
            BasicBlock nextBlock = IRMaker.makeBasicBlock(curFunction);
            isSingle = true;
            visitEqExp(node.getChild(2), true);
            if (isSingle) {
                curValue = IRMaker.makeIcmp(curBasicBlock, "ne", curValue, new ConstInt(32, 0));
                isSingle = false;
            }
            IRMaker.makeBranchWithCond(curBasicBlock, curValue, nextBlock, elseBlock);
            curBasicBlock = nextBlock;
            if (first) {
                IRMaker.makeBranchNoCond(curBasicBlock, ifBlock);
            }
        }
    }

    // 逻辑或表达式 LOrExp -> LAndExp | LOrExp '||' LAndExp
    private void visitLOrExp(ASTNode node, boolean first) {
        BasicBlock ifBlock = curIfBasicBlock;
        BasicBlock elseBlock = curElseBasicBlock;

        if(node.getChildrenSize() == 1) { // 只有一个LAndExp
            if (!first) { // first = false 意味着正在处理第一个LAndExp
                BasicBlock nextBlock = IRMaker.makeBasicBlock(curFunction);
                curElseBasicBlock = nextBlock;
                visitLAndExp(node.getChild(0), true);
                curBasicBlock = nextBlock;
            } else {
                visitLAndExp(node.getChild(0), true);
            }
        } else {
            visitLOrExp(node.getChild(0), false);
            if (!first) {
                BasicBlock nextBlock = IRMaker.makeBasicBlock(curFunction);
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

    // 常量表达式 ConstExp -> AddExp
    private void visitConstExp(ASTNode node) {
        visitAddExp(node.getChild(0));
        if (parsingConstant) {
            curValue = new ConstInt(32, curInt);
        }
    }
}
