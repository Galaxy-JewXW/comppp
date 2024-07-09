package frontend.parser.expressions.unary;

import frontend.lexer.TokenType;
import middle.symbols.SymbolTable;

public class UnaryOpExp implements UnaryExpElement {
    private final UnaryOp unaryOp;
    private final UnaryExp unaryExp;

    public UnaryOpExp(UnaryOp unaryOp, UnaryExp unaryExp) {
        this.unaryOp = unaryOp;
        this.unaryExp = unaryExp;
    }

    public UnaryOp getUnaryOp() {
        return unaryOp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    @Override
    public String print() {
        return unaryOp.print() + this.unaryExp.print();
    }

    @Override
    public int getDimension() {
        return unaryExp.getDimension();
    }

    @Override
    public int calc(SymbolTable symbolTable) {
        if (unaryOp.getToken().getType() == TokenType.PLUS) {
            return unaryExp.calc(symbolTable);
        } else if (unaryOp.getToken().getType() == TokenType.MINU) {
            return -unaryExp.calc(symbolTable);
        } else {
            return 0;
        }
    }

}
