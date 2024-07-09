package frontend.parser.parsers;

import frontend.lexer.Token;
import frontend.lexer.TokenListIterator;
import frontend.lexer.TokenType;
import frontend.parser.expressions.unary.UnaryExp;
import frontend.parser.expressions.unary.UnaryExpElement;
import middle.symbols.SymbolTable;

public class UnaryExpParser {
    private TokenListIterator iterator;
    private UnaryExpElement element = null;
    private SymbolTable symbolTable;

    public UnaryExpParser(TokenListIterator iterator, SymbolTable symbolTable) {
        this.iterator = iterator;
        this.symbolTable = symbolTable;
    }

    public UnaryExp parseUnaryExp() {
        Token token1 = iterator.next();
        Token token2 = iterator.next();
        if (token1.getType() == TokenType.IDENFR
                && token2.getType() == TokenType.LPARENT) {
            iterator.unreadToken(2);
            element = new UnaryExpFuncParser()
        }
    }
}
