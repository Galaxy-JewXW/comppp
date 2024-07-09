package frontend.parser.parsers;

import frontend.lexer.Token;
import frontend.lexer.TokenListIterator;
import frontend.lexer.TokenType;
import frontend.parser.expressions.unary.UnaryOp;

public class UnaryOpParser {
    private final TokenListIterator iterator;

    public UnaryOpParser(TokenListIterator iterator) {
        this.iterator = iterator;
    }

    public UnaryOp parseUnaryOp() {
        Token token = iterator.next();
        if (token.getType().equals(TokenType.PLUS) ||
                token.getType().equals(TokenType.MINU) ||
                token.getType().equals(TokenType.NOT)) {
            return new UnaryOp(token);
        }
        throw new IllegalArgumentException();
    }
}
