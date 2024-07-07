import EntryType.*;
import Lexer.Token;
import Parser.*;
import Table.SymbolTable;
import Table.TableEntry;
import Table.TableEntryType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Visitor {
    private final ASTNode root;
    private SymbolTable curSymbolTable = new SymbolTable(null, true);
    private final ArrayList<String> errors = new ArrayList<>();
    private final ArrayList<Integer> curDimensions = new ArrayList<>();
    private boolean isConstant = false;
    private int curInt;
    private boolean receiveReturn = false;
    private FuncType curFuncType;
    private int funcEndLine;
    private TableEntry curTableEntry = null;
    private int forLevel = 0;

    public Visitor(ASTNode root) {
        this.root = root;
        this.curFuncType = FuncType.NO;
        this.funcEndLine = 0;
    }

    public void print(BufferedWriter outputFile) throws IOException {
        for (String str : errors) {
            outputFile.write(str + "\n");
        }
        outputFile.flush();
    }

    public void visit() {
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
    // i -> MissingSEMICN
    private void visitConstDecl(ASTNode node) {
        for (int i = 2; i < node.getChildrenSize() - 1; i += 2) {
            visitConstDef(node.getChild(i));
        }
        ASTNode lastChild = node.getChild(-1);
        if (lastChild instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }
    }

    // 常数定义 ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    // b -> IdentRedefined
    // k -> RBRACKMissing
    private void visitConstDef(ASTNode node) {
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
        ConstValue value = visitConstInitVal(node.getChild(-1), curDimensions.size());
        switch (value.getType()) {
            case "int":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, value.getVar()));
                break;
            case "Array1":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, value.getArray1()));
                break;
            case "Array2":
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, value.getArray2()));
                break;
            default:
                break;
        }
    }

    // 常量初值 ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private ConstValue visitConstInitVal(ASTNode node, int dimension) {
        ConstValue value = null;
        int d1;
        switch (dimension) {
            case 0:
                isConstant = true;
                visitConstExp(node.getChild(0));
                isConstant = false;

                ConstVar constVar = new ConstVar(curInt);
                value = new ConstValue("int", constVar);
                break;
            case 1:
                d1 = curDimensions.get(0);
                ArrayList<Integer> values = new ArrayList<>();
                for (int i = 1, p = 0; p < d1; i += 2, p++) {
                    isConstant = true;
                    visitConstExp(node.getChild(i).getChild(0));
                    isConstant = false;
                    values.add(curInt);
                }
                value = new ConstValue("Array1", new ConstArray1(d1, values));
                break;
            case 2:
                d1 = curDimensions.get(0);
                int d2 = curDimensions.get(1);
                int p = 0;
                value = new ConstValue("Array2", new ConstArray2(d1, d2));
                for (int i = 1; p < d1; i += 2, p++) {
                    ASTNode temp = node.getChild(i);
                    int d11 = (temp.getChildrenSize() + 1) / 2;
                    for (int j = 1; j < d11; j += 2) {
                        isConstant = true;
                        visitConstExp(temp.getChild(j).getChild(0));
                        isConstant = false;
                        value.addValues(curInt);
                    }
                }
                break;
            default:
                break;
        }
        return value;
    }

    // 变量声明 VarDecl -> BType VarDef { ',' VarDef } ';' // i
    // i -> SEMICNMissing
    private void visitVarDecl(ASTNode node) {
        for (int i = 1; i < node.getChildrenSize() - 1; i += 2) {
            visitVarDef(node.getChildren().get(i));
        }
        ASTNode last = node.getChild(-1);
        if (last instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }
    }

    // 变量定义 VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    // b -> IdentRedefined
    // k -> RBRACKMissing
    private void visitVarDef(ASTNode node) {
        ASTNode ident = node.getChild(0);
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
            return;
        }
        int l = node.getChildrenSize();
        curDimensions.clear();
        for (int i = 1; i < l - 2 && node.getChild(i).getToken()
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
                        new TableEntry(ident, new Var()));
                break;
            case 1:
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, new Array1(curDimensions.get(0))));
                break;
            case 2:
                curSymbolTable.addEntry(ident.getToken().getValue(),
                        new TableEntry(ident, new Array2(curDimensions.get(0), curDimensions.get(1))));
                break;
            default:
                break;
        }

    }

    // 变量初值 InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void visitInitVal(ASTNode node) {
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
    private void visitFuncDef(ASTNode node) {
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

        if (funcType.getToken().getType() == Token.Type.VOIDTK) {
            FunctionVoid functionVoid = new FunctionVoid();
            funcEntry = new TableEntry(ident, functionVoid);
            curFuncType = FuncType.VOID;
        } else if (funcType.getToken().getType() == Token.Type.INTTK) {
            FunctionInt functionInt = new FunctionInt();
            funcEntry = new TableEntry(ident, functionInt);
            curFuncType = FuncType.INT;
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
        visitBlock(node.getChild(-1), true);

        if (curFuncType == FuncType.INT && !receiveReturn) {
            errors.add(new ErrorNode(ErrorType.ReturnMissing, funcEndLine, null, 0)
                    .toString());
        }

        curSymbolTable = curSymbolTable.getParent();
        receiveReturn = false;
        curFuncType = FuncType.NO;
    }

    // 主函数定义 MainFuncDef -> 'int' 'main' '(' ')' Block
    // g -> ReturnMissing
    // j -> RPARENTMissing
    private void visitMainFuncDef(ASTNode node) {
        SymbolTable mainSymbolTable = new SymbolTable(curSymbolTable, false);
        curSymbolTable.addChildTable(mainSymbolTable);
        curSymbolTable = mainSymbolTable;

        receiveReturn = false;
        curFuncType = FuncType.MAIN;

        visitBlock(node.getChild(-1), true);

        if (!receiveReturn) {
            errors.add(new ErrorNode(ErrorType.ReturnMissing, funcEndLine,
                    null, 0).toString());
        }

        receiveReturn = false;
        curFuncType = FuncType.NO;
        curSymbolTable = curSymbolTable.getParent();
    }

    // 函数形参表 FuncFParams -> FuncFParam { ',' FuncFParam }
    private void visitFuncFParams(ASTNode node, TableEntry funcEntry) {
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            TableEntry funcFParam = visitFuncFParam(node.getChild(i));
            curSymbolTable.addEntry(funcFParam.getName(), funcFParam);
            funcEntry.addParamForFuncEntry(funcFParam);
        }
    }

    // 函数形参 FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    // b -> IdentRedefined
    // k -> RBRACKMissing
    private TableEntry visitFuncFParam(ASTNode node) {
        ASTNode ident = node.getChild(1);
        if (curSymbolTable.containsEntry(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentRedefined, ident.getToken()
                    .getLine(), null, 0).toString());
        }
        switch (node.getChildrenSize()) {
            case 2: // BType Ident
                return new TableEntry(ident, new Var());
            case 4: // BType Ident '[' ']'
                if (node.getChild(3) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                return new TableEntry(ident, new Array1());
            case 7: // BType Ident '[' ']'  '[' ConstExp ']'
                if (node.getChild(3) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                if (node.getChild(6) instanceof ErrorNode errorNode) {
                    errors.add(errorNode.toString());
                }
                visitConstExp(node.getChild(5));
                return new TableEntry(ident, new Array2(curInt));
            default:
                throw new RuntimeException();
        }
    }

    // 语句块 Block -> '{' { BlockItem } '}'
    private void visitBlock(ASTNode node, boolean inFunc) {
        for (int i = 1; i < node.getChildrenSize() - 1; i++) {
            visitBlockItem(node.getChild(i), inFunc);
        }
        funcEndLine = node.getChild(-1).getToken().getLine();
    }

    // 语句块项 BlockItem -> Decl | Stmt
    private void visitBlockItem(ASTNode node, boolean inFunc) {
        if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.Decl) {
            visitDecl(node.getChild(0));
        } else {
            visitStmt(node.getChild(0), inFunc);
        }
    }

    // 语句 Stmt -> LVal '=' Exp ';' | [Exp] ';' | Block // h i
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
    // | 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    // | 'break' ';' | 'continue' ';' // i m
    // | 'return' [Exp] ';' // f i
    // | LVal '=' 'getint''('')'';' // h i j
    // | 'printf''('FormatString{,Exp}')'';' // i j l
    private void visitStmt(ASTNode node, boolean inFunc) {
        ASTNode first = node.getChild(0);
        ASTNode last = node.getChild(-1);

        if (first.getGrammarSymbol() == GrammarSymbol.LVal) {
            // Stmt -> LVal '=' Exp ';'
            // Stmt -> LVal '=' 'getint''('')'';'
            visitLval(first);
            if (curTableEntry == null) {
                return;
            } else if (curTableEntry.isConst()) {
                errors.add(new ErrorNode(ErrorType.ConstAssign, first.getChild(0).getToken().getLine(),
                        null, 0).toString());
            }

            if (node.getChild(2).getGrammarSymbol() == GrammarSymbol.Exp) {
                // Stmt -> LVal '=' Exp ';'
                visitExp(node.getChild(-2));
            } else if (node.getChild(4) instanceof ErrorNode errorNode) {
                // Stmt -> LVal '=' 'getint''('')'';'
                errors.add(errorNode.toString());
            }
            
            if (last instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (first.getGrammarSymbol() == GrammarSymbol.Block) {
            curSymbolTable = new SymbolTable(curSymbolTable, false);
            visitBlock(first, inFunc);
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
            visitStmt(node.getChild(4), inFunc);
            if (node.getChildrenSize() > 5) {
                visitStmt(node.getChild(6), inFunc);
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
                    visitStmt(node.getChild(i), inFunc);
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
            receiveReturn = inFunc;
            if (curFuncType == FuncType.VOID &&
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
            // 'printf' '(' FormatString { , Exp } ')' ';'
            ASTNode formatString = node.getChild(2);
            int count = checkFormatString(formatString);
            if (count < 0) {
                errors.add(new ErrorNode(ErrorType.IllegalChar, formatString.getToken().getLine(),
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
            if (c == '\\') {
                if (i < l - 1 && format.charAt(i + 1) == 'n') {
                    i++;
                } else {
                    return -1;
                }
            }
        }
        return count;
    }

    // 语句 ForStmt -> LVal '=' Exp
    // h -> ConstAssign
    private void visitForStmt(ASTNode node) {
        ASTNode first = node.getChild(0);
        visitLval(first);
        if (curTableEntry == null) {
            return;
        } else if (curTableEntry.isConst()) {
            errors.add(new ErrorNode(ErrorType.ConstAssign, first.getChild(0).getToken().getLine(),
                    null, 0).toString());
        }
        visitExp(node.getChild(-1));
    }

    // 表达式 Exp -> AddExp
    private void visitExp(ASTNode node) {
        visitAddExp(node.getChild(0));
    }

    // 条件表达式 Cond -> LOrExp
    private void visitCond(ASTNode node) {
        visitLOrExp(node.getChild(0));
    }

    // 左值表达式 LVal -> Ident {'[' Exp ']'}
    // c -> IdentUndefined
    // k -> RBRACKMissing
    private void visitLval(ASTNode node) {
        ASTNode ident = node.getChild(0);
        if (!curSymbolTable.haveName(ident.getToken().getValue())) {
            errors.add(new ErrorNode(ErrorType.IdentUndefined, ident.getToken()
                    .getLine(), null, 0).toString());
            curInt = 0;
            curTableEntry = null;
            return;
        }
        int l = node.getChildrenSize();
        TableEntry tableEntry = curSymbolTable.getEntry(ident.getToken().getValue());
        if (l == 1) {
            if (isConstant) {
                curInt = tableEntry.getVarValue();
            }
            curTableEntry = tableEntry;
            return;
        }

        ASTNode exp1 = node.getChild(2);
        visitExp(exp1);
        int v1;
        if (node.getChild(3) instanceof ErrorNode errorNode) {
            errors.add(errorNode.toString());
        }

        TableEntry referencedTableEntry;
        if (l == 4) {
            if (tableEntry.getTableEntryType() == TableEntryType.Array1 ||
                    tableEntry.getTableEntryType() == TableEntryType.ConstArray1) {
                referencedTableEntry = new TableEntry(tableEntry, TableEntryType.Var, curInt);
                if (isConstant) {
                    curInt = referencedTableEntry.getValueFromReferencedArray1(curInt);
                    curTableEntry = null;
                } else {
                    curTableEntry = referencedTableEntry;
                }
            } else {
                curTableEntry = new TableEntry(tableEntry, TableEntryType.Array1, curInt);
            }
        } else if (l == 7) {
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

    // 基本表达式 PrimaryExp -> '(' Exp ')' | LVal | Number
    private void visitPrimaryExp(ASTNode node) {
        if (node.getChildrenSize() > 1) {
            visitExp(node.getChild(1));
            if (node.getChild(-1) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        } else if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.LVal) {
            visitLval(node.getChild(0));
        } else if (node.getChild(0).getGrammarSymbol() == GrammarSymbol.Number) {
            visitNumber(node.getChild(0));
        }
    }

    // 数值 Number -> IntConst
    private void visitNumber(ASTNode node) {
        curInt = Integer.parseInt(node.getChild(0).getToken().getValue());
        curTableEntry = null;
    }

    // 一元表达式 UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // c -> Parser.IdentUndefined
    // d -> Parser.ParaNumNotMatch
    // e -> Parser.ParaTypeNotMatch
    // j -> Parser.RPARENTMissing
    private void visitUnaryExp(ASTNode node) {
        ASTNode first = node.getChild(0);
        if (first.getGrammarSymbol() == GrammarSymbol.PrimaryExp) {
            visitPrimaryExp(first);
        } else if (first.getGrammarSymbol() == GrammarSymbol.UnaryOp) {
            int op = (first.getChild(0).getToken().getType() == Token.Type.PLUS) ? 1 :
                    (first.getChild(0).getToken().getType() == Token.Type.MINU) ? -1 : 0;
            visitUnaryExp(node.getChild(1));
            if (op == 1 || op == -1) {
                curInt *= op;
            }
        } else if (first.getToken().getType() == Token.Type.IDENFR) {
            if (!curSymbolTable.haveName(first.getToken().getValue())) {
                errors.add(new ErrorNode(ErrorType.IdentUndefined, first.getToken()
                        .getLine(), null, 0).toString());
                curInt = 0;
                curTableEntry = null;
            } else {
                TableEntry tableEntry = curSymbolTable.getEntry(first.getToken().getValue());
                if (node.getChild(2).getGrammarSymbol() != GrammarSymbol.FuncRParams) {
                    if (tableEntry.funcParamsNum() > 0) {
                        errors.add(new ErrorNode(ErrorType.ParaNumNotMatch, first.getToken().getLine(),
                                null, 0).toString());
                        curInt = 0;
                        curTableEntry = null;
                        return;
                    }
                } else {
                    visitFuncRParams(node.getChild(2), tableEntry);
                    if (ParamErrorHelper(tableEntry, first.getToken().getLine())) {
                        curInt = 0;
                        curTableEntry = null;
                        return;
                    }
                }
                curTableEntry = tableEntry;
            }
            if (node.getChild(-1) instanceof ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }
        }
    }

    // 函数实参表 FuncRParams -> Exp { ',' Exp }
    private void visitFuncRParams(ASTNode node, TableEntry tableEntry) {
        tableEntry.clearFuncRParams();
        for (int i = 0; i < node.getChildrenSize(); i += 2) {
            visitExp(node.getChild(i));
            tableEntry.addFuncRParam(Objects.requireNonNullElseGet
                    (curTableEntry, () -> new TableEntry(curInt)));
        }
    }

    // 乘除模表达式 MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private void visitMulExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitUnaryExp(node.getChild(0));
        } else {
            visitMulExp(node.getChild(0));
            int leftNum = curInt;
            visitUnaryExp(node.getChild(2));
            if (isConstant) {
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

    // 加减表达式 AddExp -> MulExp { ('+' | '-') MulExp }
    private void visitAddExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitMulExp(node.getChild(0));
        } else {
            visitAddExp(node.getChild(0));
            int leftNum = curInt;
            visitMulExp(node.getChild(2));
            if (isConstant) {
                if (node.getChild(1).getToken().getType() == Token.Type.PLUS) {
                    curInt = leftNum + curInt;
                } else {
                    curInt = leftNum - curInt;
                }
                curTableEntry = null;
            }
        }
    }

    // 关系表达式 RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private void visitRelExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitAddExp(node.getChild(0));
        } else {
            visitRelExp(node.getChild(0));
            visitAddExp(node.getChild(2));
        }
    }

    // 相等性表达式 EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    private void visitEqExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitRelExp(node.getChild(0));
        } else {
            visitEqExp(node.getChild(0));
            visitRelExp(node.getChild(2));
        }
    }

    // 逻辑与表达式 LAndExp -> EqExp | LAndExp '&&' EqExp
    private void visitLAndExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitEqExp(node.getChild(0));
        } else {
            visitLAndExp(node.getChild(0));
            visitEqExp(node.getChild(2));
        }
    }

    // 逻辑或表达式 LOrExp -> LAndExp | LOrExp '||' LAndExp
    private void visitLOrExp(ASTNode node) {
        if (node.getChildrenSize() == 1) {
            visitLAndExp(node.getChild(0));
        } else {
            visitLOrExp(node.getChild(0));
            visitLAndExp(node.getChild(2));
        }
    }

    // 常量表达式 ConstExp -> AddExp
    private void visitConstExp(ASTNode node) {
        visitAddExp(node.getChild(0));
    }

    private boolean ParamErrorHelper(TableEntry tableEntry, int lineNum) {
        int rParamSize = tableEntry.getFuncRParamsSize();
        // d -> Parser.ParaNumNotMatch
        if (tableEntry.funcParamsNum() != rParamSize) {
            errors.add(new ErrorNode(ErrorType.ParaNumNotMatch, lineNum,
                    null, 0).toString());
            return true;
        }

        // e -> Parser.ParaTypeNotMatch
        ArrayList<FuncParam> definedFuncParams = tableEntry.getFuncParams();
        for (int i = 0; i < rParamSize; i++) {
            if (!tableEntry.getFuncRParam(i).haveSameType(definedFuncParams.get(i))) {
                errors.add(new ErrorNode(ErrorType.ParaTypeNotMatch, lineNum,
                        null, 0).toString());
                return true;
            }
        }

        return false;
    }

}
