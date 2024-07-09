package frontend.parser.parsers;

import frontend.lexer.TokenListIterator;
import frontend.parser.expressions.unary.UnaryExp;
import frontend.parser.expressions.unary.UnaryOp;
import frontend.parser.expressions.unary.UnaryOpExp;
import middle.symbols.SymbolTable;

public class UnaryOpExpParser {
    private TokenListIterator iterator;
    private UnaryOp unaryOp;
    private UnaryExp unaryExp;
    private SymbolTable symbolTable;

    public UnaryOpExpParser(TokenListIterator iterator, SymbolTable symbolTable) {
        this.iterator = iterator;
        this.symbolTable = symbolTable;
    }

    public UnaryOpExp parseUnaryOpExp() {
        unaryOp = new UnaryOpParser(iterator).parseUnaryOp();
        unaryExp = new UnaryExpParser(iterator, symbolTable).parseUnaryExp();
        return new UnaryOpExp(unaryOp, unaryExp);
    }
}
