package frontend.parser.terminals;

import frontend.lexer.Token;
import frontend.parser.SyntaxNode;

public class FormatString implements SyntaxNode {
    private final Token token;

    public FormatString(Token token) {
        this.token = token;
    }

    @Override
    public String print() {
        return token.print();
    }

    public String getContent() {
        return token.getContent();
    }

    public int getLineNum() {
        return token.getLineNum();
    }

    public Token getToken() {
        return token;
    }
}
