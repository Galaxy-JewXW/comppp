package frontend.parser.expressions.unary;

import frontend.lexer.Token;
import frontend.parser.FuncRParams;
import frontend.parser.terminals.Ident;
import middle.symbols.SymbolTable;

public class UnaryExpFunc implements UnaryExpElement {
    private Ident ident;
    private FuncRParams funcRParams = null;
    private Token left;
    private Token right;
    private int dimension;

    public UnaryExpFunc(Ident ident, Token left, Token right, int dimension) {
        this.ident = ident;
        this.left = left;
        this.right = right;
        this.dimension = dimension;
    }

    public UnaryExpFunc(Ident ident, FuncRParams rParams, Token left, Token right) {
        this.ident = ident;
        this.funcRParams = rParams;
        this.left = left;
        this.right = right;
    }

    public UnaryExpFunc(Ident ident, FuncRParams rParams, Token left, Token right, int dimension) {
        this.ident = ident;
        this.funcRParams = rParams;
        this.left = left;
        this.right = right;
        this.dimension = dimension;
    }

    public String getFunctionName() {
        return ident.getName();
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    @Override
    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append(ident.print()).append(left.print());
        if (funcRParams != null) {
            sb.append(funcRParams.print());
        }
        sb.append(right.print());
        return sb.toString();
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public int calc(SymbolTable symbolTable) {
        throw new IllegalArgumentException();
    }
}
