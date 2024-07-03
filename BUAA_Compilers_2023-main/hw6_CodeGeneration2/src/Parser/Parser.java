package Parser;

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
    private boolean debug;

    public Parser(ArrayList<Token> allTokens, boolean debug) {
        this.tokenLen = allTokens.size();
        this.tokenPos = 0;
        this.allTokens = allTokens;
        this.debug = debug;
        // this.curToken = curToken();
    }

    public void nextToken() {
        tokenPos++;
    }

    public Token curToken() {
        // if (debug) {
        //     System.out.println("curToken called with tokenPos: " + tokenPos);
        // }
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
            // System.out.println(root.toString());
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
            // System.out.println(root.toString());
            output.write(root.toString() + "\n");
        }

        // Flush and close the BufferedWriter after writing
        output.flush();
        // output.close();
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
    // i => Parser.ErrorType: SEMICNMissing
    public ASTNode parseConstDecl(int depth) {
        ASTNode constDecl = new ASTNode(GrammerSymbol.ConstDecl, null, depth);
        ASTNode child;

        // const
//        if (curToken().getSymbol() != Symbol.CONSTTK) {
//            throw new IllegalArgumentException("The first of ConstDecl is not \"const\"");
//        }
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
        if (!Objects.equals(curToken().getValue(), ";")) { // error: i => Parser.ErrorType: SEMICNMissing
            int lineNum = allTokens.get(tokenPos - 1).getLineNumber();
            constDecl.addChild(new ErrorNode(ErrorType.SEMICNMissing,
                    lineNum, constDecl, depth + 1));
            // nextToken();
            // throw new IllegalArgumentException("The end of ConstDecl is not \";\"");
        } else {
            addCurToken(constDecl, depth);
        }
        return constDecl;
    }

    // BType -> 'int'
    public ASTNode parseBType(int depth) {
        ASTNode bType = new ASTNode(GrammerSymbol.BType, null, depth);
//        if (curToken().getSymbol() != Symbol.INTTK) {
//            throw new IllegalArgumentException(String.format("BType should be \"int\", but %s", curToken().getValue()));
//        }
        addCurToken(bType, depth);
        return bType;
    }

    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    // b => Parser.ErrorType: IdentRedefined (Visitor processed)
    // k => Parser.ErrorType: RBRACKMissing
    public ASTNode parseConstDef (int depth) {
        ASTNode constDef = new ASTNode(GrammerSymbol.ConstDef, null, depth);
        ASTNode child;

        // System.out.println(curToken().getValue());

        // Ident
//        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
//            throw new IllegalArgumentException(String.format("ConstDef should start with \"Ident\", but %s in lineNumber %d: index %d", curToken().getValue(),
//                    curToken().getLineNumber(), curToken().getIndex()));
//        }
        addCurToken(constDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(constDef, depth);

            child = parseConstExp(depth + 1);
            child.setParent(constDef);
            constDef.addChild(child);

            if (!Objects.equals(curToken().getSymbol(), Symbol.RBRACK)) { // k => Parser.ErrorType: RBRACKMissing
                constDef.addChild(new ErrorNode(ErrorType.RBRACKMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        constDef, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException(String.format("ConstDef lack \"]\", but %s", curToken().getValue()));
            }
            else {
                addCurToken(constDef, depth);
            }
        }

        // '='
//        if (!Objects.equals(curToken().getSymbol(), Symbol.ASSIGN)) {
//            throw new IllegalArgumentException(String.format("ConstDef lack \"=\", but %s", curToken().getValue()));
//        }
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
    // i => ErrorType.SEMICNMissing
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
        if (curToken().getSymbol() != Symbol.SEMICN) { // i => ErrorType.SEMICNMissing
            varDecl.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                    varDecl, depth + 1));
            // nextToken();
            // throw new IllegalArgumentException("VarDecl lack \";\"");
        }
        else {
            addCurToken(varDecl, depth);
        }

        return varDecl;
    }

    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    // b => ErrorType.IdentRedefined(Visitor processed)
    // k => ErrorType.RBRACKMissing
    public ASTNode parseVarDef(int depth) {
        ASTNode varDef = new ASTNode(GrammerSymbol.VarDef, null, depth);
        ASTNode child;

        // Ident
//        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
//            throw new IllegalArgumentException("VarDef should start with \"Ident\"");
//        }
        addCurToken(varDef, depth);

        // { '[' ConstExp ']' }
        while (curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(varDef, depth);

            // ConstExp
            child = parseConstExp(depth + 1);
            child.setParent(varDef);
            varDef.addChild(child);

            if (!Objects.equals(curToken().getSymbol(), Symbol.RBRACK)) { // k => ErrorType.RBRACKMissing
                varDef.addChild(new ErrorNode(ErrorType.RBRACKMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        varDef, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("VarDef lack \"]\"");
            }
            else {
                addCurToken(varDef, depth);
            }
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

    // FuncDef -> EntryType.FuncType Ident '(' [FuncFParams] ')' Block
    // b, g => ErrorType.IdentRedefined, ErrorType.ReturnMissing(Visitor processed)
    // j => ErrorType.RPARENTMissing
    public ASTNode parseFuncDef(int depth) {
        ASTNode funcDef = new ASTNode(GrammerSymbol.FuncDef, null, depth);
        ASTNode child;

        // EntryType.FuncType
        child = parseFuncType(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        // Ident
//        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
//            throw new IllegalArgumentException("FuncDef should start with \"Ident\"");
//        }
        addCurToken(funcDef, depth);

        // '('
//        if (!Objects.equals(curToken().getSymbol(), Symbol.LPARENT)) {
//            throw new IllegalArgumentException("FuncDef lack \"(\"");
//        }
        addCurToken(funcDef, depth);

        // [FuncFParams]
        if (curToken().getSymbol() != Symbol.RPARENT &&
        curToken().getSymbol() != Symbol.LBRACE) {
            // Mistake Processing: 缺少右括号，要求紧接着左括号不能为{,出现void f1({的情况
            child = parseFuncFParams(depth + 1);
            child.setParent(funcDef);
            funcDef.addChild(child);
        }

        // ')'
        if (curToken().getSymbol() != Symbol.RPARENT) {
            funcDef.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                    funcDef, depth + 1));
            // nextToken();
        }
        else {
            addCurToken(funcDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        child.setParent(funcDef);
        funcDef.addChild(child);

        return funcDef;
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    // g => ErrorType.ReturnMissing(Visitor processed)
    // j => ErrorType.RPARENTMissing
    public ASTNode parseMainFuncDef (int depth) {
        ASTNode mainFuncDef = new ASTNode(GrammerSymbol.MainFuncDef, null, depth);
        ASTNode child;
        Token next;

        // 'int'
//        if (!Objects.equals(curToken().getSymbol(), Symbol.INTTK)) {
//            throw new IllegalArgumentException("MainFuncDef should start with \"int\"");
//        }
        addCurToken(mainFuncDef, depth);

        // 'main'
//        if (!Objects.equals(curToken().getSymbol(), Symbol.MAINTK)) {
//            throw new IllegalArgumentException("MainFuncDef should include \"main\"");
//        }
        addCurToken(mainFuncDef, depth);

        // '('
//        if (!Objects.equals(curToken().getSymbol(), Symbol.LPARENT)) {
//            throw new IllegalArgumentException("MainFuncDef lack \"(\"");
//        }
        addCurToken(mainFuncDef, depth);

        // ')'
        if (!Objects.equals(curToken().getSymbol(), Symbol.RPARENT)) {
            mainFuncDef.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                    mainFuncDef, depth + 1));
            // nextToken();
            // throw new IllegalArgumentException("MainFuncDef lack \")\"");
        }
        else {
            addCurToken(mainFuncDef, depth);
        }

        // Block
        child = parseBlock(depth + 1);
        child.setParent(mainFuncDef);
        mainFuncDef.addChild(child);

        return mainFuncDef;
    }

    // EntryType.FuncType -> 'void' | 'int'
    public ASTNode parseFuncType(int depth) {
        ASTNode funcType = new ASTNode(GrammerSymbol.FuncType, null, depth);
        ASTNode child;
        if (curToken().getSymbol() == Symbol.VOIDTK) {
            addCurToken(funcType, depth);
        } else if (curToken().getSymbol() == Symbol.INTTK) {
            addCurToken(funcType, depth);
        }
//        else {
//            throw new IllegalArgumentException("EntryType.FuncType should be \"void\" or \"int\"");
//        }

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
    // b => ErrorType.IdentRedefined(Visitor processed)
    // k => ErrorType.RBRACKMissing
    public ASTNode parseFuncFParam(int depth) {
        ASTNode funcFParam = new ASTNode(GrammerSymbol.FuncFParam, null, depth);
        ASTNode child;

        // BType
        child = parseBType(depth + 1);
        child.setParent(funcFParam);
        funcFParam.addChild(child);

        // Ident
//        if (!Objects.equals(curToken().getSymbol(), Symbol.IDENFR)) {
//            throw new IllegalArgumentException("FuncFParam should include \"Ident\"");
//        }
        addCurToken(funcFParam, depth);

        // ['[' ']' { '[' ConstExp ']' }]
        if (tokenPos < tokenLen && curToken().getSymbol() == Symbol.LBRACK) {
            addCurToken(funcFParam, depth);

            if (curToken().getSymbol() != Symbol.RBRACK) {
                funcFParam.addChild(new ErrorNode(ErrorType.RBRACKMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        funcFParam, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("FuncFParam lack \"]\"");
            }
            else {
                addCurToken(funcFParam, depth);
            }

            while (curToken().getSymbol() == Symbol.LBRACK) {
                addCurToken(funcFParam, depth);

                child = parseConstExp(depth + 1);
                child.setParent(funcFParam);
                funcFParam.addChild(child);

                if (curToken().getSymbol() != Symbol.RBRACK) {
                    funcFParam.addChild(new ErrorNode(ErrorType.RBRACKMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                            funcFParam, depth + 1));
                    // nextToken();
                    // throw new IllegalArgumentException("FuncFParam lack \"]\"");
                }
                else {
                    addCurToken(funcFParam, depth);
                }
            }
        }

        return funcFParam;
    }

    // Block -> '{' { BlockItem } '}'
    public ASTNode parseBlock(int depth) {
        ASTNode block = new ASTNode(GrammerSymbol.Block, null, depth);
        ASTNode child;
        if (debug) {
            System.out.println("Enter Block: " + curToken().getValue());
        }

        // '{'
//        if (!Objects.equals(curToken().getSymbol(), Symbol.LBRACE)) {
//            throw new IllegalArgumentException("Block should start with \"{\"");
//        }
        addCurToken(block, depth);
        if (debug) {
            System.out.println("Enter Block After {: " + curToken().getValue() + " " +
                    curToken().getSymbol());
            System.out.println("tokenPos is " + tokenPos);
            System.out.println("allTokens size is " + allTokens.size());

        }

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
        if (debug) {
            System.out.println("Enter BlockItem: " + curToken().getValue());
        }
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
        // i => ErrorType.SEMICNMissing
        // j => ErrorType.RPARENTMissing
        if (ifStmtHaveEqual() && curToken().getSymbol() == Symbol.IDENFR) {
            child = parseLVal(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);

            if (curToken().getSymbol() == Symbol.ASSIGN) {
                addCurToken(stmt, depth);

                if (curToken().getSymbol() == Symbol.GETINTTK) {
                    addCurToken(stmt, depth);

//                    if (curToken().getSymbol() != Symbol.LPARENT) {
//                        throw new IllegalArgumentException("Stmt lack \"(\"");
//                    }
                    addCurToken(stmt, depth);

                    if (curToken().getSymbol() != Symbol.RPARENT) {
                        stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                                stmt, depth + 1));
                        // nextToken();
                        // throw new IllegalArgumentException("Stmt lack \")\"");
                    }
                    else {
                        addCurToken(stmt, depth);
                    }

                    if (curToken().getSymbol() != Symbol.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                                stmt, depth + 1));
                        // nextToken();
                        // throw new IllegalArgumentException("Stmt lack \";\"");
                    }
                    else {
                        addCurToken(stmt, depth);
                    }

                } else {
                    child = parseExp(depth + 1);
                    child.setParent(stmt);
                    stmt.addChild(child);

                    if (curToken().getSymbol() != Symbol.SEMICN) {
                        stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                                stmt, depth + 1));
                        // nextToken();
                        // throw new IllegalArgumentException("Stmt lack \";\"");
                    }
                    else {
                        addCurToken(stmt, depth);
                    }
                }
            }
//            else {
//                throw new IllegalArgumentException(String.format("Stmt lack \"=\", but %s at lineNum %d: index %d",
//                        curToken().getValue(), curToken().getLineNumber(), curToken().getIndex()));
//            }
        } else if (curToken().getSymbol() == Symbol.IFTK) { // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            // j => ErrorType.RPARENTMissing
            addCurToken(stmt, depth);

//            if (curToken().getSymbol() != Symbol.LPARENT) {
//                throw new IllegalArgumentException("Stmt lack \"(\"");
//            }

            addCurToken(stmt, depth);


            child = parseCond(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);

            if (curToken().getSymbol() != Symbol.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \")\"");
            }
            else {
                addCurToken(stmt, depth);
            }

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

//            if (curToken().getSymbol() != Symbol.LPARENT) {
//                throw new IllegalArgumentException("Stmt lack \"(\"");
//            }
            addCurToken(stmt, depth);
            // ';'
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            if (curToken().getSymbol() != Symbol.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }

            // addCurToken(stmt, depth);
            // ';'
            if (curToken().getSymbol() != Symbol.SEMICN) {
                child = parseCond(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            if (curToken().getSymbol() != Symbol.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }
            // addCurToken(stmt, depth);
            // ')'
            if (curToken().getSymbol() != Symbol.RPARENT) {
                child = parseForStmt(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            if (curToken().getSymbol() != Symbol.RPARENT) {
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \")\"");
            }
            else {
                addCurToken(stmt, depth);
            }
            // addCurToken(stmt, depth);

            child = parseStmt(depth + 1);
            child.setParent(stmt);
            stmt.addChild(child);
        } else if (curToken().getSymbol() == Symbol.BREAKTK
                || curToken().getSymbol() == Symbol.CONTINUETK) { // 'break' ';' | 'continue' ';'
            addCurToken(stmt, depth);

            if (curToken().getSymbol() != Symbol.SEMICN) { // i => ErrorType.SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }
        } else if (curToken().getSymbol() == Symbol.RETURNTK) { // 'return' [Exp] ';'
            addCurToken(stmt, depth);
            if (curToken().getSymbol() != Symbol.SEMICN&&
                    (curToken().getSymbol() == Symbol.LPARENT ||
                            curToken().getSymbol() == Symbol.IDENFR ||
                            curToken().getSymbol() == Symbol.INTCON ||
                            curToken().getSymbol() == Symbol.PLUS ||
                            curToken().getSymbol() == Symbol.MINU ||
                            curToken().getSymbol() == Symbol.NOT)) {
                // Warning: mistake process: i : SEMICNMissing -> need to judge whether or not Exp
                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }
            // mistake processing: i => ErrorType.SEMICNMissing
            if (curToken().getSymbol() != Symbol.SEMICN) {
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }
            // addCurToken(stmt, depth);
        } else if (curToken().getSymbol() == Symbol.PRINTFTK) { // 'printf' '(' FormatString {',' Exp} ')' ';'
            addCurToken(stmt, depth);

//            if (curToken().getSymbol() != Symbol.LPARENT) {
//                throw new IllegalArgumentException("Stmt lack \"(\"");
//            }
            addCurToken(stmt, depth);

//            if (curToken().getSymbol() != Symbol.STRCON) {
//                throw new IllegalArgumentException("Stmt lack \"FormatString\"");
//            }
            addCurToken(stmt, depth);

            while (curToken().getSymbol() == Symbol.COMMA) {
                addCurToken(stmt, depth);

                child = parseExp(depth + 1);
                child.setParent(stmt);
                stmt.addChild(child);
            }

            if (curToken().getSymbol() != Symbol.RPARENT) { // j => ErrorType.RPARENTMissing
                stmt.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \")\"");
            }
            else {
                addCurToken(stmt, depth);
            }

            if (curToken().getSymbol() != Symbol.SEMICN) { // i => ErrorType.SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }
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

            if (curToken().getSymbol() != Symbol.SEMICN) { // i => ErrorType.SEMICNMissing
                stmt.addChild(new ErrorNode(ErrorType.SEMICNMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        stmt, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("Stmt lack \";\"");
            }
            else {
                addCurToken(stmt, depth);
            }
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
    // c => ErrorType.IdentUnDefined(Visitor processed)
    // k => ErrorType.RBRACKMissing
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
                lVal.addChild(new ErrorNode(ErrorType.RBRACKMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        lVal, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("LVal lack \"]\"");
            }
            else {
                addCurToken(lVal, depth);
            }
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
                primaryExp.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        primaryExp, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("PrimaryExp lack \")\"");
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
    // c, d, e => ErrorType.IdentUnDefined, ParaNumNotMatch, ParaTypeNotMatch(Visitor processed)
    // j => ErrorType.RPARENTMissing
    public ASTNode parseUnaryExp(int depth) {
        ASTNode unaryExp = new ASTNode(GrammerSymbol.UnaryExp, null, depth);
        ASTNode child;

        if (curToken().getSymbol() == Symbol.IDENFR && allTokens.get(tokenPos + 1).getSymbol() == Symbol.LPARENT) {
            addCurToken(unaryExp, depth);

            if (curToken().getSymbol() != Symbol.LPARENT) {
                throw new IllegalArgumentException("UnaryExp lack \"(\"");
            }
            addCurToken(unaryExp, depth);

            if (curToken().getSymbol() != Symbol.RPARENT &&
            curToken().getSymbol() != Symbol.SEMICN) {
                // 错误处理中函数调用 f2(; 的情况
                child = parseFuncRParams(depth + 1);
                child.setParent(unaryExp);
                unaryExp.addChild(child);
            }
            if (curToken().getSymbol() != Symbol.RPARENT) {
                unaryExp.addChild(new ErrorNode(ErrorType.RPARENTMissing, allTokens.get(tokenPos - 1).getLineNumber(),
                        unaryExp, depth + 1));
                // nextToken();
                // throw new IllegalArgumentException("UnaryExp lack \")\"");
            }
            else {
                addCurToken(unaryExp, depth);
            }
            // addCurToken(unaryExp, depth);
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
            // addSelfNode(mulExp, depth);
            ASTNode temp = new ASTNode(GrammerSymbol.MulExp, null, depth);
            temp.addChild(mulExp);
            mulExp.setParent(temp);
            mulExp.setDepth(depth + 1);
            child.setDepth(depth + 2);
            mulExp = temp;

            addCurToken(mulExp, depth);
            // addCurToken(mulExp, depth + 1);

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
            // addSelfNode(addExp, depth);
            // depth 有误
            ASTNode temp = new ASTNode(GrammerSymbol.AddExp, null, depth);
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
            // addSelfNode(relExp, depth);
            ASTNode temp = new ASTNode(GrammerSymbol.RelExp, null, depth);
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
            // addSelfNode(eqExp, depth);
            ASTNode temp = new ASTNode(GrammerSymbol.EqExp, null, depth);
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

    // LAndExp → EqExp | LAndExp '&&' EqExp
    // Change to: LAndExp → EqExp { '&&' EqExp }
    public ASTNode parseLAndExp(int depth) {
        ASTNode lAndExp = new ASTNode(GrammerSymbol.LAndExp, null, depth);
        ASTNode child;

        child = parseEqExp(depth + 1);
        child.setParent(lAndExp);
        lAndExp.addChild(child);

        while (curToken().getSymbol() == Symbol.AND) {
            // addSelfNode(lAndExp, depth);
            ASTNode temp = new ASTNode(GrammerSymbol.LAndExp, null, depth);
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

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    // Change to: LOrExp → LAndExp { '||' LAndExp }
    public ASTNode parseLOrExp(int depth) {
        ASTNode lOrExp = new ASTNode(GrammerSymbol.LOrExp, null, depth);
        ASTNode child;

        child = parseLAndExp(depth + 1);
        child.setParent(lOrExp);
        lOrExp.addChild(child);

        while (curToken().getSymbol() == Symbol.OR) {
            // addSelfNode(lOrExp, depth);
            ASTNode temp = new ASTNode(GrammerSymbol.LOrExp, null, depth);
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
    // Use '=' to judge 当前行是否有 '='
    private boolean ifStmtHaveEqual() {
        int i = tokenPos;
        while (allTokens.get(i).getSymbol() != Symbol.SEMICN &&
               allTokens.get(i).getSymbol() != Symbol.ASSIGN &&
               allTokens.get(i).getLineNumber() == curToken().getLineNumber()) {
            i++;
            if (i >= tokenLen) {
                return false;
            }
        }
        return allTokens.get(i).getSymbol() == Symbol.ASSIGN;
    }
}
