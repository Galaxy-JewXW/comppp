package frontend.parser.terminals;

import frontend.lexer.Token;
import frontend.parser.SyntaxNode;

public class Ident implements SyntaxNode {
    private final Token token;

    public Ident(Token token) {
        this.token = token;
    }

    public String getName() {
        return token.getContent();
    }

    public int getLineNum() {
        return token.getLineNum();
    }

    @Override
    public String print() {
        return token.print();
    }

    @Override
    public String toString() {
        return token.getContent();
    }
}
