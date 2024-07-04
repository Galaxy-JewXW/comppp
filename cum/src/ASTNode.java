import Lexer.Token;

import java.util.ArrayList;

public class ASTNode {
    public enum GrammarSymbol {
        BlockItem,
        BType,
        Decl,

        CompUnit,
        FuncDef,
        MainFuncDef,
        ConstDecl,
        VarDecl,
        ConstDef,
        Ident,
        ConstExp,
        ConstInitVal,
        VarDef,
        InitVal,
        Exp,
        FuncType,
        FuncFParams,
        FuncFParam,
        Block,
        Stmt,
        LVal,
        Cond,
        ForStmt,
        forStmt,
        FormatString,
        AddExp,
        LOrExp,
        PrimaryExp,
        UnaryExp,
        MulExp,
        RelExp,
        EqExp,
        LAndExp,
        Number,
        UnaryOp,
        FuncRParams,
    }

    private GrammarSymbol symbol;
    private Token token;
    private ArrayList<ASTNode> children;
    private ASTNode parent;
    private int depth;
    private boolean isLeaf;

    public ASTNode(GrammarSymbol symbol, ASTNode parent, int depth) {
        this.symbol = symbol;
        this.token = null;
        this.depth = depth;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.isLeaf = false;
    }

    public ASTNode(Token token, ASTNode parent, int depth) {
        this.symbol = null;
        this.token = token;
        this.depth = depth;
        this.parent = parent;
        this.children = null;
        this.isLeaf = true;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public ASTNode removeLastChild() {
        ASTNode node = children.get(children.size() - 1);
        children.remove(children.size() - 1);
        return node;
    }

    public GrammarSymbol getSymbol() {
        return symbol;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void addChild(ASTNode node) {
        children.add(node);
    }

    public void setParent(ASTNode parent) {
        this.parent = parent;
        parent.addChild(this);
    }

    public ArrayList<ASTNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        if (isLeaf) {
            return token.toString();
        } else {
            return "<" + symbol.name() + ">";
        }
    }

    public static void printTree(ASTNode root) {
        if (root.isLeaf()) {
            System.out.println(root);
            return;
        }
        for (ASTNode child : root.getChildren()) {
            printTree(child);
        }
        if (root.getSymbol() != GrammarSymbol.BlockItem && root.getSymbol() != GrammarSymbol.BType
                && root.getSymbol() != GrammarSymbol.Decl) {
            System.out.println(root);
        }
    }
}
