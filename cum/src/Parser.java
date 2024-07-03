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
        node.addChildren(new ASTNode(curToken(), node, depth + 1));
        next();
    }

    public ASTNode parse() {
        return parseCompUnit(0);
    }

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

    private ASTNode parseBType(int depth) {
        ASTNode bType = new ASTNode(ASTNode.GrammarSymbol.BType, null, depth);
        if (curToken().getType() == Token.Type.INTTK) {
            addCurToken(bType, depth);
            return bType;
        }
        throw new IllegalArgumentException();
    }

    private ASTNode parseConstDef(int depth) {
        ASTNode constDef = new ASTNode(ASTNode.GrammarSymbol.ConstDef, null, depth);
        ASTNode child;

        if (curToken().getType() != Token.Type.IDENFR) {
            throw new IllegalArgumentException();
        }
        addCurToken(constDef, depth);

        while (curToken().getType() == Token.Type.LBRACK) {
            addCurToken(constDef, depth);
        }
    }
}
