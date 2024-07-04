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
        if (curToken().getType() == Token.Type.CONSTTK) {
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
        if (curToken().getType() != Token.Type.CONSTTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDecl, depth);

        child = parseBType(depth + 1);
        child.setParent(constDecl);

        child = parseConstDef(depth + 1);
        child.setParent(constDecl);

        while (curToken().getType() == Token.Type.COMMA) {
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
        if (curToken().getType() == Token.Type.INTTK) {
            addCurToken(bType, depth);
            return bType;
        }
        throw new IllegalArgumentException();
    }

    // 常数定义 ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    private ASTNode parseConstDef(int depth) {
        ASTNode constDef = new ASTNode(ASTNode.GrammarSymbol.ConstDef, null, depth);
        ASTNode child;

        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDef, depth);

        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(constDef, depth);
            child = parseConstExp(depth + 1);
            child.setParent(constDef);
            if (curToken().getType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(constDef, depth);
        }

        if (curToken().getType() != Token.Type.ASSIGN) {
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
        if (curToken().getType() != Token.Type.LBRACE) {
            child = parseConstExp(depth + 1);
            child.setParent(constInitVal);
        } else {
            addCurToken(constInitVal, depth);
            if (curToken().getType() != Token.Type.RBRACE) {
                child = parseConstInitVal(depth + 1);
                child.setParent(constInitVal);
                while (curToken().getType() == Token.Type.COMMA) {
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

        while (curToken().getType() == Token.Type.COMMA) {
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
        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(varDef, depth);
        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(varDef, depth);
            child = parseConstExp(depth + 1);
            child.setParent(varDef);
            if (curToken().getType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(varDef, depth);
        }
        if (curToken().getType() == Token.Type.ASSIGN) {
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
        if (curToken().getType() == Token.Type.LBRACE) {
            addCurToken(initVal, depth);
            if (curToken().getType() != Token.Type.RBRACE) {
                child = parseInitVal(depth + 1);
                child.setParent(initVal);
                while (curToken().getType() == Token.Type.COMMA) {
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

        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcDef, depth);

        if (curToken().getType() != Token.Type.LPARENT) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcDef, depth);

        if (curToken().getType() != Token.Type.RPARENT) {
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

        if (curToken().getType() != Token.Type.INTTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curToken().getType() != Token.Type.MAINTK) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curToken().getType() != Token.Type.LPARENT) {
            throw new IllegalArgumentException();
        }
        addCurToken(mainFuncDef, depth);

        if (curToken().getType() != Token.Type.RPARENT) {
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
        if (curToken().getType() == Token.Type.VOIDTK || curToken().getType() == Token.Type.INTTK) {
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

        while (curToken().getType() == Token.Type.COMMA) {
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
        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(funcFParam, depth);
        if (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(funcFParam, depth);
            if (curToken().getType() != Token.Type.RBRACK) {
                throw new IllegalArgumentException();
            }
            addCurToken(funcFParam, depth);
            while (curToken().getType() == Token.Type.LBRACK) {
                addCurToken(funcFParam, depth);
                child = parseConstExp(depth + 1);
                child.setParent(funcFParam);
                if (curToken().getType() != Token.Type.RBRACK) {
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

        if (curToken().getType() != Token.Type.LBRACE) {
            throw new IllegalArgumentException();
        }
        addCurToken(block, depth);
    }
}
