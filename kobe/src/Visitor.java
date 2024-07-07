import EntryType.*;
import Lexer.Token;
import Parser.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Visitor {
    private final ASTNode root;
    private SymbolTable curSymbolTable= new SymbolTable(null, true);
    private final ArrayList<String> errors = new ArrayList<>();
    private final ArrayList<Integer> curDimensions = new ArrayList<>();
    private ConstValue curConstValue;
    private boolean isMultiArrayInit = false;
    private boolean isConstant = false;
    private int curInt;
    private boolean receiveReturn = false;
    private boolean createSTableBeforeBlock = false;
    private FuncType curFuncType;
    private int funcEndLineNum;
    private final boolean debug;
    private TableEntry curTableEntry = null;
    private int forLevel = 0;

    public Visitor(ASTNode root, boolean debug) {
        this.root = root;
        this.curFuncType = FuncType.NotFunc;
        this.funcEndLineNum = 0;
        this.debug = debug;
    }

    // 编译单元 CompUnit -> {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit() {
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
    public void visitDecl(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammarSymbol() == GrammarSymbol.ConstDecl) {
                visitConstDecl(child);
            } else {
                visitVarDecl(child);
            }
        }
    }

    // 常量声明 ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    // i -> MissingSEMICN
    public void visitConstDecl(ASTNode node) {
        for (int i = 2; i < node.getChildrenSize() - 1; i += 2) {
            visitConstDef(node.getChild(i));
        }
        ASTNode lastChild = node.getChildren().get(node.getChildrenSize() - 1);
        if (lastChild instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }
    }

    // 常数定义 ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    // b -> IdentRedefined
    // k -> RBRACKMissing
    public void visitConstDef(ASTNode node) {
        ASTNode ident = node.getChild(0);
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
            return;
        }
        int length = node.getChildrenSize();
        curDimensions.clear();

        for (int i = 1; i < length - 2 && node.getChild(i).getToken().
                getType() == Token.Type.LBRACK; i += 3) {
            isConstant = true;
            visitConstExp(node.getChild(i + 1));
            isConstant = false;
            curDimensions.add(curInt);
            if (node.getChild(i + 2) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        }

        visitConstInitVal(node.getChild(-1), curDimensions.size());
        switch (curConstValue.getType()) {
            case "int":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, curConstValue.getVar(), false));
                break;
            case "Array1":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, curConstValue.getArray1(), false));
                break;
            case "Array2":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, curConstValue.getArray2(), false));
                break;
            default:
                break;
        }



    }

    // 常量初值 ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public void visitConstInitVal(ASTNode node, int dimension) {
        switch (dimension) {
            case 0:
                isConstant = true;
                visitConstExp(node.getChild(0));
                isConstant = false;

                ConstVar constVar = new ConstVar(curInt);
                curConstValue = new ConstValue("int", constVar);
                break;
            case 1:
                if (isMultiArrayInit) {
                    int d1 = (node.getChildrenSize() + 1) / 2;
                    for (int i = 1; i < d1; i += 2) {
                        isConstant = true;
                        visitConstExp(node.getChild(i).getChild(0));
                        isConstant = false;
                        curConstValue.addValues(curInt);
                    }
                } else {
                    int d1 = curDimensions.get(0);
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 1, p = 0; p < d1; i += 2, p++) {
                        isConstant = true;
                        visitConstExp(node.getChild(i).getChild(0));
                        isConstant = false;
                        values.add(curInt);
                    }
                    curConstValue = new ConstValue("Array1", new ConstArray1(d1, values));
                }
                break;
            case 2:
                isMultiArrayInit = true;
                int d1 = curDimensions.get(0);
                int d2 = curDimensions.get(1);
                int p = 0;
                curConstValue = new ConstValue("Array2", new ConstArray2(d1, d2));
                for (int i = 1; p < d1; i += 2, p++) {
                    visitConstInitVal(node.getChild(i), 1);
                }
                isMultiArrayInit = false;
                break;
            default:
                break;
        }
    }

    // 变量声明 VarDecl -> BType VarDef { ',' VarDef } ';' // i
    // i -> SEMICNMissing
    public void visitVarDecl(ASTNode node) {
        for (int i = 1; i < node.getChildrenSize() - 1; i += 2) {
            visitVarDef(node.getChildren().get(i));
        }
        ASTNode lastChild = node.getChildren().get(node.getChildrenSize() - 1);
        if (lastChild instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }
    }

    // 变量定义 VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    // b -> IdentRedefined
    // k -> RBRACKMissing
    public void visitVarDef(ASTNode node) {
        ASTNode ident = node.getChild(0);
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
            return;
        }
        int length = node.getChildrenSize();
        curDimensions.clear();
        for (int i = 1; i < length - 2 && node.getChild(i).getToken()
                .getType() == Token.Type.LBRACK; i += 3) {
            isConstant = true;
            visitConstExp(node.getChild(i + 1));
            isConstant = false;
            curDimensions.add(curInt);
            if (node.getChild(i + 2) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        }

        if (node.getChild(-1).getGrammarSymbol() == GrammarSymbol.InitVal) {
            visitInitVal(node.getChild(-1));
        }

        switch (curDimensions.size()) {
            case 0:
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, new Var(), false));
                break;
            case 1:
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, new Array1(curDimensions.get(0)), false));
                break;
            case 2:
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, new Array2(curDimensions.get(0), curDimensions.get(1)),
                                false));
                break;
            default:
                break;
        }

    }

    // 变量初值 InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public void visitInitVal(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitExp(node.getChild(0));
            return;
        }
        for (int i = 1; i < node.getChildrenSize() - 1; i += 2) {
            visitInitVal(node.getChild(i));
        }
    }

    // 函数定义 FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    // b -> IdentRedefined
    // g -> RETURNMissing
    // j -> RPARENTMissing
    public void visitFuncDef(ASTNode node) {
        ASTNode funcType = node.getChild(0).getChild(0);
        ASTNode ident = node.getChild(1);
        TableEntry funcEntry = new TableEntry();
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
            return;
        }
        SymbolTable funcSymbolTable = new SymbolTable(curSymbolTable, false);
        curSymbolTable.addChildTable(funcSymbolTable);

        if (funcType.getToken().getType().equals(Token.Type.VOIDTK)) {
            FunctionVoid functionVoid = new FunctionVoid();
            funcEntry = new TableEntry(ident, functionVoid);
            curFuncType = FuncType.VoidFunc;
        } else if (funcType.getToken().getType().equals(Token.Type.INTTK)) {
            FunctionInt functionInt = new FunctionInt();
            funcEntry = new TableEntry(ident, functionInt);
            curFuncType = FuncType.IntFunc;
        }
        curSymbolTable.addEntry(ident.getToken().getValue(), funcEntry);
        curSymbolTable = funcSymbolTable;

        if (node.getChild(3).getGrammarSymbol() == GrammarSymbol.FuncFParams) {
            visitFuncFParams(node.getChild(3), funcEntry);
        }

        if (node.getChild(-2) instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }

        receiveReturn = false;
        createSTableBeforeBlock = true;

        visitBlock(node.getChild(-1), true);

        if (curFuncType == FuncType.IntFunc && !receiveReturn) {
            errors.add(new ErrorNode(ErrorType.ReturnMissing, funcEndLineNum, null, 0)
                    .toString());
        }

        curSymbolTable = curSymbolTable.getParent();

        receiveReturn = false;
        curFuncType = FuncType.NotFunc;
        createSTableBeforeBlock = false;

    }

    // 主函数定义 MainFuncDef -> 'int' 'main' '(' ')' Block
    // g -> ReturnMissing
    // j -> RPARENTMissing
    public void visitMainFuncDef(ASTNode node) {
        SymbolTable mainSymbolTable = new SymbolTable(curSymbolTable, false);
        curSymbolTable.addChildTable(mainSymbolTable);
        curSymbolTable = mainSymbolTable;

        receiveReturn = false;
        createSTableBeforeBlock = true;
        curFuncType = FuncType.MainFunc;

        visitBlock(node.getChild(-1), true);

        if (!receiveReturn) {
            errors.add(new ErrorNode(ErrorType.ReturnMissing, funcEndLineNum,
                    null, 0).toString());
        }

        receiveReturn = false;
        curFuncType = FuncType.NotFunc;
        createSTableBeforeBlock = false;
        curSymbolTable = curSymbolTable.getParent();
    }

    // 函数形参表 FuncFParams -> FuncFParam { ',' FuncFParam }
    public void visitFuncFParams(ASTNode node, TableEntry funcEntry) {
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            TableEntry funcFParam = visitFuncFParam(node.getChild(i));
            curSymbolTable.addEntry(funcFParam.getName(), funcFParam);
            funcEntry.addParamForFuncEntry(funcFParam);
        }
    }

    // 函数形参 FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    // b -> IdentRedefined
    // k -> RBRACKMissing
    public TableEntry visitFuncFParam(ASTNode node) {
        ASTNode ident = node.getChild(1);
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
        }

        switch (node.getChildrenSize()) {
            case 2:
                return new TableEntry(ident, new Var(), true);
            case 4:
                if (node.getChild(3) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                return new TableEntry(ident, new Array1(), true);
            case 7:
                visitConstExp(node.getChild(5));
                if (node.getChild(3) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                if (node.getChild(6) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                return new TableEntry(ident, new Array2(curInt), true);
            default:
                throw new RuntimeException();
        }
    }

    // 语句块 Block -> '{' { BlockItem } '}'
    public void visitBlock(ASTNode node, boolean inFuncBlock) {
        createSTableBeforeBlock = false;
        for (int i = 1; i < node.getChildrenSize() - 1; i++) {
            visitBlockItem(node.getChild(i), inFuncBlock);
        }
        funcEndLineNum = node.getChild(-1).getToken().getLine();
    }

    // 语句块项 BlockItem -> Decl | Stmt
    public void visitBlockItem(ASTNode node, boolean inFuncBlock) {
        if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.Decl) {
            visitDecl(node.getChild(0));
        } else {
            visitStmt(node.getChild(0), inFuncBlock);
        }
    }

    // 语句 Stmt -> LVal '=' Exp ';' | [Exp] ';' | Block // h i
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
    // | 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    // | 'break' ';' | 'continue' ';' // i m
    // | 'return' [Exp] ';' // f i
    // | LVal '=' 'getint''('')'';' // h i j
    // | 'printf''('FormatString{,Exp}')'';' // i j l
    public void visitStmt(ASTNode node, boolean inFuncBlock) {
        ASTNode first = node.getChild(0);
        ASTNode last = node.getChild(-1);

        if (first.getGrammarSymbol() == GrammarSymbol.LVal) {
            // Stmt -> LVal '=' Exp ';'
            // Stmt -> LVal '=' 'getint''('')'';'
            visitLval(first);
            if (curTableEntry == null)
                return;

            if (curTableEntry.isConst()) {
                errors.add(new ErrorNode(ErrorType.ConstAssign, first.getChild(0).getToken().getLine(),
                        null, 0).toString());
            }

            if (node.getChild(2).getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(node.getChild(-2));
            } else if (node.getChild(4) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
            
            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (first.getGrammarSymbol() == GrammarSymbol.Block) {
            curSymbolTable = new SymbolTable(curSymbolTable, false);
            createSTableBeforeBlock = true;
            visitBlock(first, inFuncBlock);
            curSymbolTable = curSymbolTable.getParent();
        } else if (first.getGrammarSymbol() == GrammarSymbol.Exp
                || node.getChildrenSize() == 1) {
            // Stmt -> [Exp] ';'
            if (first.getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(first);
            }

            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (first.getToken().getType() == Token.Type.IFTK) {
            // Stmt -> 'if' '(' Cond ')' Stmt ['else' Stmt]
            visitCond(node.getChild(2));

            if (node.getChild(3) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }

            visitStmt(node.getChild(4), inFuncBlock);
            if (node.getChildrenSize() > 5) {
                visitStmt(node.getChild(6), inFuncBlock);
            }
        } else if (first.getToken().getType() == Token.Type.FORTK) {
            forLevel++;
            for (int i = 2; i < node.getChildrenSize(); i++) {
                if (Objects.equals(node.getChild(i).getGrammarSymbol(), GrammarSymbol.ForStmt)) {
                    visitForStmt(node.getChild(i));
                } else if (Objects.equals(node.getChild(i).getGrammarSymbol(), GrammarSymbol.Cond)) {
                    visitCond(node.getChild(i));
                } else if (node.getChild(i) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                } else if (Objects.equals(node.getChild(i).getGrammarSymbol(), GrammarSymbol.Stmt)) {
                    visitStmt(node.getChild(i), inFuncBlock);
                }
            }
            forLevel--;
        } else if (first.getToken().getType() == Token.Type.BREAKTK ||
                first.getToken().getType() == Token.Type.CONTINUETK) {
            if (forLevel == 0) {
                errors.add(new ErrorNode(ErrorType.BreakContinueNotInLoop,
                        first.getToken().getLine(), null, 0)
                        .toString());
            }
            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (first.getToken().getType()== Token.Type.RETURNTK) {
            receiveReturn = inFuncBlock;
            if (curFuncType == FuncType.VoidFunc &&
                    node.getChild(1).getGrammarSymbol() == GrammarSymbol.Exp) {
                errors.add(new ErrorNode(ErrorType.ReturnTypeError, first.getToken().getLine(),
                        null, 0).toString());
            }

            if (node.getChild(1).getGrammarSymbol() == GrammarSymbol.Exp) {
                visitExp(node.getChild(1));
            }

            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (first.getToken().getType().equals(Token.Type.PRINTFTK)) {
            ASTNode formatStr = node.getChild(2);
            int count = checkFormatString(formatStr);
            if (count < 0) {
                errors.add(new ErrorNode(ErrorType.IllegalChar, formatStr.getToken().getLine(),
                        null, 0).toString());
            }
            int rightNum = 0;
            for (int i = 4; i < node.getChildrenSize() - 2; i += 2) {
                rightNum++;
                visitExp(node.getChild(i));
            }
            if (count >= 0 && count != rightNum) {
                errors.add(new ErrorNode(ErrorType.PrintfFormatStrNumNotMatch,
                        first.getToken().getLine(), null, 0)
                        .toString());
            }

            if (node.getChild(-2) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }

            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else {
            throw new RuntimeException();
        }
    }

    private int checkFormatString(ASTNode node) {
        String format = node.getToken().getValue();
        int l = format.length();
        int count = 0;

        for (int i = 1; i < l - 1; i++) {
            char c = format.charAt(i);
            if (c != ' ' && c != '!' && !(c >= '(' && c <= '~')) {
                if (c == '%') {
                    if (i < l - 1 && format.charAt(i + 1) == 'd') {
                        count++;
                        i++;
                        continue;
                    }
                    return -1;
                }
                return -1;
            }

            if (c == '\\' && (i >= l - 1 || format.charAt(i + 1) != 'n')) {
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
            errors.add(new ErrorNode(ErrorType.ConstAssign, first.getChild(0).getToken().getLine(),
                    null, 0).toString());
        }
        visitExp(node.getChild(-1));
    }

    // Cond -> LOrExp
    public void visitCond(ASTNode node) {
        visitLOrExp(node.getChild(0));
    }

    // PrimaryExp -> '(' Exp ')' | LVal | Number
    public void visitPrimaryExp(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter PrimaryExp");
            if (node.getGrammarSymbol() != null) {
                System.out.println("PrimaryExp Node is " + node.getGrammarSymbol());
            } else {
                System.out.println("PrimaryExp Node is " + node.getToken().getType());
            }
        }
        if (node.getChildrenSize() > 1) {
            // '(' Exp ')'
            visitExp(node.getChild(1));
            if (node.getChild(-1) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        }
        else if (Objects.equals(node.getChild(0).getGrammarSymbol(), GrammarSymbol.LVal)) {
            visitLval(node.getChild(0));
        } else if (Objects.equals(node.getChild(0).getGrammarSymbol(), GrammarSymbol.Number)) {
            visitNumber(node.getChild(0));
        }
        else {
            throw new RuntimeException("PrimaryExp no match condition!!!!");
        }
    }

    // FuncRParams -> Exp { ',' Exp }
    public void visitFuncRParams(ASTNode node, TableEntry tableEntry) { // 分函数名记录实参
        tableEntry.clearFuncRParams();
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            visitExp(node.getChild(i)); // 改变了curTableEntry
            if (curTableEntry != null) {
                if (debug) {
                    System.out.println("visitFuncRParams curTableEntry Type is "
                            + curTableEntry.getTableEntryType());
                    System.out.println("visitFuncRParams curInt EntryType.Value is "
                            + curInt);
                }
                tableEntry.addFuncRParam(curTableEntry);
            } else { // 函数实参为常数
                tableEntry.addFuncRParam(new TableEntry(curInt));
            }
        }
    }

    // LVal -> Ident {'[' Exp ']'} // c k
    // c -> IdentUndefined
    // k -> RBRACKMissing
    // Lval 一定是之前已经定义过的变量
    public void visitLval(ASTNode node) { // 该函数的返回值传回curTableEntry
        ASTNode ident = node.getChild(0);
        if (!curSymbolTable.nameExisted(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentUndefined, ident.getToken()
                    .getLine(), null, 0).toString());
            curInt = 0; // ???
            curTableEntry = null;
        } else {
            int length = node.getChildren().size(); // 1 or 4 or 7

            TableEntry tableEntry = curSymbolTable.getEntry(ident.getToken().getValue());
            // definedEntry
            switch (length) {
                case 1: // ident
                    if (isConstant) { // 常量赋值时
                        assert tableEntry.isConst();
                        curInt = tableEntry.getVarValue();
                    }
                    curTableEntry = tableEntry;
                    break;

                case 4: // ident[exp]
                case 7: // ident[exp][exp]
                    ASTNode exp1 = node.getChild(2);
                    visitExp(exp1);

                    int v1;

                    if (node.getChild(3) instanceof ErrorNode errorNode) { // k mistake
                        errors.add(errorNode.toString());
                    }

                    TableEntry referencedTableEntry;
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
                            errors.add(errorNode.toString());
                        }

                        if (isConstant) {
                            curInt = referencedTableEntry.getValueFromReferencedArray2(v1, v2);
                            curTableEntry = null;
                        } else {
                            curTableEntry = referencedTableEntry;
                        }
                    }
            }


        }
    }

    // Number -> IntConst
    public void visitNumber(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter Number");
        }
        curInt = Integer.parseInt(node.getChild(0).getToken().getValue());
        curTableEntry = null;
    }

    // Exp -> AddExp
    public void visitExp(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter Exp");
        }
        visitAddExp(node.getChild(0));
    }

    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' // c d e j
    //        | UnaryOp UnaryExp
    // c =-> Parser.IdentUndefined
    // d =-> Parser.ParaNumNotMatch
    // e =-> Parser.ParaTypeNotMatch
    // j =-> Parser.RPARENTMissing
    public void visitUnaryExp(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter UnaryExp");
            if (node.getGrammarSymbol() != null) {
                System.out.println("UnaryExp Node is " + node.getGrammarSymbol());
            } else {
                System.out.println("UnaryExp Node is " + node.getToken().getType());
            }
        }
        ASTNode first = node.getChild(0);
        if (debug) {
            // System.out.println("Visitor Enter UnaryExp");
            if (first.getGrammarSymbol() != null) {
                System.out.println("UnaryExp First Child Node is " + first.getGrammarSymbol());
            } else {
                System.out.println("UnaryExp First Child Node is " + first.getToken().getType());
            }
        }
        if (Objects.equals(first.getGrammarSymbol(), GrammarSymbol.PrimaryExp)) {
            visitPrimaryExp(first);
        } else if (Objects.equals(first.getGrammarSymbol(), GrammarSymbol.UnaryOp)) {
            // UnaryOp -> '+' | '−' | '!'
            int op = first.getChild(0).getToken().getType().equals(Token.Type.PLUS) ? 1 :
                    first.getChild(0).getToken().getType().equals(Token.Type.MINU) ? -1 : 2;
            visitUnaryExp(node.getChild(1));
            if (op == 1 || op == -1) {
                curInt *= op;
            }
        }
        else if (Objects.equals(first.getToken().getType(), Token.Type.IDENFR)) {
            if (!curSymbolTable.nameExisted(first.getToken().getValue())) { // Undefined Ident
                errors.add(new ErrorNode(ErrorType.IdentUndefined, first.getToken()
                        .getLine(), null, 0).toString());
                curInt = 0;
                curTableEntry = null;
            } else {
                TableEntry tableEntry = curSymbolTable.getEntry(first.getToken().getValue());
                if (!Objects.equals(node.getChild(2).getGrammarSymbol(), GrammarSymbol.FuncRParams)) {
                    // 如果没有参数
                    if (tableEntry.funcParamsNum() > 0) {
                        errors.add(new ErrorNode(ErrorType.ParaNumNotMatch, first.getToken().getLine(),
                                null, 0).toString());
                        curInt = 0;
                        curTableEntry = null;
                        return;
                    }
                } else { // 有实参
                    visitFuncRParams(node.getChild(2), tableEntry);

                    // 参数类型不匹配或者数量不匹配
                    if (ParamErrorHelper(tableEntry, first.getToken().getLine())) {
                        curInt = 0;
                        curTableEntry = null;
                        return;
                    }
                }

                curTableEntry = tableEntry;
            }

            // j mistake
            if (node.getChild(-1) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }


        } else {
            throw new RuntimeException("UnaryExp no match condition!!!!");
        }
    }

    private boolean ParamErrorHelper(TableEntry tableEntry, int lineNum) {
        int rParamSize = tableEntry.getFuncRParamsNum();
        // d =-> Parser.ParaNumNotMatch
        if (tableEntry.funcParamsNum() != rParamSize) {
            if (debug) {
                System.out.println("ParamErrorHelper tableEntry.funcParamsNum() is " + tableEntry.funcParamsNum());
                System.out.println("ParamErrorHelper rParamSize is " + rParamSize);
            }
            errors.add(new ErrorNode(ErrorType.ParaNumNotMatch, lineNum,
                    null, 0).toString());
            return true;
        }

        // e =-> Parser.ParaTypeNotMatch
        ArrayList<FuncParam> definedFuncParams = tableEntry.getFuncParams();
        for (int i = 0; i < rParamSize; i++) {
            if (debug) {
                System.out.println("ParamErrorHelper i is " + i);
                System.out.println("ParamErrorHelper funcRParams.size is " + tableEntry.getFuncRParamsNum());
                System.out.println("ParamErrorHelper funcRParams.get(i).TableEntryType is " + tableEntry.getFuncRParam(i).getTableEntryType());
                System.out.println("ParamErrorHelper funcRParams.get(i) is " + tableEntry.getFuncRParam(i));
            }
            if (!tableEntry.getFuncRParam(i).hasSameType(definedFuncParams.get(i))) {
                errors.add(new ErrorNode(ErrorType.ParaTypeNotMatch, lineNum,
                        null, 0).toString());
                return true;
            }
        }

        return false;
    }

    // ConstExp -> AddExp
    public void visitConstExp(ASTNode node) {
        if (debug) {
            if (node.getGrammarSymbol() != null) {
                System.out.println("ConstExp Node is " + node.getGrammarSymbol());
            } else {
                System.out.println("ConstExp Node is " + node.getToken().getType());
            }
        }
        visitAddExp(node.getChild(0));
    }

    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // AddExp -> MulExp { ('+' | '-') MulExp }
    public void visitAddExp(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter AddExp");
            System.out.println("AddExp children size is " + node.getChildrenSize());
            node.printAllChildren();
        }
        if (node.getChildrenSize() == 1) {
            visitMulExp(node.getChild(0));
        } else {
            visitAddExp(node.getChild(0));
            int leftNum = curInt;
            visitMulExp(node.getChild(2));
            if (isConstant) { // 只有在为常量时需要做简化
                if (node.getChild(1).getToken().getType() == Token.Type.PLUS) {
                    curInt = leftNum + curInt;
                } else {
                    curInt = leftNum - curInt;
                }
                curTableEntry = null;
            }
        }
    }

    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    public void visitMulExp(ASTNode node) {
        if (debug) {
            System.out.println("Visitor Enter MulExp");
        }
        if (node.getChildrenSize() == 1) {
            visitUnaryExp(node.getChild(0));
        } else {
            visitMulExp(node.getChild(0));
            int leftNum = curInt;

            visitUnaryExp(node.getChild(2));
            if (isConstant) { // 常量的话符号表项就返回null
                if (node.getChild(1).getToken().getType() == Token.Type.MULT) {
                    curInt = leftNum * curInt;
                } else if (node.getChild(1).getToken().getType() == Token.Type.DIV) {
                    curInt = leftNum / curInt;
                } else {
                    curInt = leftNum % curInt;
                }
                curTableEntry = null;
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

    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public void visitRelExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitAddExp(node.getChild(0));
        } else {
            visitRelExp(node.getChild(0));
            visitAddExp(node.getChild(2));
        }
    }

    public void print(BufferedWriter outputFile) throws IOException {
        for (String str : errors) {
            outputFile.write(str + "\n");
        }
        outputFile.flush();
    }

}
