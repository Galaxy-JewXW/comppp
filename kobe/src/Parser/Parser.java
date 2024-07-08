package Parser;

import Lexer.Token;

import java.util.ArrayList;
import java.util.Objects;

public class Parser {
    private int pos;
    private final ArrayList<Token> tokens;
    private final int length;

    public Parser(ArrayList<Token> tokens) {
        this.length = tokens.size();
        this.pos = 0;
        this.tokens = tokens;
    }

    public Token curToken() {
        return tokens.get(pos);
    }
    
    public Token.TokenType curTokenType() {
        return curToken().getType();
    }

    private void addCurToken(ASTNode node, int depth) {
        node.addChild(new ASTNode(curToken(), node, depth + 1));
        pos++;
    }

    private void connect(ASTNode parent, ASTNode child) {
        parent.addChild(child);
        child.setParent(parent);
    }

    public ASTNode parse() {
        return parseCompUnit(0);
    }

    // 编译单元 CompUnit -> {Decl} {FuncDef} MainFuncDef
    public ASTNode parseCompUnit(int depth) {
        ASTNode root = new ASTNode(GrammarSymbol.CompUnit, null, depth);
        ASTNode child;
        // {Decl}
        while (tokens.get(pos + 2).getType() != Token.TokenType.LPARENT) {
            child = parseDecl(depth + 1);
            connect(root, child);
        }
        // {FuncDef}
        while (tokens.get(pos + 1).getType() != Token.TokenType.MAINTK) {
            child = parseFuncDef(depth + 1);
            connect(root, child);
        }
        // MainFuncDef
        child = parseMainFuncDef(depth + 1);
        connect(root, child);
        return root;
    }

    // 声明 Decl -> ConstDecl | VarDecl
    public ASTNode parseDecl(int depth) {
        ASTNode decl = new ASTNode(GrammarSymbol.Decl, null, depth);
        ASTNode child;
        if (curTokenType() == Token.TokenType.CONSTTK) {
            child = parseConstDecl(depth + 1);
        } else {
            child = parseVarDecl(depth + 1);
        }
        connect(decl, child);
        return decl;
    }

