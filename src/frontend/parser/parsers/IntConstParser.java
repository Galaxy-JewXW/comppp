package frontend.parser.parsers;

import frontend.lexer.Token;
import frontend.lexer.TokenListIterator;
import frontend.parser.terminals.IntConst;

public class IntConstParser {
    private final TokenListIterator iterator;

    public IntConstParser(TokenListIterator iterator) {
        this.iterator = iterator;
    }

    public IntConst parseIntConst() {
        Token token = iterator.next();
        return new IntConst(token);
    }
}
