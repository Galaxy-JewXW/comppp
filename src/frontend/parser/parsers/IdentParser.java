package frontend.parser.parsers;

import frontend.lexer.Token;
import frontend.lexer.TokenListIterator;
import frontend.parser.terminals.Ident;

public class IdentParser {
    private final TokenListIterator iterator;

    public IdentParser(TokenListIterator iterator) {
        this.iterator = iterator;
    }

    public Ident parseIdent() {
        Token token = iterator.next();
        return new Ident(token);
    }
}