    // 常量声明 ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    // i -> SEMICNMissing
    public ASTNode parseConstDecl(int depth) {
        ASTNode constDecl = new ASTNode(GrammarSymbol.ConstDecl, null, depth);
        ASTNode child;

        // 'const'
        addCurToken(constDecl, depth);

        // BType
        child = parseBType(depth + 1);
        connect(constDecl, child);

        // ConstDef
        child = parseConstDef(depth + 1);
        connect(constDecl, child);

        // {',' ConstDef}
        while (curTokenType() == Token.TokenType.COMMA) {
            addCurToken(constDecl, depth);
            child = parseConstDef(depth + 1);
            connect(constDecl, child);
        }

        // ';'
        if (!Objects.equals(curToken().getValue(), ";")) {
            int lineNum = tokens.get(pos - 1).getLine();
            constDecl.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                    lineNum, constDecl, depth + 1));
        } else {
            addCurToken(constDecl, depth);
        }
        return constDecl;
    }

    // 基本类型 BType -> 'int'
    public ASTNode parseBType(int depth) {
        ASTNode bType = new ASTNode(GrammarSymbol.BType, null, depth);
        addCurToken(bType, depth);
        return bType;
    }

    // 常数定义 ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    // b -> IdentRedefined (Parser.ErrorVisitor)
    // k -> RBRACKMissing
    public ASTNode parseConstDef (int depth) {
        ASTNode constDef = new ASTNode(GrammarSymbol.ConstDef, null, depth);
        ASTNode child;

        addCurToken(constDef, depth);

        // { '[' ConstExp ']' }
        while (curTokenType() == Token.TokenType.LBRACK) {
            addCurToken(constDef, depth);

            child = parseConstExp(depth + 1);
            connect(constDef, child);

            if (!Objects.equals(curTokenType(), Token.TokenType.RBRACK)) {
                constDef.addChild(new ErrorNode(ErrorType.RBRACKMissing,
                        tokens.get(pos - 1).getLine(),
                        constDef, depth + 1));
            } else {
                addCurToken(constDef, depth);
            }
        }

        // '='
        addCurToken(constDef, depth);

        // ConstInitVal
        child = parseConstInitVal(depth + 1);
        connect(constDef, child);

        return constDef;
    }

    // 常量初值 ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public ASTNode parseConstInitVal(int depth) {
        ASTNode constInitVal = new ASTNode(GrammarSymbol.ConstInitVal, null, depth);
        ASTNode child;
        if (curTokenType() != Token.TokenType.LBRACE) {
            // ConstExp
            child = parseConstExp(depth + 1);
            connect(constInitVal, child);
        } else {
            // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            addCurToken(constInitVal, depth);
            if (curTokenType() != Token.TokenType.RBRACE) {
                // ConstInitVal { ',' ConstInitVal }
                child = parseConstInitVal(depth + 1);
                connect(constInitVal, child);

                while (curTokenType() == Token.TokenType.COMMA) {
                    addCurToken(constInitVal, depth);
                    child = parseConstInitVal(depth + 1);
                    connect(constInitVal, child);
                }
            }
            addCurToken(constInitVal, depth);
        }
        return constInitVal;
    }

    // 变量声明 VarDecl -> BType VarDef { ',' VarDef } ';'
    // i -> SEMICNMissing
    public ASTNode parseVarDecl(int depth) {
        ASTNode varDecl = new ASTNode(GrammarSymbol.VarDecl, null, depth);
        ASTNode child;
        // BType
        child = parseBType(depth + 1);
        connect(varDecl, child);

        // VarDef
        child = parseVarDef(depth + 1);
        connect(varDecl, child);

        // { ',' VarDef }
        while (curTokenType() == Token.TokenType.COMMA) {
            addCurToken(varDecl, depth);

            // VarDef
            child = parseVarDef(depth + 1);
            connect(varDecl, child);
        }

        // ';'
        if (curTokenType() != Token.TokenType.SEMICN) {
            varDecl.addChild(new ErrorNode(ErrorType.SEMICNMissing, tokens.get(pos - 1).getLine(),
                    varDecl, depth + 1));
        } else {
            addCurToken(varDecl, depth);
        }

        return varDecl;
    }

    // 变量定义 VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    // b -> IdentRedefined(Parser.ErrorVisitor)
    // k -> RBRACKMissing
    public ASTNode parseVarDef(int depth) {
        ASTNode varDef = new ASTNode(GrammarSymbol.VarDef, null, depth);
        ASTNode child;

        // Ident
        addCurToken(varDef, depth);

        // { '[' ConstExp ']' }
        while (curTokenType() == Token.TokenType.LBRACK) {
            addCurToken(varDef, depth);

            // ConstExp
            child = parseConstExp(depth + 1);
            connect(varDef, child);

            if (!Objects.equals(curTokenType(), Token.TokenType.RBRACK)) {
                varDef.addChild(new ErrorNode(ErrorType.RBRACKMissing, tokens.get(pos - 1).getLine(),
                        varDef, depth + 1));
            } else {
                addCurToken(varDef, depth);
            }
        }
        if (curTokenType() == Token.TokenType.ASSIGN) {
            addCurToken(varDef, depth);
            // InitVal
            child = parseInitVal(depth + 1);
            connect(varDef, child);
        }
        return varDef;
    }

    // 变量初值 InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public ASTNode parseInitVal(int depth) {
        ASTNode initVal = new ASTNode(GrammarSymbol.InitVal, null, depth);
        ASTNode child;
        if (curTokenType() != Token.TokenType.LBRACE) {
            child = parseExp(depth + 1);
            connect(initVal, child);
        } else {
            // '{'
            addCurToken(initVal, depth);
            if (curTokenType() != Token.TokenType.RBRACE) {
                child = parseInitVal(depth + 1);
                connect(initVal, child);

                while (curTokenType() == Token.TokenType.COMMA) {
                    // { ',' InitVal }
                    addCurToken(initVal, depth);
                    child = parseInitVal(depth + 1);
                    connect(initVal, child);
                }
            }
            addCurToken(initVal, depth);
        }
        return initVal;
    }

    // 函数定义 FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    // b, g -> IdentRedefined, ReturnMissing(Parser.ErrorVisitor)
    // j -> RPARENTMissing
    public ASTNode parseFuncDef(int depth) {
        ASTNode funcDef = new ASTNode(GrammarSymbol.FuncDef, null, depth);
        ASTNode child;

        // FuncType
        child = parseFuncType(depth + 1);
        connect(funcDef, child);

        // Ident
        addCurToken(funcDef, depth);

        // '('
        addCurToken(funcDef, depth);

        // [FuncFParams]
        if (curTokenType() != Token.TokenType.RPARENT &&
                curTokenType() != Token.TokenType.LBRACE) {
            child = parseFuncFParams(depth + 1);
            connect(funcDef, child);
        }

        // ')'
        if (curTokenType() != Token.TokenType.RPARENT) {
            funcDef.addChild(new ErrorNode(ErrorType.RPARENTMissing, tokens.get(pos - 1).getLine(),
                    funcDef, depth + 1));
        } else {
            addCurToken(funcDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        connect(funcDef, child);

        return funcDef;
    }

    // 主函数定义 MainFuncDef -> 'int' 'main' '(' ')' Block
    // g -> ReturnMissing(Parser.ErrorVisitor)
    // j -> RPARENTMissing
    public ASTNode parseMainFuncDef (int depth) {
        ASTNode mainFuncDef = new ASTNode(GrammarSymbol.MainFuncDef, null, depth);
        ASTNode child;

        // 'int'
        addCurToken(mainFuncDef, depth);

        // 'main'
        addCurToken(mainFuncDef, depth);

        // '('
        addCurToken(mainFuncDef, depth);

        // ')'
        if (!Objects.equals(curTokenType(), Token.TokenType.RPARENT)) {
            mainFuncDef.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                    tokens.get(pos - 1).getLine(),
                    mainFuncDef, depth + 1));
        } else {
            addCurToken(mainFuncDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        connect(mainFuncDef, child);

        return mainFuncDef;
    }

    // 函数类型 FuncType -> 'void' | 'int'
    public ASTNode parseFuncType(int depth) {
        ASTNode funcType = new ASTNode(GrammarSymbol.FuncType, null, depth);
        if (curTokenType() == Token.TokenType.VOIDTK) {
            addCurToken(funcType, depth);
        } else if (curTokenType() == Token.TokenType.INTTK) {
            addCurToken(funcType, depth);
        }

        return funcType;
    }

    // 函数形参表 FuncFParams -> FuncFParam { ',' FuncFParam }
    public ASTNode parseFuncFParams(int depth) {
        ASTNode funcFParams = new ASTNode(GrammarSymbol.FuncFParams, null, depth);
        ASTNode child;

        // FuncFParam
        child = parseFuncFParam(depth + 1);
        connect(funcFParams, child);

        // { ',' FuncFParam }
        while (curTokenType() == Token.TokenType.COMMA) {
            addCurToken(funcFParams, depth);
            child = parseFuncFParam(depth + 1);
            connect(funcFParams, child);
        }

        return funcFParams;
    }

    // 函数形参 FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    // b -> ErrorType.IdentRedefined(Parser.ErrorVisitor)
    // k -> ErrorType.RBRACKMissing
    public ASTNode parseFuncFParam(int depth) {
        ASTNode funcFParam = new ASTNode(GrammarSymbol.FuncFParam, null, depth);
        ASTNode child;

        // BType
        child = parseBType(depth + 1);
        connect(funcFParam, child);

        // Ident
        addCurToken(funcFParam, depth);

        // ['[' ']' { '[' ConstExp ']' }]
        if (pos < length && curTokenType() == Token.TokenType.LBRACK) {
            addCurToken(funcFParam, depth);

            if (curTokenType() != Token.TokenType.RBRACK) {
                funcFParam.addChild(new ErrorNode(ErrorType.RBRACKMissing,
                        tokens.get(pos - 1).getLine(), funcFParam, depth + 1));
            } else {
                addCurToken(funcFParam, depth);
            }

            while (curTokenType() == Token.TokenType.LBRACK) {
                addCurToken(funcFParam, depth);

                child = parseConstExp(depth + 1);
                connect(funcFParam, child);

                if (curTokenType() != Token.TokenType.RBRACK) {
                    funcFParam.addChild(new ErrorNode(ErrorType.RBRACKMissing,
                            tokens.get(pos - 1).getLine(), funcFParam, depth + 1));
                } else {
                    addCurToken(funcFParam, depth);
                }
            }
        }

        return funcFParam;
    }

    // 语句块 Block -> '{' { BlockItem } '}'
    public ASTNode parseBlock(int depth) {
        ASTNode block = new ASTNode(GrammarSymbol.Block, null, depth);
        ASTNode child;

        // '{'
        addCurToken(block, depth);

        // { BlockItem }
        while (curTokenType() != Token.TokenType.RBRACE) {
            child = parseBlockItem(depth + 1);
            connect(block, child);
        }

        // '}'
        addCurToken(block, depth);
        return block;
    }

    // 语句块项 BlockItem -> Decl | Stmt
    public ASTNode parseBlockItem(int depth) {
        ASTNode blockItem = new ASTNode(GrammarSymbol.BlockItem, null, depth);
        ASTNode child;
        if (curTokenType() == Token.TokenType.CONSTTK
                || curTokenType() == Token.TokenType.INTTK) {
            child = parseDecl(depth + 1);
            connect(blockItem, child);
        } else {
            child = parseStmt(depth + 1);
            connect(blockItem, child);
        }
        return blockItem;
    }

    // 语句 Stmt -> LVal '=' Exp ';'
    // | [Exp] ';'
    // | Block
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    // | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    // | 'break' ';' | 'continue' ';'
    // | 'return' [Exp] ';'
    // | LVal '=' 'getint''('')'';'
    // | 'printf''('FormatString{','Exp}')'';'
    public ASTNode parseStmt(int depth) {
        ASTNode stmt = new ASTNode(GrammarSymbol.Stmt, null, depth);
        ASTNode child;

        // LVal '=' Exp ';' | LVal '=' 'getint' '('')' ';'
        // i -> SEMICNMissing
        // j -> RPARENTMissing
        if (ifStmtHaveEqual() && curTokenType() == Token.TokenType.IDENFR) {
            child = parseLVal(depth + 1);
            connect(stmt, child);
            if (curTokenType() == Token.TokenType.ASSIGN) {
                addCurToken(stmt, depth);
                if (curTokenType() == Token.TokenType.GETINTTK) {
                    // 'getint'
                    addCurToken(stmt, depth);
                    // '('
                    addCurToken(stmt, depth);
                    // ')'
                    if (curTokenType() != Token.TokenType.RPARENT) {
                        stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                    // ';'
                    if (curTokenType() != Token.TokenType.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                } else {
                    child = parseExp(depth + 1);
                    connect(stmt, child);
                    if (curTokenType() != Token.TokenType.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                }
            }
        } else if (curTokenType() == Token.TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            // j -> RPARENTMissing

            // 'if'
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // Cond
            child = parseCond(depth + 1);
            connect(stmt, child);
            // ')'
            if (curTokenType() != Token.TokenType.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
            // Stmt
            child = parseStmt(depth + 1);
            connect(stmt, child);
            if (curTokenType() == Token.TokenType.ELSETK) {
                // 'else'
                addCurToken(stmt, depth);
                child = parseStmt(depth + 1);
                connect(stmt, child);
            }
        } else if (curTokenType() == Token.TokenType.FORTK) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // [ForStmt] ';'
            if (curTokenType() != Token.TokenType.SEMICN) {
                child = parseForStmt(depth + 1);
                connect(stmt, child);
            }
            if (curTokenType() != Token.TokenType.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));

            } else {
                addCurToken(stmt, depth);
            }

            // [Cond] ';'
            if (curTokenType() != Token.TokenType.SEMICN) {
                child = parseCond(depth + 1);
                connect(stmt, child);
            }

            if (curTokenType() != Token.TokenType.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
            // ')'
            if (curTokenType() != Token.TokenType.RPARENT) {
                child = parseForStmt(depth + 1);
                connect(stmt, child);
            }
            if (curTokenType() != Token.TokenType.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            }
            else {
                addCurToken(stmt, depth);
            }

            child = parseStmt(depth + 1);
            connect(stmt, child);
        } else if (curTokenType() == Token.TokenType.BREAKTK
                || curTokenType() == Token.TokenType.CONTINUETK) {
            // 'break' ';' | 'continue' ';'
            addCurToken(stmt, depth);
            // ';'
            if (curTokenType() != Token.TokenType.SEMICN) {
                // i -> ErrorType.SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curTokenType() == Token.TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            addCurToken(stmt, depth);
            if (curTokenType() != Token.TokenType.SEMICN &&
                    (curTokenType() == Token.TokenType.LPARENT ||
                            curTokenType() == Token.TokenType.IDENFR ||
                            curTokenType() == Token.TokenType.INTCON ||
                            curTokenType() == Token.TokenType.PLUS ||
                            curTokenType() == Token.TokenType.MINU ||
                            curTokenType() == Token.TokenType.NOT)) {
                child = parseExp(depth + 1);
                connect(stmt, child);
            }
            // i -> SEMICNMissing
            if (curTokenType() != Token.TokenType.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curTokenType() == Token.TokenType.PRINTFTK) {
            // 'printf' '(' FormatString {',' Exp} ')' ';'
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // FormatString
            addCurToken(stmt, depth);
            // {',' Exp}
            while (curTokenType() == Token.TokenType.COMMA) {
                addCurToken(stmt, depth);
                child = parseExp(depth + 1);
                connect(stmt, child);
            }

            if (curTokenType() != Token.TokenType.RPARENT) {
                // j -> RPARENTMissing
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }

            if (curTokenType() != Token.TokenType.SEMICN) {
                // i -> SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curTokenType() == Token.TokenType.LBRACE) {
            // Block
            child = parseBlock(depth + 1);
            connect(stmt, child);
        } else {
            // [Exp] ';'
            if (curTokenType() != Token.TokenType.SEMICN) {
                child = parseExp(depth + 1);
                connect(stmt, child);
            }
            // i -> SEMICNMissing
            if (curTokenType() != Token.TokenType.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        }
        return stmt;
    }

    // 语句 ForStmt -> LVal '=' Exp
    public ASTNode parseForStmt(int depth) {
        ASTNode forStmt = new ASTNode(GrammarSymbol.ForStmt, null, depth);
        ASTNode child;
        // LVal
        child = parseLVal(depth + 1);
        connect(forStmt, child);
        // '='
        if (!Objects.equals(curTokenType(), Token.TokenType.ASSIGN)) {
            throw new IllegalArgumentException();
        }
        addCurToken(forStmt, depth);
        // Exp
        child = parseExp(depth + 1);
        connect(forStmt, child);

        return forStmt;
    }

    // 表达式 Exp -> AddExp
    public ASTNode parseExp(int depth) {
        ASTNode exp = new ASTNode(GrammarSymbol.Exp, null, depth);
        ASTNode child;
        // AddExp
        child = parseAddExp(depth + 1);
        connect(exp, child);

        return exp;
    }

    // 条件表达式 Cond -> LOrExp
    public ASTNode parseCond(int depth) {
        ASTNode cond = new ASTNode(GrammarSymbol.Cond, null, depth);
        ASTNode child;

        // LOrExp
        child = parseLOrExp(depth + 1);
        connect(cond, child);

        return cond;
    }

    // 左值表达式 LVal -> Ident {'[' Exp ']'}
    // c -> IdentUnDefined(Parser.ErrorVisitor)
    // k -> RBRACKMissing
    public ASTNode parseLVal(int depth) {
        ASTNode lVal = new ASTNode(GrammarSymbol.LVal, null, depth);
        ASTNode child;
        // Ident
        if (curTokenType() != Token.TokenType.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(lVal, depth);

        while (curTokenType() == Token.TokenType.LBRACK) {
            addCurToken(lVal, depth);

            child = parseExp(depth + 1);
            connect(lVal, child);

            if (curTokenType() != Token.TokenType.RBRACK) {
                lVal.addChild(new ErrorNode(ErrorType.RBRACKMissing,
                        tokens.get(pos - 1).getLine(), lVal, depth + 1));
            } else {
                addCurToken(lVal, depth);
            }
        }

        return lVal;
    }

    // 基本表达式 PrimaryExp -> '(' Exp ')' | LVal | Number
    public ASTNode  parsePrimaryExp(int depth) {
        ASTNode primaryExp = new ASTNode(GrammarSymbol.PrimaryExp, null, depth);
        ASTNode child;
        if (curTokenType() != Token.TokenType.LPARENT) {
            if (curTokenType() == Token.TokenType.IDENFR) {
                child = parseLVal(depth + 1);
                connect(primaryExp, child);
            } else if (curTokenType() == Token.TokenType.INTCON) {
                child = parseNumber(depth + 1);
                connect(primaryExp, child);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            // '(' Exp ')'
            addCurToken(primaryExp, depth);
            child = parseExp(depth + 1);
            connect(primaryExp, child);
            if (curTokenType() != Token.TokenType.RPARENT) {
                primaryExp.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), primaryExp, depth + 1));
            }
            addCurToken(primaryExp, depth);
        }
        return primaryExp;
    }

    // 数值 Number -> IntConst
    public ASTNode parseNumber(int depth) {
        ASTNode number = new ASTNode(GrammarSymbol.Number, null, depth);
        if (curTokenType() != Token.TokenType.INTCON) {
            throw new IllegalArgumentException("");
        }
        addCurToken(number, depth);
        return number;
    }

    // 一元表达式 UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // c, d, e -> IdentUnDefined, ParaNumNotMatch, ParaTypeNotMatch(Parser.ErrorVisitor)
    // j -> ErrorType.RPARENTMissing
    public ASTNode parseUnaryExp(int depth) {
        ASTNode unaryExp = new ASTNode(GrammarSymbol.UnaryExp, null, depth);
        ASTNode child;

        if (curTokenType() == Token.TokenType.IDENFR
                && tokens.get(pos + 1).getType() == Token.TokenType.LPARENT) {
            addCurToken(unaryExp, depth);

            if (curTokenType() != Token.TokenType.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(unaryExp, depth);

            if (curTokenType() != Token.TokenType.RPARENT &&
                    curTokenType() != Token.TokenType.SEMICN) {
                child = parseFuncRParams(depth + 1);
                connect(unaryExp, child);
            }
            if (curTokenType() != Token.TokenType.RPARENT) {
                unaryExp.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), unaryExp, depth + 1));
            }
            else {
                addCurToken(unaryExp, depth);
            }
        } else if (curTokenType() == Token.TokenType.PLUS
                || curTokenType() == Token.TokenType.MINU
                || curTokenType() == Token.TokenType.NOT) {
            child = parseUnaryOp(depth + 1);
            connect(unaryExp, child);
            child = parseUnaryExp(depth + 1);
            connect(unaryExp, child);
        } else {
            child = parsePrimaryExp(depth + 1);
            connect(unaryExp, child);
        }

        return unaryExp;
    }

    // 单目运算符 UnaryOp -> '+' | '-' | '!'
    public ASTNode parseUnaryOp(int depth) {
        ASTNode unaryOp = new ASTNode(GrammarSymbol.UnaryOp, null, depth);
        if (curTokenType() != Token.TokenType.PLUS
                && curTokenType() != Token.TokenType.MINU
                && curTokenType() != Token.TokenType.NOT) {
            throw new IllegalArgumentException("");
        }
        addCurToken(unaryOp, depth);
        return unaryOp;
    }

    // 函数实参表 FuncRParams -> Exp { ',' Exp }
    public ASTNode parseFuncRParams(int depth) {
        ASTNode funcRParams = new ASTNode(GrammarSymbol.FuncRParams, null, depth);
        ASTNode child;
        child = parseExp(depth + 1);
        child.setParent(funcRParams);
        funcRParams.addChild(child);
        while (curTokenType() == Token.TokenType.COMMA) {
            addCurToken(funcRParams, depth);
            child = parseExp(depth + 1);
            connect(funcRParams, child);
        }
        return funcRParams;
    }

    private ASTNode addSelfNode(ASTNode node, GrammarSymbol symbol, int depth) {
        ASTNode temp = new ASTNode(symbol, null, depth);
        connect(temp, node);
        node.setDepth(depth + 1);
        return temp;
    }

    // 乘除模表达式 MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
    public ASTNode parseMulExp(int depth) {
        ASTNode mulExp = new ASTNode(GrammarSymbol.MulExp, null, depth);
        ASTNode child;

        child = parseUnaryExp(depth + 1);
        connect(mulExp, child);
        while (curTokenType() == Token.TokenType.MULT
                || curTokenType() == Token.TokenType.DIV
                || curTokenType() == Token.TokenType.MOD) {
            child.setDepth(depth + 2);
            mulExp = addSelfNode(mulExp, GrammarSymbol.MulExp, depth);

            addCurToken(mulExp, depth);

            child = parseUnaryExp(depth + 1);
            connect(mulExp, child);
        }
        return mulExp;
    }

    // 加减表达式 AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // AddExp -> MulExp { ('+' | '−') MulExp }
    public ASTNode parseAddExp(int depth) {
        ASTNode addExp = new ASTNode(GrammarSymbol.AddExp, null, depth);
        ASTNode child;

        child = parseMulExp(depth + 1);
        connect(addExp, child);

        while (curTokenType() == Token.TokenType.PLUS
                || curTokenType() == Token.TokenType.MINU) {
            addExp = addSelfNode(addExp, GrammarSymbol.AddExp, depth);
            child.setDepth(depth + 2);

            addCurToken(addExp, depth);

            child = parseMulExp(depth + 1);
            connect(addExp, child);
        }

        return addExp;
    }

    // 关系表达式 RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    public ASTNode parseRelExp(int depth) {
        ASTNode relExp = new ASTNode(GrammarSymbol.RelExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        connect(relExp, child);

        while (curTokenType() == Token.TokenType.LSS
                || curTokenType() == Token.TokenType.LEQ
                || curTokenType() == Token.TokenType.GRE
                || curTokenType() == Token.TokenType.GEQ) {
            relExp = addSelfNode(relExp, GrammarSymbol.RelExp, depth);
            child.setDepth(depth + 2);

            addCurToken(relExp, depth);

            child = parseAddExp(depth + 1);
            connect(relExp, child);
        }

        return relExp;
    }

    // 相等性表达式 EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    // EqExp -> RelExp { ('==' | '!=') RelExp }
    public ASTNode parseEqExp(int depth) {
        ASTNode eqExp = new ASTNode(GrammarSymbol.EqExp, null, depth);
        ASTNode child;

        child = parseRelExp(depth + 1);
        connect(eqExp, child);

        while (curTokenType() == Token.TokenType.EQL
                || curTokenType() == Token.TokenType.NEQ) {
            eqExp = addSelfNode(eqExp, GrammarSymbol.EqExp, depth);
            child.setDepth(depth + 2);

            addCurToken(eqExp, depth);

            child = parseRelExp(depth + 1);
            connect(eqExp, child);
        }

        return eqExp;
    }

    // 逻辑与表达式 LAndExp -> EqExp | LAndExp '&&' EqExp
    // LAndExp -> EqExp { '&&' EqExp }
    public ASTNode parseLAndExp(int depth) {
        ASTNode lAndExp = new ASTNode(GrammarSymbol.LAndExp, null, depth);
        ASTNode child;

        child = parseEqExp(depth + 1);
        connect(lAndExp, child);

        while (curTokenType() == Token.TokenType.AND) {
            lAndExp = addSelfNode(lAndExp, GrammarSymbol.LAndExp, depth);
            child.setDepth(depth + 2);

            addCurToken(lAndExp, depth);

            child = parseEqExp(depth + 1);
            connect(lAndExp, child);
        }

        return lAndExp;
    }

    // 逻辑或表达式 LOrExp -> LAndExp | LOrExp '||' LAndExp
    // LOrExp -> LAndExp { '||' LAndExp }
    public ASTNode parseLOrExp(int depth) {
        ASTNode lOrExp = new ASTNode(GrammarSymbol.LOrExp, null, depth);
        ASTNode child;

        child = parseLAndExp(depth + 1);
        connect(lOrExp, child);

        while (curTokenType() == Token.TokenType.OR) {
            lOrExp = addSelfNode(lOrExp, GrammarSymbol.LOrExp, depth);
            child.setDepth(depth + 2);

            addCurToken(lOrExp, depth);

            child = parseLAndExp(depth + 1);
            connect(lOrExp, child);
        }

        return lOrExp;
    }

    // 常量表达式 ConstExp -> AddExp
    public ASTNode parseConstExp(int depth) {
        ASTNode constExp = new ASTNode(GrammarSymbol.ConstExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        connect(constExp, child);

        return constExp;
    }

    private boolean ifStmtHaveEqual() {
        int i = pos;
        while (tokens.get(i).getType() != Token.TokenType.SEMICN &&
                tokens.get(i).getType() != Token.TokenType.ASSIGN &&
                tokens.get(i).getLine() == curToken().getLine()) {
            i++;
            if (i >= length) {
                return false;
            }
        }
        return tokens.get(i).getType() == Token.TokenType.ASSIGN;
    }
}
