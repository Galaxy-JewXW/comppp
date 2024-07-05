package Parser;

import Lexer.*;

import java.io.BufferedWriter;
import java.io.IOException;
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

    private void addCurToken(ASTNode node, int depth) {
        node.addChild(new ASTNode(curToken(), node, depth + 1));
        pos++;
    }

    public ASTNode parse() {
        return parseCompUnit(0);
    }

    public void printAST(ASTNode root, BufferedWriter output) throws IOException {
        if (root.isLeaf()) {
            // System.out.println(root.toString());
            output.write(root.toString() + "\n");
            return;
        }
        for (ASTNode child : root.getChildren()) {
            printAST(child, output);
        }
        // Don't print BlockItem, BType, Decl
        if (root.getGrammarSymbol() != GrammarSymbol.BlockItem
                && root.getGrammarSymbol() != GrammarSymbol.BType
                && root.getGrammarSymbol() != GrammarSymbol.Decl) {
            // System.out.println(root.toString());
            output.write(root.toString() + "\n");
        }

        // Flush and close the BufferedWriter after writing
        output.flush();
        // output.close();
    }

    // 编译单元 CompUnit -> {Decl} {FuncDef} MainFuncDef
    public ASTNode parseCompUnit(int depth) {
        ASTNode root = new ASTNode(GrammarSymbol.CompUnit, null, depth);
        ASTNode child;
        // {Decl}
        while (tokens.get(pos + 2).getType() != Token.Type.LPARENT) {
            child = parseDecl(depth + 1);
            child.setParent(root);
            root.addChild(child);
        }
        // {FuncDef}
        while (tokens.get(pos + 1).getType() != Token.Type.MAINTK) {
            child = parseFuncDef(depth + 1);
            child.setParent(root);
            root.addChild(child);
        }
        // MainFuncDef
        child = parseMainFuncDef(depth + 1);
        child.setParent(root);
        root.addChild(child);

        return root;
    }

    // 声明 Decl -> ConstDecl | VarDecl
    public ASTNode parseDecl(int depth) {
        ASTNode decl = new ASTNode(GrammarSymbol.Decl, null, depth);
        ASTNode child;
        if (curToken().getType() == Token.Type.CONSTTK) {
            child = parseConstDecl(depth + 1);
        } else {
            child = parseVarDecl(depth + 1);
        }
        decl.addChild(child);
        child.setParent(decl);
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
        constDecl.addChild(child);
        child.setParent(constDecl);

        // ConstDef
        child = parseConstDef(depth + 1);
        constDecl.addChild(child);
        child.setParent(constDecl);

        // {',' ConstDef}
        while (curToken().getType() == Token.Type.COMMA) {
            addCurToken(constDecl, depth);
            child = parseConstDef(depth + 1);
            constDecl.addChild(child);
            child.setParent(constDecl);
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
    // b -> IdentRedefined (Visitor)
    // k -> RBRACKMissing
    public ASTNode parseConstDef (int depth) {
        ASTNode constDef = new ASTNode(GrammarSymbol.ConstDef, null, depth);
        ASTNode child;

        addCurToken(constDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(constDef, depth);

            child = parseConstExp(depth + 1);
            child.setParent(constDef);
            constDef.addChild(child);

            if (!Objects.equals(curToken().getType(), Token.Type.RBRACK)) {
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
        constDef.addChild(child);
        child.setParent(constDef);

        return constDef;
    }

    // 常量初值 ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public ASTNode parseConstInitVal(int depth) {
        ASTNode constInitVal = new ASTNode(GrammarSymbol.ConstInitVal, null, depth);
        ASTNode child;
        if (curToken().getType() != Token.Type.LBRACE) {
            // ConstExp
            child = parseConstExp(depth + 1);
            constInitVal.addChild(child);
            child.setParent(constInitVal);
        } else {
            // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            addCurToken(constInitVal, depth);
            if (curToken().getType() != Token.Type.RBRACE) {
                // ConstInitVal { ',' ConstInitVal }
                child = parseConstInitVal(depth + 1);
                constInitVal.addChild(child);
                child.setParent(constInitVal);

                while (curToken().getType() == Token.Type.COMMA) {
                    addCurToken(constInitVal, depth);
                    child = parseConstInitVal(depth + 1);
                    constInitVal.addChild(child);
                    child.setParent(constInitVal);
                }
            }
            addCurToken(constInitVal, depth);
        }
        return constInitVal;
    }

    // 变量声明 VarDecl -> BType VarDef { ',' VarDef } ';'
    // i -> SEMICNMissing
    public ASTNode parseVarDecl(int depth) {
        // BType
        ASTNode varDecl = new ASTNode(GrammarSymbol.VarDecl, null, depth);
        ASTNode child = parseBType(depth + 1);
        child.setParent(varDecl);
        varDecl.addChild(child);

        // VarDef
        child = parseVarDef(depth + 1);
        child.setParent(varDecl);
        varDecl.addChild(child);

        // { ',' VarDef }
        while (curToken().getType() == Token.Type.COMMA) {
            addCurToken(varDecl, depth);

            // VarDef
            child = parseVarDef(depth + 1);
            child.setParent(varDecl);
            varDecl.addChild(child);
        }

        // ';'
        if (curToken().getType() != Token.Type.SEMICN) {
            varDecl.addChild(new ErrorNode(ErrorType.SEMICNMissing, tokens.get(pos - 1).getLine(),
                    varDecl, depth + 1));
        } else {
            addCurToken(varDecl, depth);
        }

        return varDecl;
    }

    // 变量定义 VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    // b -> IdentRedefined(Visitor)
    // k -> RBRACKMissing
    public ASTNode parseVarDef(int depth) {
        ASTNode varDef = new ASTNode(GrammarSymbol.VarDef, null, depth);
        ASTNode child;

        // Ident
        addCurToken(varDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(varDef, depth);

            // ConstExp
            child = parseConstExp(depth + 1);
            child.setParent(varDef);
            varDef.addChild(child);

            if (!Objects.equals(curToken().getType(), Token.Type.RBRACK)) {
                varDef.addChild(new ErrorNode(ErrorType.RBRACKMissing, tokens.get(pos - 1).getLine(),
                        varDef, depth + 1));
            } else {
                addCurToken(varDef, depth);
            }
        }
        if (curToken().getType() == Token.Type.ASSIGN) {
            addCurToken(varDef, depth);
            // InitVal
            child = parseInitVal(depth + 1);
            child.setParent(varDef);
            varDef.addChild(child);
        }
        return varDef;
    }

    // 变量初值 InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public ASTNode parseInitVal(int depth) {
        ASTNode initVal = new ASTNode(GrammarSymbol.InitVal, null, depth);
        ASTNode child;
        if (curToken().getType() != Token.Type.LBRACE) {
            child = parseExp(depth + 1);
            initVal.addChild(child);
            child.setParent(initVal);
        } else {
            // '{'
            addCurToken(initVal, depth);
            if (curToken().getType() != Token.Type.RBRACE) {
                child = parseInitVal(depth + 1);
                initVal.addChild(child);
                child.setParent(initVal);

                while (curToken().getType() == Token.Type.COMMA) {
                    // { ',' InitVal }
                    addCurToken(initVal, depth);
                    child = parseInitVal(depth + 1);
                    initVal.addChild(child);
                    child.setParent(initVal);
                }
            }
            addCurToken(initVal, depth);
        }
        return initVal;
    }

    // 函数定义 FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    // b, g -> IdentRedefined, ReturnMissing(Visitor)
    // j -> RPARENTMissing
    public ASTNode parseFuncDef(int depth) {
        ASTNode funcDef = new ASTNode(GrammarSymbol.FuncDef, null, depth);
        ASTNode child;

        // FuncType
        child = parseFuncType(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        // Ident
        addCurToken(funcDef, depth);

        // '('
        addCurToken(funcDef, depth);

        // [FuncFParams]
        if (curToken().getType() != Token.Type.RPARENT &&
                curToken().getType() != Token.Type.LBRACE) {
            child = parseFuncFParams(depth + 1);
            child.setParent(funcDef);
            funcDef.addChild(child);
        }

        // ')'
        if (curToken().getType() != Token.Type.RPARENT) {
            funcDef.addChild(new ErrorNode(ErrorType.RPARENTMissing, tokens.get(pos - 1).getLine(),
                    funcDef, depth + 1));
        } else {
            addCurToken(funcDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        return funcDef;
    }

    // 主函数定义 MainFuncDef -> 'int' 'main' '(' ')' Block
    // g -> ReturnMissing(Visitor)
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
        if (!Objects.equals(curToken().getType(), Token.Type.RPARENT)) {
            mainFuncDef.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                    tokens.get(pos - 1).getLine(),
                    mainFuncDef, depth + 1));
        } else {
            addCurToken(mainFuncDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        child.setParent(mainFuncDef);
        mainFuncDef.addChild(child);

        return mainFuncDef;
    }

    // 函数类型 FuncType -> 'void' | 'int'
    public ASTNode parseFuncType(int depth) {
        ASTNode funcType = new ASTNode(GrammarSymbol.FuncType, null, depth);
        if (curToken().getType() == Token.Type.VOIDTK) {
            addCurToken(funcType, depth);
        } else if (curToken().getType() == Token.Type.INTTK) {
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
        child.setParent(funcFParams);
        funcFParams.addChild(child);

        // { ',' FuncFParam }
        while (curToken().getType() == Token.Type.COMMA) {
            addCurToken(funcFParams, depth);
            child = parseFuncFParam(depth + 1);
            child.setParent(funcFParams);
            funcFParams.addChild(child);
        }

        return funcFParams;
    }

    // 函数形参 FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    // b -> ErrorType.IdentRedefined(Visitor)
    // k -> ErrorType.RBRACKMissing
    public ASTNode parseFuncFParam(int depth) {
        ASTNode funcFParam = new ASTNode(GrammarSymbol.FuncFParam, null, depth);
        ASTNode child;

        // BType
        child = parseBType(depth + 1);
        child.setParent(funcFParam);
        funcFParam.addChild(child);

        // Ident
        addCurToken(funcFParam, depth);

        // ['[' ']' { '[' ConstExp ']' }]
        if (pos < length && curToken().getType() == Token.Type.LBRACK) {
            addCurToken(funcFParam, depth);

            if (curToken().getType() != Token.Type.RBRACK) {
                funcFParam.addChild(new ErrorNode(ErrorType.RBRACKMissing,
                        tokens.get(pos - 1).getLine(), funcFParam, depth + 1));
            } else {
                addCurToken(funcFParam, depth);
            }

            while (curToken().getType() == Token.Type.LBRACK) {
                addCurToken(funcFParam, depth);

                child = parseConstExp(depth + 1);
                child.setParent(funcFParam);
                funcFParam.addChild(child);

                if (curToken().getType() != Token.Type.RBRACK) {
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
        while (curToken().getType() != Token.Type.RBRACE) {
            child = parseBlockItem(depth + 1);
            child.setParent(block);
            block.addChild(child);
        }

        // '}'
        addCurToken(block, depth);
        return block;
    }

    // 语句块项 BlockItem -> Decl | Stmt
    public ASTNode parseBlockItem(int depth) {
        ASTNode blockItem = new ASTNode(GrammarSymbol.BlockItem, null, depth);
        ASTNode child;
        if (curToken().getType() == Token.Type.CONSTTK
                || curToken().getType() == Token.Type.INTTK) {
            child = parseDecl(depth + 1);
            child.setParent(blockItem);
            blockItem.addChild(child);
        } else {
            child = parseStmt(depth + 1);
            child.setParent(blockItem);
            blockItem.addChild(child);
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
        if (ifStmtHaveEqual() && curToken().getType() == Token.Type.IDENFR) {
            child = parseLVal(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
            if (curToken().getType() == Token.Type.ASSIGN) {
                addCurToken(stmt, depth);
                if (curToken().getType() == Token.Type.GETINTTK) {
                    // 'getint'
                    addCurToken(stmt, depth);
                    // '('
                    addCurToken(stmt, depth);
                    // ')'
                    if (curToken().getType() != Token.Type.RPARENT) {
                        stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                    // ';'
                    if (curToken().getType() != Token.Type.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                } else {
                    child = parseExp(depth + 1);
                    child.setParent(stmt);
                    stmt.addChild(child);
                    if (curToken().getType() != Token.Type.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                                tokens.get(pos - 1).getLine(), stmt, depth + 1));
                    } else {
                        addCurToken(stmt, depth);
                    }
                }
            }
        } else if (curToken().getType() == Token.Type.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            // j -> RPARENTMissing

            // 'if'
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // Cond
            child = parseCond(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
            // ')'
            if (curToken().getType() != Token.Type.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
            // Stmt
            child = parseStmt(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
            if (curToken().getType() == Token.Type.ELSETK) {
                // 'else'
                addCurToken(stmt, depth);
                child = parseStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
        } else if (curToken().getType() == Token.Type.FORTK) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // [ForStmt] ';'
            if (curToken().getType() != Token.Type.SEMICN) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            if (curToken().getType() != Token.Type.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));

            } else {
                addCurToken(stmt, depth);
            }

            // [Cond] ';'
            if (curToken().getType() != Token.Type.SEMICN) {
                child = parseCond(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }

            if (curToken().getType() != Token.Type.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
            // ')'
            if (curToken().getType() != Token.Type.RPARENT) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            if (curToken().getType() != Token.Type.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            }
            else {
                addCurToken(stmt, depth);
            }

            child = parseStmt(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
        } else if (curToken().getType() == Token.Type.BREAKTK
                || curToken().getType() == Token.Type.CONTINUETK) {
            // 'break' ';' | 'continue' ';'
            addCurToken(stmt, depth);
            // ';'
            if (curToken().getType() != Token.Type.SEMICN) {
                // i -> ErrorType.SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curToken().getType() == Token.Type.RETURNTK) {
            // 'return' [Exp] ';'
            addCurToken(stmt, depth);
            if (curToken().getType() != Token.Type.SEMICN &&
                    (curToken().getType() == Token.Type.LPARENT ||
                            curToken().getType() == Token.Type.IDENFR ||
                            curToken().getType() == Token.Type.INTCON ||
                            curToken().getType() == Token.Type.PLUS ||
                            curToken().getType() == Token.Type.MINU ||
                            curToken().getType() == Token.Type.NOT)) {
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            // i -> SEMICNMissing
            if (curToken().getType() != Token.Type.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curToken().getType() == Token.Type.PRINTFTK) {
            // 'printf' '(' FormatString {',' Exp} ')' ';'
            addCurToken(stmt, depth);
            // '('
            addCurToken(stmt, depth);
            // FormatString
            addCurToken(stmt, depth);
            // {',' Exp}
            while (curToken().getType() == Token.Type.COMMA) {
                addCurToken(stmt, depth);
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }

            if (curToken().getType() != Token.Type.RPARENT) {
                // j -> RPARENTMissing
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }

            if (curToken().getType() != Token.Type.SEMICN) {
                // i -> SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                        tokens.get(pos - 1).getLine(), stmt, depth + 1));
            } else {
                addCurToken(stmt, depth);
            }
        } else if (curToken().getType() == Token.Type.LBRACE) {
            // Block
            child = parseBlock(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
        } else {
            // [Exp] ';'
            if (curToken().getType() != Token.Type.SEMICN) {
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            // i -> SEMICNMissing
            if (curToken().getType() != Token.Type.SEMICN) {
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
        child.setParent(forStmt);
        forStmt.addChild(child);
        // '='
        if (!Objects.equals(curToken().getType(), Token.Type.ASSIGN)) {
            throw new IllegalArgumentException();
        }
        addCurToken(forStmt, depth);
        // Exp
        child = parseExp(depth + 1);
        child.setParent(forStmt);
        forStmt.addChild(child);

        return forStmt;
    }

    // 表达式 Exp -> AddExp
    public ASTNode parseExp(int depth) {
        ASTNode exp = new ASTNode(GrammarSymbol.Exp, null, depth);
        ASTNode child;
        // AddExp
        child = parseAddExp(depth + 1);
        child.setParent(exp);
        exp.addChild(child);

        return exp;
    }

    // 条件表达式 Cond -> LOrExp
    public ASTNode parseCond(int depth) {
        ASTNode cond = new ASTNode(GrammarSymbol.Cond, null, depth);
        ASTNode child;

        // LOrExp
        child = parseLOrExp(depth + 1);
        child.setParent(cond);
        cond.addChild(child);

        return cond;
    }

    // 左值表达式 LVal -> Ident {'[' Exp ']'}
    // c -> IdentUnDefined(Visitor)
    // k -> RBRACKMissing
    public ASTNode parseLVal(int depth) {
        ASTNode lVal = new ASTNode(GrammarSymbol.LVal, null, depth);
        ASTNode child;
        // Ident
        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(lVal, depth);

        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(lVal, depth);

            child = parseExp(depth + 1);
            child.setParent(lVal);
            lVal.addChild(child);

            if (curToken().getType() != Token.Type.RBRACK) {
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
        if (curToken().getType() != Token.Type.LPARENT) {
            if (curToken().getType() == Token.Type.IDENFR) {
                child = parseLVal(depth + 1);
                child.setParent(primaryExp);
                primaryExp.addChild(child);
            } else if (curToken().getType() == Token.Type.INTCON) {
                child = parseNumber(depth + 1);
                child.setParent(primaryExp);
                primaryExp.addChild(child);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            // '(' Exp ')'
            addCurToken(primaryExp, depth);
            child = parseExp(depth + 1);
            child.setParent(primaryExp);
            primaryExp.addChild(child);
            if (curToken().getType() != Token.Type.RPARENT) {
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
        if (curToken().getType() != Token.Type.INTCON) {
            throw new IllegalArgumentException("");
        }
        addCurToken(number, depth);
        return number;
    }

    // 一元表达式 UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // c, d, e -> IdentUnDefined, ParaNumNotMatch, ParaTypeNotMatch(Visitor)
    // j -> ErrorType.RPARENTMissing
    public ASTNode parseUnaryExp(int depth) {
        ASTNode unaryExp = new ASTNode(GrammarSymbol.UnaryExp, null, depth);
        ASTNode child;

        if (curToken().getType() == Token.Type.IDENFR
                && tokens.get(pos + 1).getType() == Token.Type.LPARENT) {
            addCurToken(unaryExp, depth);

            if (curToken().getType() != Token.Type.LPARENT) {
                throw new IllegalArgumentException();
            }
            addCurToken(unaryExp, depth);

            if (curToken().getType() != Token.Type.RPARENT &&
                    curToken().getType() != Token.Type.SEMICN) {
                child = parseFuncRParams(depth + 1);
                child.setParent(unaryExp);
                unaryExp.addChild(child);
            }
            if (curToken().getType() != Token.Type.RPARENT) {
                unaryExp.addChild(new ErrorNode(ErrorType.RPARENTMissing,
                        tokens.get(pos - 1).getLine(), unaryExp, depth + 1));
            }
            else {
                addCurToken(unaryExp, depth);
            }
        } else if (curToken().getType() == Token.Type.PLUS
                || curToken().getType() == Token.Type.MINU
                || curToken().getType() == Token.Type.NOT) {
            child = parseUnaryOp(depth + 1);
            child.setParent(unaryExp);
            unaryExp.addChild(child);
            child = parseUnaryExp(depth + 1);
            child.setParent(unaryExp);
            unaryExp.addChild(child);
        } else {
            child = parsePrimaryExp(depth + 1);
            child.setParent(unaryExp);
            unaryExp.addChild(child);
        }

        return unaryExp;
    }

    // 单目运算符 UnaryOp -> '+' | '-' | '!'
    public ASTNode parseUnaryOp(int depth) {
        ASTNode unaryOp = new ASTNode(GrammarSymbol.UnaryOp, null, depth);
        if (curToken().getType() != Token.Type.PLUS
                && curToken().getType() != Token.Type.MINU
                && curToken().getType() != Token.Type.NOT) {
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
        while (curToken().getType() == Token.Type.COMMA) {
            addCurToken(funcRParams, depth);
            child = parseExp(depth + 1);
            child.setParent(funcRParams);
            funcRParams.addChild(child);
        }
        return funcRParams;
    }

    // 乘除模表达式 MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
    public ASTNode parseMulExp(int depth) {
        ASTNode mulExp = new ASTNode(GrammarSymbol.MulExp, null, depth);
        ASTNode child;

        child = parseUnaryExp(depth + 1);
        child.setParent(mulExp);
        mulExp.addChild(child);
        while (curToken().getType() == Token.Type.MULT
                || curToken().getType() == Token.Type.DIV
                || curToken().getType() == Token.Type.MOD) {
            ASTNode temp = new ASTNode(GrammarSymbol.MulExp, null, depth);
            temp.addChild(mulExp);
            mulExp.setParent(temp);
            mulExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            mulExp = temp;

            addCurToken(mulExp, depth);

            child = parseUnaryExp(depth + 1);
            child.setParent(mulExp);
            mulExp.addChild(child);
        }
        return mulExp;
    }

    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // AddExp -> MulExp { ('+' | '−') MulExp }
    public ASTNode parseAddExp(int depth) {
        ASTNode addExp = new ASTNode(GrammarSymbol.AddExp, null, depth);
        ASTNode child;

        child = parseMulExp(depth + 1);
        child.setParent(addExp);
        addExp.addChild(child);

        while (curToken().getType() == Token.Type.PLUS
                || curToken().getType() == Token.Type.MINU) {
            ASTNode temp = new ASTNode(GrammarSymbol.AddExp, null, depth);
            temp.addChild(addExp);
            addExp.setParent(temp);
            addExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            addExp = temp;

            addCurToken(addExp, depth);

            child = parseMulExp(depth + 1);
            child.setParent(addExp);
            addExp.addChild(child);
        }

        return addExp;
    }

    // 关系表达式 RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    public ASTNode parseRelExp(int depth) {
        ASTNode relExp = new ASTNode(GrammarSymbol.RelExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        child.setParent(relExp);
        relExp.addChild(child);

        while (curToken().getType() == Token.Type.LSS
                || curToken().getType() == Token.Type.LEQ
                || curToken().getType() == Token.Type.GRE
                || curToken().getType() == Token.Type.GEQ) {
            ASTNode temp = new ASTNode(GrammarSymbol.RelExp, null, depth);
            temp.addChild(relExp);
            relExp.setParent(temp);
            relExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            relExp = temp;

            addCurToken(relExp, depth);

            child = parseAddExp(depth + 1);
            child.setParent(relExp);
            relExp.addChild(child);
        }

        return relExp;
    }

    // 相等性表达式 EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    // EqExp -> RelExp { ('==' | '!=') RelExp }
    public ASTNode parseEqExp(int depth) {
        ASTNode eqExp = new ASTNode(GrammarSymbol.EqExp, null, depth);
        ASTNode child;

        child = parseRelExp(depth + 1);
        child.setParent(eqExp);
        eqExp.addChild(child);

        while (curToken().getType() == Token.Type.EQL
                || curToken().getType() == Token.Type.NEQ) {
            ASTNode temp = new ASTNode(GrammarSymbol.EqExp, null, depth);
            temp.addChild(eqExp);
            eqExp.setParent(temp);
            eqExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            eqExp = temp;

            addCurToken(eqExp, depth);

            child = parseRelExp(depth + 1);
            child.setParent(eqExp);
            eqExp.addChild(child);
        }

        return eqExp;
    }

    // 逻辑与表达式 LAndExp -> EqExp | LAndExp '&&' EqExp
    // LAndExp -> EqExp { '&&' EqExp }
    public ASTNode parseLAndExp(int depth) {
        ASTNode lAndExp = new ASTNode(GrammarSymbol.LAndExp, null, depth);
        ASTNode child;

        child = parseEqExp(depth + 1);
        child.setParent(lAndExp);
        lAndExp.addChild(child);

        while (curToken().getType() == Token.Type.AND) {
            ASTNode temp = new ASTNode(GrammarSymbol.LAndExp, null, depth);
            temp.addChild(lAndExp);
            lAndExp.setParent(temp);
            lAndExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            lAndExp = temp;

            addCurToken(lAndExp, depth);

            child = parseEqExp(depth + 1);
            child.setParent(lAndExp);
            lAndExp.addChild(child);
        }

        return lAndExp;
    }

    // 逻辑或表达式 LOrExp -> LAndExp | LOrExp '||' LAndExp
    // LOrExp -> LAndExp { '||' LAndExp }
    public ASTNode parseLOrExp(int depth) {
        ASTNode lOrExp = new ASTNode(GrammarSymbol.LOrExp, null, depth);
        ASTNode child;

        child = parseLAndExp(depth + 1);
        child.setParent(lOrExp);
        lOrExp.addChild(child);

        while (curToken().getType() == Token.Type.OR) {
            ASTNode temp = new ASTNode(GrammarSymbol.LOrExp, null, depth);
            temp.addChild(lOrExp);
            lOrExp.setParent(temp);
            lOrExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            lOrExp = temp;

            addCurToken(lOrExp, depth);

            child = parseLAndExp(depth + 1);
            child.setParent(lOrExp);
            lOrExp.addChild(child);
        }

        return lOrExp;
    }

    // 常量表达式 ConstExp -> AddExp
    public ASTNode parseConstExp(int depth) {
        ASTNode constExp = new ASTNode(GrammarSymbol.ConstExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        child.setParent(constExp);
        constExp.addChild(child);

        return constExp;
    }

    private boolean ifStmtHaveEqual() {
        int i = pos;
        while (tokens.get(i).getType() != Token.Type.SEMICN &&
                tokens.get(i).getType() != Token.Type.ASSIGN &&
                tokens.get(i).getLine() == curToken().getLine()) {
            i++;
            if (i >= length) {
                return false;
            }
        }
        return tokens.get(i).getType() == Token.Type.ASSIGN;
    }
}
