import Lexer.Token;

import java.util.ArrayList;
import java.util.TreeMap;

public class ASTNode {
    private GrammerSymbol grammerSymbol; // 对于非终结符
    private Token token; // 对于终结符

    private ArrayList<ASTNode> children;
    private ASTNode parent;
    private int depth; // 该节点的深度
    private boolean isLeaf;// 该节点是否为叶节点

    public ASTNode(GrammerSymbol grammerSymbol, ASTNode parent, int depth) {
        this.grammerSymbol = grammerSymbol;
        this.parent = parent;
        this.depth = depth;

        this.children = new ArrayList<>();
        this.isLeaf = false;
        this.token = null;
    }

    public ASTNode(Token token, ASTNode parent, int depth) {
        this.grammerSymbol = null;
        this.parent = parent;
        this.depth = depth;

        this.children = null;
        this.isLeaf = true;
        this.token = token;
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }

    public ASTNode getParent() {
        return parent;
    }

    public ArrayList<ASTNode> getChildren() {
        return children;
    }

    public void setParent(ASTNode parent) {
        this.parent = parent;
    }

    public GrammerSymbol getGrammerSymbol() {
        return grammerSymbol;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public String toString() {
        if (isLeaf) {
            return token.toString();
        } else {
            return "<" + grammerSymbol + ">";
        }
    }

    public boolean needOutput() {
        return !(this.grammerSymbol == GrammerSymbol.BlockItem || this.grammerSymbol == GrammerSymbol.BType ||
                this.grammerSymbol == GrammerSymbol.Decl);
    }

    // remove the last child from children and return it
    public ASTNode removeLastChild() {
        ASTNode lastChild = children.get(children.size() - 1);
        children.remove(children.size() - 1);
        return lastChild;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
