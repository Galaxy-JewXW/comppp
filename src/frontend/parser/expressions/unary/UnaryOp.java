package frontend.parser.expressions.unary;

import frontend.lexer.Token;
import frontend.parser.SyntaxNode;

public class UnaryOp implements SyntaxNode {
    private final String name = "<UnaryOp>";
    private final Token token;

    public UnaryOp(Token token) {
        this.token = token;
    }

    @Override
    public String print() {
        return token.print() +
                name + "\n";
    }

    public Token getToken() {
        return token;
    }
}
