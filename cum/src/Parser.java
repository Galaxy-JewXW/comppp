import Lexer.Token;

import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> allTokens;
    private int pos;
    private final int length;

    public Parser(ArrayList<Token> tokens) {
        this.allTokens = new ArrayList<>(tokens);
        this.length = allTokens.size();
        this.pos = 0;
    }

    public void next() {
        pos++;
    }

    public Token curToken() {
        return allTokens.get(pos);
    }
    
    public Token.Type curTokenType() {
        return curToken().getType();
    }

    public Token getToken(int p) {
        return allTokens.get(p);
    }

    private void addCurToken(ASTNode node, int depth) {
        node.addChild(new ASTNode(curToken(), node, depth + 1));
        next();
    }

    public ASTNode parse() {
        return parseCompUnit(0);
    }

    private void addSelfNode(ASTNode node, int depth) {
        ASTNode child = node.removeLastChild();
        ASTNode selfNode = new ASTNode(node.getSymbol(), node, depth + 1);
        node.addChild(selfNode);
        child.setParent(selfNode);
        child.setDepth(depth + 2);
    }

    // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    private ASTNode parseCompUnit(int depth) {
        ASTNode root = new ASTNode(ASTNode.GrammarSymbol.CompUnit, null, depth);
        ASTNode child;
        while (getToken(pos + 2).getType() != Token.Type.LPARENT) {
            child = parseDecl(depth + 1);
            child.setParent(root);
        }
        while (getToken(pos + 1).getType() != Token.Type.MAINTK) {
            child = parseFuncDef(depth + 1);
            child.setParent(root);
        }
        child = parseMainFuncDef(depth + 1);
        child.setParent(root);
        return root;
    }

    // 声明 Decl → ConstDecl | VarDecl
    private ASTNode parseDecl(int depth) {
        ASTNode decl = new ASTNode(ASTNode.GrammarSymbol.Decl, null, depth);
        ASTNode child;
        if (curTokenType() == Token.Type.CONSTTK) {
            child = parseConstDecl(depth + 1);
        } else {
            child = parseVarDecl(depth + 1);
        }
        child.setParent(decl);
        return decl;
    }

    // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private ASTNode parseConstDecl(int depth) {
        ASTNode constDecl = new ASTNode(ASTNode.GrammarSymbol.ConstDecl, null, depth);
        ASTNode child;
        if (curTokenType() != Token.Type.CONSTTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDecl, depth);

        child = parseBType(depth + 1);
        child.setParent(constDecl);

        child = parseConstDef(depth + 1);
        child.setParent(constDecl);

        while (curTokenType() == Token.Type.COMMA) {
            addCurToken(constDecl, depth);
            child = parseConstDef(depth + 1);
            child.setParent(constDecl);
        }

        if (curToken().getVal().equals(";")) {
            addCurToken(constDecl, depth);
            return constDecl;
        }
        throw new IllegalArgumentException();
    }

    // 基本类型 BType → 'int'
    private ASTNode parseBType(int depth) {
        ASTNode bType = new ASTNode(ASTNode.GrammarSymbol.BType, null, depth);
        if (curTokenType() == Token.Type.INTTK) {
            addCurToken(bType, depth);
            return bType;
        }
        throw new IllegalArgumentException();
    }

    // 常数定义 ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    private ASTNode parseConstDef(int depth) {
        ASTNode constDef = new ASTNode(ASTNode.GrammarSymbol.ConstDef, null, depth);
        ASTNode child;

        if (curTokenType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDef, depth);

        while (curTokenType() == Token.Type.LBRACK) {
            addCurToken(constDef, depth);
            child = parseConstExp(depth + 1);
            child.setParent(constDef);
            if (curTokenType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(constDef, depth);
        }

        if (curTokenType() != Token.Type.ASSIGN) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDef, depth);

        child = parseConstInitVal(depth + 1);
        child.setParent(constDef);
        return constDef;
    }

    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private ASTNode parseConstInitVal(int depth) {
        ASTNode constInitVal = new ASTNode(ASTNode.GrammarSymbol.ConstInitVal, null, depth);
        ASTNode child;
        if (curTokenType() != Token.Type.LBRACE) {
            child = parseConstExp(depth + 1);
            child.setParent(constInitVal);
        } else {
            addCurToken(constInitVal, depth);
            if (curTokenType() != Token.Type.RBRACE) {
                child = parseConstInitVal(depth + 1);
                child.setParent(constInitVal);
                while (curTokenType() == Token.Type.COMMA) {
                    addCurToken(constInitVal, depth);
                    child = parseConstInitVal(depth + 1);
                    child.setParent(constInitVal);
                }
            }
            addCurToken(constInitVal, depth);
        }
        return constInitVal;
    }

    // 变量声明 VarDecl → BType VarDef { ',' VarDef } ';'
    private ASTNode parseVarDecl(int depth) {
        ASTNode valDecl = new ASTNode(ASTNode.GrammarSymbol.VarDecl, null, depth);
        ASTNode child = parseBType(depth + 1);
        child.setParent(valDecl);

        child = parseVarDef(depth + 1);
        child.setParent(valDecl);

        while (curTokenType() == Token.Type.COMMA) {
            addCurToken(valDecl, depth);
            child = parseVarDef(depth + 1);
            child.setParent(valDecl);
        }

        if (curToken().getVal().equals(";")) {
            addCurToken(valDecl, depth);
            return valDecl;
        }
        throw new IllegalArgumentException();
    }

    // 变量定义 VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private ASTNode parseVarDef(int depth) {
        ASTNode varDef = new ASTNode(ASTNode.GrammarSymbol.VarDef, null, depth);
        ASTNode child;
        if (curTokenType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(varDef, depth);
        while (curTokenType() == Token.Type.LBRACK) {
            addCurToken(varDef, depth);
            child = parseConstExp(depth + 1);
            child.setParent(varDef);
            if (curTokenType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(varDef, depth);
        }
        if (curTokenType() == Token.Type.ASSIGN) {
            addCurToken(varDef, depth);
            child = parseInitVal(depth + 1);
            child.setParent(varDef);
        }
        return varDef;
    }

    // 变量初值 InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private ASTNode parseInitVal(int depth) {
        ASTNode initVal = new ASTNode(ASTNode.GrammarSymbol.InitVal, null, depth);
        ASTNode child;
        if (curTokenType() == Token.Type.LBRACE) {
            addCurToken(initVal, depth);
            if (curTokenType() != Token.Type.RBRACE) {
                child = parseInitVal(depth + 1);
                child.setParent(initVal);
                while (curTokenType() == Token.Type.COMMA) {
                    addCurToken(initVal, depth);
                    child = parseInitVal(depth + 1);
                    child.setParent(initVal);
                }
            }
            addCurToken(initVal, depth);
        } else {
            child = parseExp(depth + 1);
            child.setParent(initVal);
        }
        return initVal;
    }

    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private ASTNode parseFuncDef(int depth) {
        ASTNode funcDef = new ASTNode(ASTNode.GrammarSymbol.FuncDef, null, depth);
        ASTNode child;

        child = parseFuncType(depth + 1);
        child.setParent(funcDef);

        if (curTokenType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcDef, depth);

        if (curTokenType() != Token.Type.LPARENT) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcDef, depth);

        if (curTokenType() != Token.Type.RPARENT) {
            child = parseFuncFParams(depth + 1);
            child.setParent(funcDef);
        }

        addCurToken(funcDef, depth);
        child = parseBlock(depth + 1);
        child.setParent(funcDef);
        return funcDef;
    }

    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    public ASTNode parseMainFuncDef(int depth) {
        ASTNode mainFuncDef = new ASTNode(ASTNode.GrammarSymbol.MainFuncDef, null, depth);
        ASTNode child;

        if (curTokenType() != Token.Type.INTTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curTokenType() != Token.Type.MAINTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curTokenType() != Token.Type.LPARENT) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curTokenType() != Token.Type.RPARENT) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        child = parseBlock(depth + 1);
        child.setParent(mainFuncDef);
        return mainFuncDef;
    }

    // 函数类型 FuncType → 'void' | 'int'
    private ASTNode parseFuncType(int depth) {
        ASTNode funcType = new ASTNode(ASTNode.GrammarSymbol.FuncType, null, depth);
        if (curTokenType() == Token.Type.VOIDTK || curTokenType() == Token.Type.INTTK) {
            addCurToken(funcType, depth);
        } else {
            throw new IllegalArgumentException();
        }
        return funcType;
    }

    // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    private ASTNode parseFuncFParams(int depth) {
        ASTNode funcFParams = new ASTNode(ASTNode.GrammarSymbol.FuncFParams, null, depth);
        ASTNode child;

        child = parseFuncFParam(depth + 1);
        child.setParent(funcFParams);

        while (curTokenType() == Token.Type.COMMA) {
            addCurToken(funcFParams, depth);
            child = parseFuncFParam(depth + 1);
            child.setParent(funcFParams);
        }

        return funcFParams;
    }

    // 函数形参 FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private ASTNode parseFuncFParam(int depth) {
        ASTNode funcFParam = new ASTNode(ASTNode.GrammarSymbol.FuncFParam, null, depth);
        ASTNode child;
        child = parseBType(depth + 1);
        child.setParent(funcFParam);
        if (curTokenType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcFParam, depth);
        if (curTokenType() == Token.Type.LBRACK) {
            addCurToken(funcFParam, depth);
            if (curTokenType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(funcFParam, depth);
            while (curTokenType() == Token.Type.LBRACK) {
                addCurToken(funcFParam, depth);
                child = parseConstExp(depth + 1);
                child.setParent(funcFParam);
                if (curTokenType() != Token.Type.RBRACK) {
                    throw new IllegalArgumentException();
                }
                addCurToken(funcFParam, depth);
            }
        }
        return funcFParam;
    }

    // 语句块 Block → '{' { BlockItem } '}'
    private ASTNode parseBlock(int depth) {
        ASTNode block = new ASTNode(ASTNode.GrammarSymbol.Block, null, depth);
        ASTNode child;

        if (curTokenType() != Token.Type.LBRACE) {
            throw new IllegalArgumentException();
        }
        addCurToken(block, depth);

        while (curTokenType() != Token.Type.RBRACE) {
            child = parseBlockItem(depth + 1);
            child.setParent(block);
        }
        addCurToken(block, depth);
        return block;
    }

    // 语句块项 BlockItem → Decl | Stmt
    private ASTNode parseBlockItem(int depth) {
        ASTNode blockItem = new ASTNode(ASTNode.GrammarSymbol.BlockItem, null, depth);
        ASTNode child;

        if (curTokenType() == Token.Type.CONSTTK || curTokenType() == Token.Type.INTTK) {
            child = parseDecl(depth + 1);
        } else {
            child = parseStmt(depth + 1);
        }
        child.setParent(blockItem);
        return blockItem;
    }

    // 语句 Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
    // | [Exp] ';' //有无Exp两种情况
    // | Block
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
    // | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    // | 'break' ';' | 'continue' ';'
    // | 'return' [Exp] ';' // 1.有Exp 2.无Exp
    // | LVal '=' 'getint''('')'';'
    // | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
    private ASTNode parseStmt(int depth) {
        ASTNode stmt = new ASTNode(ASTNode.GrammarSymbol.Stmt, null, depth);
        ASTNode child;
        if (curTokenType() == Token.Type.IFTK) {
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);

            child = parseCond(depth + 1);
            child.setParent(stmt);

            if (curTokenType() != Token.Type.RPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);

            child = parseStmt(depth + 1);
            child.setParent(stmt);

            if (curTokenType() == Token.Type.ELSETK) {
                addCurToken(stmt, depth);
                child = parseStmt(depth + 1);
                child.setParent(stmt);
            }
        } else if (curTokenType() == Token.Type.FORTK) {
            addCurToken(stmt, depth);

            if (curTokenType() != Token.Type.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);

            if (curTokenType() != Token.Type.SEMICN) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
            }
            addCurToken(stmt, depth);

            if (curTokenType() != Token.Type.SEMICN) {
                child = parseCond(depth + 1);
                child.setParent(stmt);
            }
            addCurToken(stmt, depth);

            if (curTokenType() != Token.Type.RPARENT) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
            }
            addCurToken(stmt, depth);

            child = parseStmt(depth + 1);
            child.setParent(stmt);
        } else if (curTokenType() == Token.Type.BREAKTK || curTokenType() == Token.Type.CONTINUETK) {
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.SEMICN) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);
        } else if (curTokenType() == Token.Type.RETURNTK) {
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.SEMICN) {
                child = parseExp(depth + 1);
                child.setParent(stmt);
            }
            addCurToken(stmt, depth);
        } else if (curTokenType() == Token.Type.IDENFR && assignBeforeSemicn()) {
            child = parseLVal(depth + 1);
            child.setParent(stmt);
            if (curTokenType() == Token.Type.ASSIGN) {
                addCurToken(stmt, depth);
                if (curTokenType() == Token.Type.GETINTTK) {
                    addCurToken(stmt, depth);
                    if (curTokenType() != Token.Type.LPARENT) {
                        throw new IllegalArgumentException();
                    }
                    addCurToken(stmt, depth);
                    if (curTokenType() != Token.Type.RPARENT) {
                        throw new IllegalArgumentException();
                    }
                    addCurToken(stmt, depth);
                    if (curTokenType() != Token.Type.SEMICN) {
                        throw new IllegalArgumentException();
                    }
                    addCurToken(stmt, depth);
                } else {
                    child = parseExp(depth + 1);
                    child.setParent(stmt);
                    if (curTokenType() != Token.Type.SEMICN) {
                        throw new IllegalArgumentException();
                    }
                    addCurToken(stmt, depth);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (curTokenType() == Token.Type.PRINTFTK) {
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.STRCON) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);
            while (curTokenType() == Token.Type.COMMA) {
                addCurToken(stmt, depth);
                child = parseExp(depth + 1);
                child.setParent(stmt);
            }
            if (curTokenType() != Token.Type.RPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);
            if (curTokenType() != Token.Type.SEMICN) {
                throw new IllegalArgumentException();
            }
            addCurToken(stmt, depth);
        } else if (curTokenType() == Token.Type.LBRACE) {
            child = parseBlock(depth + 1);
            child.setParent(stmt);
        } else if (curTokenType() != Token.Type.SEMICN) {
            child = parseExp(depth + 1);
            child.setParent(stmt);
            addCurToken(stmt, depth);
        } else {
            addCurToken(stmt, depth);
        }
        return stmt;
    }

    // 语句 ForStmt → LVal '=' Exp
    private ASTNode parseForStmt(int depth) {
        ASTNode forStmt = new ASTNode(ASTNode.GrammarSymbol.ForStmt, null, depth);
        ASTNode child;

        child = parseLVal(depth + 1);
        child.setParent(forStmt);

        if (curTokenType() != Token.Type.ASSIGN) {
            throw new IllegalArgumentException();
        }
        addCurToken(forStmt, depth);

        child = parseExp(depth + 1);
        child.setParent(forStmt);
        return forStmt;
    }

    // 表达式 Exp → AddExp
    private ASTNode parseExp(int depth) {
        ASTNode exp = new ASTNode(ASTNode.GrammarSymbol.Exp, null, depth);
        ASTNode child;
        child = parseAddExp(depth + 1);
        child.setParent(exp);
        return exp;
    }

    // 条件表达式 Cond → LOrExp
    private ASTNode parseCond(int depth) {
        ASTNode cond = new ASTNode(ASTNode.GrammarSymbol.Cond, null, depth);
        ASTNode child;

        // LOrExp
        child = parseLOrExp(depth + 1);
        child.setParent(cond);

        return cond;
    }

    // 左值表达式 LVal → Ident {'[' Exp ']'}
    private ASTNode parseLVal(int depth) {
        ASTNode lVal = new ASTNode(ASTNode.GrammarSymbol.LVal, null, depth);
        ASTNode child;
        if (curTokenType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(lVal, depth);
        while (curTokenType() == Token.Type.LBRACK) {
            addCurToken(lVal, depth);
            child = parseExp(depth + 1);
            child.setParent(lVal);
            if (curTokenType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(lVal, depth);
        }
        return lVal;
    }

    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    private ASTNode parsePrimaryExp(int depth) {
        ASTNode primaryExp = new ASTNode(ASTNode.GrammarSymbol.PrimaryExp, null, depth);
        ASTNode child;
        if (curTokenType() == Token.Type.LPARENT) {
            addCurToken(primaryExp, depth);
            child = parseExp(depth + 1);
            child.setParent(primaryExp);
            if (curTokenType() != Token.Type.RPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(primaryExp, depth);
        } else if (curTokenType() == Token.Type.IDENFR) {
            child = parseLVal(depth + 1);
            child.setParent(primaryExp);
        } else if (curTokenType() == Token.Type.INTCON) {
            child = parseNumber(depth + 1);
            child.setParent(primaryExp);
        } else {
            throw new IllegalArgumentException();
        }
        return primaryExp;
    }

    // 数值 Number → IntConst
    private ASTNode parseNumber(int depth) {
        ASTNode number = new ASTNode(ASTNode.GrammarSymbol.Number, null, depth);
        if (curTokenType() != Token.Type.INTCON) {
            throw new IllegalArgumentException();
        }
        addCurToken(number, depth);
        return number;
    }

    // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private ASTNode parseUnaryExp(int depth) {
        ASTNode unaryExp = new ASTNode(ASTNode.GrammarSymbol.UnaryExp, null, depth);
        ASTNode child;

        if (curTokenType() == Token.Type.IDENFR && allTokens.get(pos + 1).getType() == Token.Type.LPARENT) {
            addCurToken(unaryExp, depth);
            if (curTokenType() != Token.Type.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(unaryExp, depth);
            if (curTokenType() != Token.Type.RPARENT) {
                child = parseFuncRParams(depth + 1);
                child.setParent(unaryExp);
            }
            addCurToken(unaryExp, depth);
        } else if (curTokenType() == Token.Type.PLUS || curTokenType() == Token.Type.MINU
                || curTokenType() == Token.Type.NOT) {
            child = parseUnaryOp(depth + 1);
            child.setParent(unaryExp);
            child = parseUnaryExp(depth + 1);
            child.setParent(unaryExp);
        } else {
            child = parsePrimaryExp(depth + 1);
            child.setParent(unaryExp);
        }
        return unaryExp;
    }

    // 单目运算符 UnaryOp → '+' | '−' | '!'
    private ASTNode parseUnaryOp(int depth) {
        ASTNode unaryOp = new ASTNode(ASTNode.GrammarSymbol.UnaryOp, null, depth);
        if (curTokenType() != Token.Type.PLUS && curTokenType() != Token.Type.MINU
                && curTokenType() != Token.Type.NOT) {
            throw new IllegalArgumentException();
        } else {
            addCurToken(unaryOp, depth);
        }
        return unaryOp;
    }

    // 函数实参表 FuncRParams → Exp { ',' Exp }
    private ASTNode parseFuncRParams(int depth) {
        ASTNode funcRParams = new ASTNode(ASTNode.GrammarSymbol.FuncRParams, null, depth);
        ASTNode child;
        child = parseExp(depth + 1);
        child.setParent(funcRParams);
        while (curTokenType() == Token.Type.COMMA) {
            addCurToken(funcRParams, depth);
            child = parseExp(depth + 1);
            child.setParent(funcRParams);
        }
        return funcRParams;
    }

    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // Better Definition: MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
    private ASTNode parseMulExp(int depth) {
        ASTNode mulExp = new ASTNode(ASTNode.GrammarSymbol.MulExp, null, depth);
        ASTNode child;

        child = parseUnaryExp(depth + 1);
        child.setParent(mulExp);

        while (curTokenType() == Token.Type.MULT
                || curTokenType() == Token.Type.DIV || curTokenType() == Token.Type.MOD) {
            addSelfNode(mulExp, depth);
            addCurToken(mulExp, depth);
            child = parseUnaryExp(depth + 1);
            child.setParent(mulExp);
        }
        return mulExp;
    }

    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    // Or: AddExp -> MulExp { ('+' | '−') MulExp }
    private ASTNode parseAddExp(int depth) {
        ASTNode addExp = new ASTNode(ASTNode.GrammarSymbol.AddExp, null, depth);
        ASTNode child;
        child = parseMulExp(depth + 1);
        child.setParent(addExp);
        while (curTokenType() == Token.Type.PLUS || curTokenType() == Token.Type.MINU) {
            addSelfNode(addExp, depth);
            addCurToken(addExp, depth);
            child = parseMulExp(depth + 1);
            child.setParent(addExp);
        }
        return addExp;
    }

    // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    private ASTNode parseRelExp(int depth) {
        ASTNode relExp = new ASTNode(ASTNode.GrammarSymbol.RelExp, null, depth);
        ASTNode child;
        child = parseAddExp(depth + 1);
        child.setParent(relExp);
        while (curTokenType() == Token.Type.LSS || curTokenType() == Token.Type.LEQ
                || curTokenType() == Token.Type.GRE || curTokenType() == Token.Type.GEQ) {
            addSelfNode(relExp, depth);
            addCurToken(relExp, depth);
            child = parseAddExp(depth + 1);
            child.setParent(relExp);
        }
        return relExp;
    }

    // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private ASTNode parseEqExp(int depth) {
        ASTNode eqExp = new ASTNode(ASTNode.GrammarSymbol.EqExp, null, depth);
        ASTNode child;
        child = parseRelExp(depth + 1);
        child.setParent(eqExp);
        while (curTokenType() == Token.Type.EQL || curTokenType() == Token.Type.NEQ) {
            addSelfNode(eqExp, depth);
            addCurToken(eqExp, depth);
            child = parseRelExp(depth + 1);
            child.setParent(eqExp);
        }
        return eqExp;
    }

    // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
    private ASTNode parseLAndExp(int depth) {
        ASTNode lAndExp = new ASTNode(ASTNode.GrammarSymbol.LAndExp, null, depth);
        ASTNode child;
        child = parseEqExp(depth + 1);
        child.setParent(lAndExp);
        while (curTokenType() == Token.Type.AND) {
            addSelfNode(lAndExp, depth);
            addCurToken(lAndExp, depth);
            child = parseEqExp(depth + 1);
            child.setParent(lAndExp);
        }
        return lAndExp;
    }

    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    private ASTNode parseLOrExp(int depth) {
        ASTNode lOrExp = new ASTNode(ASTNode.GrammarSymbol.LOrExp, null, depth);
        ASTNode child;
        child = parseLAndExp(depth + 1);
        child.setParent(lOrExp);
        while (curTokenType() == Token.Type.OR) {
            addSelfNode(lOrExp, depth);
            addCurToken(lOrExp, depth);
            child = parseLAndExp(depth + 1);
            child.setParent(lOrExp);
        }
        return lOrExp;
    }

    // 常量表达式 ConstExp → AddExp
    private ASTNode parseConstExp(int depth) {
        ASTNode constExp = new ASTNode(ASTNode.GrammarSymbol.ConstExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        child.setParent(constExp);
        return constExp;
    }

    private boolean assignBeforeSemicn() {
        int i = pos;
        while (i < length) {
            if (allTokens.get(i).getType() == Token.Type.SEMICN) {
                return false;
            } else if (allTokens.get(i).getType() == Token.Type.ASSIGN) {
                return true;
            }
            i++;
        }
        return true;
    }
}
