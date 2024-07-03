import Lexer.Symbol;
import Lexer.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Parser {
    private int tokenPos;
    private ArrayList<Token> allTokens;
    private int tokenLen;

    public Parser(ArrayList<Token> allTokens) {
        this.tokenLen = allTokens.size();
        this.tokenPos = 0;
        this.allTokens = allTokens;
        // this.curToken = curToken();
    }

    public void nextToken() {
        tokenPos++;
    }
    
    public Token curToken() {
        return allTokens.get(tokenPos);
    }

    private void addCurToken(ASTNode node, int depth) {
        node.addChild(new ASTNode(curToken(), node, depth + 1));
        nextToken();
    }

    // To left recursion grammar add a new self node to the parent of the last child
    // and add the last child to the new self node
    private void addSelfNode(ASTNode node, int depth) {
        ASTNode child = node.removeLastChild();
        ASTNode selfNode = new ASTNode(node.getGrammerSymbol(), node, depth + 1);
        node.addChild(selfNode);
        selfNode.addChild(child);
        child.setParent(selfNode);
        child.setDepth(depth + 2);
    }

    // Build AST
    public ASTNode parse() {
        ASTNode root = parseCompUnit(0);
        return root;
    }

    // Post-order traversal
    public void printAST(ASTNode root, BufferedWriter output) throws IOException {
        if (root.isLeaf()) {
            output.write(root.toString() + "\n");
            return;
        }
        for (ASTNode child : root.getChildren()) {
            printAST(child, output);
        }
        // Don't print BlockItem, BType, Decl
        if (root.getGrammerSymbol() != GrammerSymbol.BlockItem
                && root.getGrammerSymbol() != GrammerSymbol.BType
                && root.getGrammerSymbol() != GrammerSymbol.Decl) {
            output.write(root.toString() + "\n");
        }
    }

    // print the image of AST
    public void printASTImage(ASTNode root, BufferedWriter output) throws IOException {
        if (root.isLeaf()) {
            output.write(String.format("[label=\"%s\"]\n", root.toString()));
            return;
        }
        for (ASTNode child : root.getChildren()) {
            printASTImage(child, output);
        }
        // Don't print BlockItem, BType, Decl
        if (root.getGrammerSymbol() != GrammerSymbol.BlockItem
                && root.getGrammerSymbol() != GrammerSymbol.BType
                && root.getGrammerSymbol() != GrammerSymbol.Decl) {
            output.write(String.format("[label=\"%s\"]\n", root.toString()));
        }
        for (ASTNode child : root.getChildren()) {
            output.write(String.format("%s -> %s\n", root.toString(), child.toString()));
        }
    }



    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    public ASTNode parseCompUnit(int depth) {
        ASTNode root = new ASTNode(GrammerSymbol.CompUnit, null, depth);
        ASTNode child;
        while (allTokens.get(tokenPos + 2).getSymbol() != Symbol.LPARENT) {
            child = parseDecl(depth + 1);
            child.setParent(root);
            root.addChild(child);
        }
        while (allTokens.get(tokenPos + 1).getSymbol() != Symbol.MAINTK) {
            child = parseFuncDef(depth + 1);
            child.setParent(root);
            root.addChild(child);
        }
        child = parseMainFuncDef(depth + 1);
        child.setParent(root);
        root.addChild(child);

        return root;
    }

    // Decl -> ConstDecl | VarDecl
    public ASTNode parseDecl(int depth) {
        ASTNode decl = new ASTNode(GrammerSymbol.Decl, null, depth);
        ASTNode child;
        if (curToken().getSymbol() == Symbol.CONSTTK) {
            child = parseConstDecl(depth + 1);
        } else {
            child = parseVarDecl(depth + 1);
        }
        decl.addChild(child);
        child.setParent(decl);
        return decl;
    }

    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    public ASTNode parseConstDecl(int depth) {
        ASTNode constDecl = new ASTNode(GrammerSymbol.ConstDecl, null, depth);
        ASTNode child;

        // const
        if (curToken().getSymbol() != Symbol.CONSTTK) {
            throw new IllegalArgumentException("The first of ConstDecl is not \"const\"");
        }
        addCurToken(constDecl, depth);

        // BType
        child = parseBType(depth + 1);
        constDecl.addChild(child);
        child.setParent(constDecl);

        // ConstDef
        // next = nextToken();
        child = parseConstDef(depth + 1);
        constDecl.addChild(child);
        child.setParent(constDecl);

        // {',' ConstDef}
        // next = nextToken();
        while (curToken().getSymbol() == Symbol.COMMA) {
            addCurToken(constDecl, depth);

            child = parseConstDef(depth + 1);
            constDecl.addChild(child);
            child.setParent(constDecl);
        }

        // ';'
        if (!Objects.equals(curToken().getValue(), ";")) {
            throw new IllegalArgumentException("The end of ConstDecl is not \";\"");
        }
        addCurToken(constDecl, depth);
        return constDecl;
    }

    // BType -> 'int'
    public ASTNode parseBType(int depth) {
        ASTNode bType = new ASTNode(GrammerSymbol.BType, null, depth);
        if (curToken().getSymbol() != Symbol.INTTK) {
            throw new IllegalArgumentException(String.format("BType should be \"int\", but %s", curToken().getValue()));
        }
        addCurToken(bType, depth);
        return bType;
    }

    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    public ASTNode parseConstDef (int depth) {
        ASTNode constDef = new ASTNode(GrammerSymbol.ConstDef, null, depth);
        ASTNode child;

        // System.out.println(curToken().getValue());

        // Ident
        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
            throw new IllegalArgumentException(String.format("ConstDef should start with \"Ident\", but %s in lineNumber %d: index %d", curToken().getValue(),
                    curToken().getLineNumber(), curToken().getIndex()));
        }
        addCurToken(constDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(constDef, depth);

            child = parseConstExp(depth + 1);
            child.setParent(constDef);
            constDef.addChild(child);

            if (!Objects.equals(curToken().getSymbol(), Symbol.RBRACK)) {
                throw new IllegalArgumentException(String.format("ConstDef lack \"]\", but %s", curToken().getValue()));
            }
            addCurToken(constDef, depth);
        }

        // '='
        if (!Objects.equals(curToken().getSymbol(), Symbol.ASSIGN)) {
            throw new IllegalArgumentException(String.format("ConstDef lack \"=\", but %s", curToken().getValue()));
        }
        addCurToken(constDef, depth);

        // ConstInitVal
        child = parseConstInitVal(depth + 1);
        constDef.addChild(child);
        child.setParent(constDef);

        return constDef;
    }

    // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public ASTNode parseConstInitVal(int depth) {
        ASTNode constInitVal = new ASTNode(GrammerSymbol.ConstInitVal, null, depth);
        ASTNode child;
        if (curToken().getSymbol() != Symbol.LBRACE) { // ConstExp
            child = parseConstExp(depth + 1);
            constInitVal.addChild(child);
            child.setParent(constInitVal);
        } else { // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            addCurToken(constInitVal, depth);

            if (curToken().getSymbol() != Symbol.RBRACE) {// ConstInitVal { ',' ConstInitVal }
                child = parseConstInitVal(depth + 1);
                constInitVal.addChild(child);
                child.setParent(constInitVal);

                while (curToken().getSymbol() == Symbol.COMMA) {
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

    // VarDecl -> BType VarDef { ',' VarDef } ';'
    public ASTNode parseVarDecl(int depth) {
        // BType
        ASTNode varDecl = new ASTNode(GrammerSymbol.VarDecl, null, depth);
        ASTNode child = parseBType(depth + 1);
        child.setParent(varDecl);
        varDecl.addChild(child);

        // VarDef
        child = parseVarDef(depth + 1);
        child.setParent(varDecl);
        varDecl.addChild(child);

        // { ',' VarDef }
        Token next;
        while (curToken().getSymbol() == Symbol.COMMA) {
            addCurToken(varDecl, depth);

            // VarDef
            child = parseVarDef(depth + 1);
            child.setParent(varDecl);
            varDecl.addChild(child);
        }

        // ';'
        if (curToken().getSymbol() != Symbol.SEMICN) {
            throw new IllegalArgumentException("VarDecl lack \";\"");
        }
        addCurToken(varDecl, depth);

        return varDecl;
    }

    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    public ASTNode parseVarDef(int depth) {
        ASTNode varDef = new ASTNode(GrammerSymbol.VarDef, null, depth);
        ASTNode child;

        // Ident
        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
            throw new IllegalArgumentException("VarDef should start with \"Ident\"");
        }
        addCurToken(varDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(varDef, depth);

            // ConstExp
            child = parseConstExp(depth + 1);
            child.setParent(varDef);
            varDef.addChild(child);

            if (!Objects.equals(curToken().getSymbol(), Symbol.RBRACK)) {
                throw new IllegalArgumentException("VarDef lack \"]\"");
            }
            addCurToken(varDef, depth);
        }

        if (curToken().getSymbol() == Symbol.ASSIGN) {
           addCurToken(varDef, depth);

            // InitVal
            child = parseInitVal(depth + 1);
            child.setParent(varDef);
            varDef.addChild(child);
        }

        return varDef;
    }


    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public ASTNode parseInitVal(int depth) {
        ASTNode initVal = new ASTNode(GrammerSymbol.InitVal, null, depth);
        ASTNode child;
        Token next;
        if (curToken().getSymbol() != Symbol.LBRACE) {
            child = parseExp(depth + 1);
            initVal.addChild(child);
            child.setParent(initVal);
        } else {
            // '{'
            addCurToken(initVal, depth);

            if (curToken().getSymbol() != Symbol.RBRACE) {
                child = parseInitVal(depth + 1);
                initVal.addChild(child);
                child.setParent(initVal);

                while (curToken().getSymbol() == Symbol.COMMA) {
                    // ','
                    addCurToken(initVal, depth);

                    child = parseInitVal(depth + 1);
                    initVal.addChild(child);
                    child.setParent(initVal);
                }
            }

            // '}' 不在这里做错误处理
            addCurToken(initVal, depth);
        }

        return initVal;
    }

    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    public ASTNode parseFuncDef(int depth) {
        ASTNode funcDef = new ASTNode(GrammerSymbol.FuncDef, null, depth);
        ASTNode child;

        // FuncType
        child = parseFuncType(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        // Ident
        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
            throw new IllegalArgumentException("FuncDef should start with \"Ident\"");
        }
        addCurToken(funcDef, depth);

        // '('
        if (!Objects.equals(curToken().getSymbol(), Symbol.LPARENT)) {
            throw new IllegalArgumentException("FuncDef lack \"(\"");
        }
        addCurToken(funcDef, depth);

        // [FuncFParams]
        if (curToken().getSymbol() != Symbol.RPARENT) {
            child = parseFuncFParams(depth + 1);
            child.setParent(funcDef);
            funcDef.addChild(child);
        }

        // ')'
        addCurToken(funcDef, depth);

        // Block
        child = parseBlock(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        return funcDef;
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    public ASTNode parseMainFuncDef (int depth) {
        ASTNode mainFuncDef = new ASTNode(GrammerSymbol.MainFuncDef, null, depth);
        ASTNode child;
        Token next;

        // 'int'
        if (!Objects.equals(curToken().getSymbol(), Symbol.INTTK)) {
            throw new IllegalArgumentException("MainFuncDef should start with \"int\"");
        }
        addCurToken(mainFuncDef, depth);

        // 'main'
        if (!Objects.equals(curToken().getSymbol(), Symbol.MAINTK)) {
            throw new IllegalArgumentException("MainFuncDef should include \"main\"");
        }
        addCurToken(mainFuncDef, depth);

        // '('
        if (!Objects.equals(curToken().getSymbol(), Symbol.LPARENT)) {
            throw new IllegalArgumentException("MainFuncDef lack \"(\"");
        }
        addCurToken(mainFuncDef, depth);

        // ')'
        if (!Objects.equals(curToken().getSymbol(), Symbol.RPARENT)) {
            throw new IllegalArgumentException("MainFuncDef lack \")\"");
        }
        addCurToken(mainFuncDef, depth);

        // Block
        child = parseBlock(depth + 1);
        child.setParent(mainFuncDef);
        mainFuncDef.addChild(child);

        return mainFuncDef;
    }

    // FuncType -> 'void' | 'int'
    public ASTNode parseFuncType(int depth) {
        ASTNode funcType = new ASTNode(GrammerSymbol.FuncType, null, depth);
        ASTNode child;
        if (curToken().getSymbol() == Symbol.VOIDTK) {
            addCurToken(funcType, depth);
        } else if (curToken().getSymbol() == Symbol.INTTK) {
            addCurToken(funcType, depth);
        } else {
            throw new IllegalArgumentException("FuncType should be \"void\" or \"int\"");
        }

        return funcType;
    }

    // FuncFParams -> FuncFParam { ',' FuncFParam }
    public ASTNode parseFuncFParams(int depth) {
        ASTNode funcFParams = new ASTNode(GrammerSymbol.FuncFParams, null, depth);
        ASTNode child;

        // FuncFParam
        child = parseFuncFParam(depth + 1);
        child.setParent(funcFParams);
        funcFParams.addChild(child);

        // { ',' FuncFParam }
        while (curToken().getSymbol() == Symbol.COMMA) {
            addCurToken(funcFParams, depth);

            child = parseFuncFParam(depth + 1);
            child.setParent(funcFParams);
            funcFParams.addChild(child);
        }

        return funcFParams;
    }

    // FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    public ASTNode parseFuncFParam(int depth) {
        ASTNode funcFParam = new ASTNode(GrammerSymbol.FuncFParam, null, depth);
        ASTNode child;

        // BType
        child = parseBType(depth + 1);
        child.setParent(funcFParam);
        funcFParam.addChild(child);

        // Ident
        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
            throw new IllegalArgumentException("FuncFParam should include \"Ident\"");
        }
        addCurToken(funcFParam, depth);

        // ['[' ']' { '[' ConstExp ']' }]
        if (tokenPos < tokenLen && curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(funcFParam, depth);

            if (curToken().getSymbol() != Symbol.RBRACK) {
                throw new IllegalArgumentException("FuncFParam lack \"]\"");
            }
            addCurToken(funcFParam, depth);

            while (curToken().getSymbol() == Symbol.LBRACK) {
                addCurToken(funcFParam, depth);

                child = parseConstExp(depth + 1);
                child.setParent(funcFParam);
                funcFParam.addChild(child);

                if (curToken().getSymbol() != Symbol.RBRACK) {
                    throw new IllegalArgumentException("FuncFParam lack \"]\"");
                }
                addCurToken(funcFParam, depth);
            }
        }

        return funcFParam;
    }

    // Block -> '{' { BlockItem } '}'
    public ASTNode parseBlock(int depth) {
        ASTNode block = new ASTNode(GrammerSymbol.Block, null, depth);
        ASTNode child;

        // '{'
        if (!Objects.equals(curToken().getSymbol(), Symbol.LBRACE)) {
            throw new IllegalArgumentException("Block should start with \"{\"");
        }
        addCurToken(block, depth);

        // { BlockItem }
        while (curToken().getSymbol() != Symbol.RBRACE) {
            child = parseBlockItem(depth + 1);
            child.setParent(block);
            block.addChild(child);
        }

        // '}'
        addCurToken(block, depth);

        return block;
    }

    // BlockItem -> Decl | Stmt
    public ASTNode parseBlockItem(int depth) {
        ASTNode blockItem = new ASTNode(GrammerSymbol.BlockItem, null, depth);
        ASTNode child;
        if (curToken().getSymbol() == Symbol.CONSTTK || curToken().getSymbol() == Symbol.INTTK) {
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

    // Stmt -> LVal '=' Exp ';'
    // | LVal '=' 'getint' '('')' ';'
    // | [Exp] ';'
    // | Block
    // | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    // | 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    // | 'break' ';'
    // | 'continue' ';'
    // | 'return' [Exp] ';'
    // | 'printf' '(' FormatString {',' Exp} ')' ';'
    public ASTNode parseStmt(int depth) {
        ASTNode stmt = new ASTNode(GrammerSymbol.Stmt, null, depth);
        ASTNode child;
        Token next;

        // LVal '=' Exp ';' | LVal '=' 'getint' '('')' ';'
        if (!semiBeforeEqual() && curToken().getSymbol() == Symbol.IDENFR) {
            child = parseLVal(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);

            if (curToken().getSymbol() == Symbol.ASSIGN) {
                addCurToken(stmt, depth);

                if (curToken().getSymbol() == Symbol.GETINTTK) {
                    addCurToken(stmt, depth);

                    if (curToken().getSymbol() != Symbol.LPARENT) {
                        throw new IllegalArgumentException("Stmt lack \"(\"");
                    }
                    addCurToken(stmt, depth);

                    if (curToken().getSymbol() != Symbol.RPARENT) {
                        throw new IllegalArgumentException("Stmt lack \")\"");
                    }
                    addCurToken(stmt, depth);

                    if (curToken().getSymbol() != Symbol.SEMICN) {
                        throw new IllegalArgumentException("Stmt lack \";\"");
                    }
                    addCurToken(stmt, depth);

                } else {
                    child = parseExp(depth + 1);
                    child.setParent(stmt);
                    stmt.addChild(child);

                    if (curToken().getSymbol() != Symbol.SEMICN) {
                        throw new IllegalArgumentException("Stmt lack \";\"");
                    }
                   addCurToken(stmt, depth);
                }
            } else {
                throw new IllegalArgumentException(String.format("Stmt lack \"=\", but %s at lineNum %d: index %d",
                        curToken().getValue(), curToken().getLineNumber(), curToken().getIndex()));
            }
        } else if (curToken().getSymbol() == Symbol.IFTK) { // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.LPARENT) {
                throw new IllegalArgumentException("Stmt lack \"(\"");
            }
            addCurToken(stmt, depth);

            child = parseCond(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);

            if (curToken().getSymbol() != Symbol.RPARENT) {
                throw new IllegalArgumentException("Stmt lack \")\"");
            }
            addCurToken(stmt, depth);

            child = parseStmt(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);

            if (curToken().getSymbol() == Symbol.ELSETK) {
                addCurToken(stmt, depth);

                child = parseStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
        } else if (curToken().getSymbol() == Symbol.FORTK) { // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.LPARENT) {
                throw new IllegalArgumentException("Stmt lack \"(\"");
            }
            addCurToken(stmt, depth);
            // ';'
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            addCurToken(stmt, depth);
            // ';'
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseCond(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            addCurToken(stmt, depth);
            // ')'
            if (curToken().getSymbol() != Symbol.RPARENT) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            addCurToken(stmt, depth);

            child = parseStmt(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
        } else if (curToken().getSymbol() == Symbol.BREAKTK
                || curToken().getSymbol() == Symbol.CONTINUETK) { // 'break' ';' | 'continue' ';'
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.SEMICN) {
                throw new IllegalArgumentException("Stmt lack \";\"");
            }
            addCurToken(stmt, depth);
        } else if (curToken().getSymbol() == Symbol.RETURNTK) { // 'return' [Exp] ';'
            addCurToken(stmt, depth);
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            addCurToken(stmt, depth);
        } else if (curToken().getSymbol() == Symbol.PRINTFTK) { // 'printf' '(' FormatString {',' Exp} ')' ';'
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.LPARENT) {
                throw new IllegalArgumentException("Stmt lack \"(\"");
            }
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.STRCON) {
                throw new IllegalArgumentException("Stmt lack \"FormatString\"");
            }
            addCurToken(stmt, depth);

            while (curToken().getSymbol() == Symbol.COMMA) {
                addCurToken(stmt, depth);

                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }

            if (curToken().getSymbol() != Symbol.RPARENT) {
                throw new IllegalArgumentException("Stmt lack \")\"");
            }
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.SEMICN) {
                throw new IllegalArgumentException("Stmt lack \";\"");
            }
            addCurToken(stmt, depth);
        } else if (curToken().getSymbol() == Symbol.LBRACE) { // Block
            child = parseBlock(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
        } else { // [Exp] ';'
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            addCurToken(stmt, depth);
        }
        return stmt;
    }

    // ForStmt -> LVal '=' Exp
    public ASTNode parseForStmt(int depth) {
        ASTNode forStmt = new ASTNode(GrammerSymbol.ForStmt, null, depth);
        ASTNode child;

        // LVal
        child = parseLVal(depth + 1);
        child.setParent(forStmt);
        forStmt.addChild(child);

        // '='
        if (!Objects.equals(curToken().getSymbol(), Symbol.ASSIGN)) {
            throw new IllegalArgumentException("ForStmt lack \"=\"");
        }
        addCurToken(forStmt, depth);

        // Exp
        child = parseExp(depth + 1);
        child.setParent(forStmt);
        forStmt.addChild(child);

        return forStmt;
    }

    // Exp -> AddExp
    public ASTNode parseExp(int depth) {
        ASTNode exp = new ASTNode(GrammerSymbol.Exp, null, depth);
        ASTNode child;

        // AddExp
        child = parseAddExp(depth + 1);
        child.setParent(exp);
        exp.addChild(child);

        return exp;
    }

    // Cond → LOrExp
    public ASTNode parseCond(int depth) {
        ASTNode cond = new ASTNode(GrammerSymbol.Cond, null, depth);
        ASTNode child;

        // LOrExp
        child = parseLOrExp(depth + 1);
        child.setParent(cond);
        cond.addChild(child);

        return cond;
    }

    // LVal -> Ident {'[' Exp ']'}
    public ASTNode parseLVal(int depth) {
        ASTNode lVal = new ASTNode(GrammerSymbol.LVal, null, depth);
        ASTNode child;
        if (curToken().getSymbol() != Symbol.IDENFR) {
            throw new IllegalArgumentException(String.format("LVal should start with \"Ident\", but %s at lineNum %d: index %d",
                    curToken().getValue(), curToken().getLineNumber(), curToken().getIndex()));
        }
        addCurToken(lVal, depth);

        while (curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(lVal, depth);

            child = parseExp(depth + 1);
            child.setParent(lVal);
            lVal.addChild(child);

            if (curToken().getSymbol() != Symbol.RBRACK) {
                throw new IllegalArgumentException("LVal lack \"]\"");
            }
            addCurToken(lVal, depth);
        }

        return lVal;
    }

    // PrimaryExp -> '(' Exp ')' | LVal | Number
    public ASTNode  parsePrimaryExp(int depth) {
        ASTNode primaryExp = new ASTNode(GrammerSymbol.PrimaryExp, null, depth);
        ASTNode child;

        if (curToken().getSymbol() != Symbol.LPARENT) {
            if (curToken().getSymbol() == Symbol.IDENFR) {
                child = parseLVal(depth + 1);
                child.setParent(primaryExp);
                primaryExp.addChild(child);
            } else if (curToken().getSymbol() == Symbol.INTCON) {
                child = parseNumber(depth + 1);
                child.setParent(primaryExp);
                primaryExp.addChild(child);
            } else {
                throw new IllegalArgumentException("PrimaryExp should start with \"(\" or \"Ident\" or \"Number\"");
            }
        }
        else {
            addCurToken(primaryExp, depth);

            child = parseExp(depth + 1);
            child.setParent(primaryExp);
            primaryExp.addChild(child);

            if (curToken().getSymbol() != Symbol.RPARENT) {
                throw new IllegalArgumentException("PrimaryExp lack \")\"");
            }
            addCurToken(primaryExp, depth);
        }
        return primaryExp;
    }

    // Number -> IntConst
    public ASTNode parseNumber(int depth) {
        ASTNode number = new ASTNode(GrammerSymbol.Number, null, depth);
        ASTNode child;

        if (curToken().getSymbol() != Symbol.INTCON) {
            throw new IllegalArgumentException("Number should start with \"IntConst\"");
        }
        addCurToken(number, depth);

        return number;
    }

    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    public ASTNode parseUnaryExp(int depth) {
        ASTNode unaryExp = new ASTNode(GrammerSymbol.UnaryExp, null, depth);
        ASTNode child;

        if (curToken().getSymbol() == Symbol.IDENFR && allTokens.get(tokenPos + 1).getSymbol() == Symbol.LPARENT) {
            addCurToken(unaryExp, depth);

            if (curToken().getSymbol() != Symbol.LPARENT) {
                throw new IllegalArgumentException("UnaryExp lack \"(\"");
            }
            addCurToken(unaryExp, depth);

            if (curToken().getSymbol() != Symbol.RPARENT) {
                child = parseFuncRParams(depth + 1);
                child.setParent(unaryExp);
                unaryExp.addChild(child);
            }
            addCurToken(unaryExp, depth);
        } else if (curToken().getSymbol() == Symbol.PLUS
                || curToken().getSymbol() == Symbol.MINU
                || curToken().getSymbol() == Symbol.NOT) {
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

    // UnaryOp -> '+' | '-' | '!'
    public ASTNode parseUnaryOp(int depth) {
        ASTNode unaryOp = new ASTNode(GrammerSymbol.UnaryOp, null, depth);
        ASTNode child;

        if (curToken().getSymbol() != Symbol.PLUS
                && curToken().getSymbol() != Symbol.MINU
                && curToken().getSymbol() != Symbol.NOT) {
            throw new IllegalArgumentException("UnaryOp should be \"+\" or \"-\" or \"!\"");
        }
        addCurToken(unaryOp, depth);

        return unaryOp;
    }

    // FuncRParams -> Exp { ',' Exp }
    public ASTNode parseFuncRParams(int depth) {
        ASTNode funcRParams = new ASTNode(GrammerSymbol.FuncRParams, null, depth);
        ASTNode child;

        child = parseExp(depth + 1);
        child.setParent(funcRParams);
        funcRParams.addChild(child);

        while (curToken().getSymbol() == Symbol.COMMA) {
            addCurToken(funcRParams, depth);

            child = parseExp(depth + 1);
            child.setParent(funcRParams);
            funcRParams.addChild(child);
        }

        return funcRParams;
    }

    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // Change to : MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
    public ASTNode parseMulExp(int depth) {
        ASTNode mulExp = new ASTNode(GrammerSymbol.MulExp, null, depth);
        ASTNode child;

        child = parseUnaryExp(depth + 1);
        child.setParent(mulExp);
        mulExp.addChild(child);

        while (curToken().getSymbol() == Symbol.MULT
                || curToken().getSymbol() == Symbol.DIV
                || curToken().getSymbol() == Symbol.MOD) {
            addSelfNode(mulExp, depth);
            addCurToken(mulExp, depth);

            child = parseUnaryExp(depth + 1);
            child.setParent(mulExp);
            mulExp.addChild(child);
        }

        return mulExp;
    }

    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    // Change to: AddExp -> MulExp { ('+' | '−') MulExp }
    // But Warning: don't change the structure of original AST
    public ASTNode parseAddExp(int depth) {
        ASTNode addExp = new ASTNode(GrammerSymbol.AddExp, null, depth);
        ASTNode child;

        child = parseMulExp(depth + 1);
        child.setParent(addExp);
        addExp.addChild(child);

        while (curToken().getSymbol() == Symbol.PLUS
                || curToken().getSymbol() == Symbol.MINU) {
            addSelfNode(addExp, depth);
            addCurToken(addExp, depth);

            child = parseMulExp(depth + 1);
            child.setParent(addExp);
            addExp.addChild(child);
        }

        return addExp;
    }

    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // Change to: RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    public ASTNode parseRelExp(int depth) {
        ASTNode relExp = new ASTNode(GrammerSymbol.RelExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        child.setParent(relExp);
        relExp.addChild(child);

        while (curToken().getSymbol() == Symbol.LSS
                || curToken().getSymbol() == Symbol.LEQ
                || curToken().getSymbol() == Symbol.GRE
                || curToken().getSymbol() == Symbol.GEQ) {
            addSelfNode(relExp, depth);
            addCurToken(relExp, depth);

            child = parseAddExp(depth + 1);
            child.setParent(relExp);
            relExp.addChild(child);
        }

        return relExp;
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    // Change to: EqExp → RelExp { ('==' | '!=') RelExp }
    public ASTNode parseEqExp(int depth) {
        ASTNode eqExp = new ASTNode(GrammerSymbol.EqExp, null, depth);
        ASTNode child;

        child = parseRelExp(depth + 1);
        child.setParent(eqExp);
        eqExp.addChild(child);

        while (curToken().getSymbol() == Symbol.EQL
                || curToken().getSymbol() == Symbol.NEQ) {
            addSelfNode(eqExp, depth);
            addCurToken(eqExp, depth);

            child = parseRelExp(depth + 1);
            child.setParent(eqExp);
            eqExp.addChild(child);
        }

        return eqExp;
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    // Change to: LAndExp → EqExp { '&&' EqExp }
    public ASTNode parseLAndExp(int depth) {
        ASTNode lAndExp = new ASTNode(GrammerSymbol.LAndExp, null, depth);
        ASTNode child;

        child = parseEqExp(depth + 1);
        child.setParent(lAndExp);
        lAndExp.addChild(child);

        while (curToken().getSymbol() == Symbol.AND) {
            addSelfNode(lAndExp, depth);
            addCurToken(lAndExp, depth);

            child = parseEqExp(depth + 1);
            child.setParent(lAndExp);
            lAndExp.addChild(child);
        }

        return lAndExp;
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    // Change to: LOrExp → LAndExp { '||' LAndExp }
    public ASTNode parseLOrExp(int depth) {
        ASTNode lOrExp = new ASTNode(GrammerSymbol.LOrExp, null, depth);
        ASTNode child;

        child = parseLAndExp(depth + 1);
        child.setParent(lOrExp);
        lOrExp.addChild(child);

        while (curToken().getSymbol() == Symbol.OR) {
            addSelfNode(lOrExp, depth);
            addCurToken(lOrExp, depth);

            child = parseLAndExp(depth + 1);
            child.setParent(lOrExp);
            lOrExp.addChild(child);
        }

        return lOrExp;
    }

    // ConstExp -> AddExp
    public ASTNode parseConstExp(int depth) {
        ASTNode constExp = new ASTNode(GrammerSymbol.ConstExp, null, depth);
        ASTNode child;

        child = parseAddExp(depth + 1);
        child.setParent(constExp);
        constExp.addChild(child);

        return constExp;
    }

    // Judge [Exp] ';' or LVal '=' Exp ';' in parseStmt
    // ';' or '=' show first in parseStmt
    private boolean semiBeforeEqual() {
        int i = tokenPos;
        while (i < tokenLen) {
            if (allTokens.get(i).getSymbol() == Symbol.SEMICN) {
                return true;
            } else if (allTokens.get(i).getSymbol() == Symbol.ASSIGN) {
                return false;
            }
            i++;
        }
        return false;
    }
}
