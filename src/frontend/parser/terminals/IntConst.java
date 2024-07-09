package frontend.parser.terminals;

import frontend.lexer.Token;
import frontend.parser.SyntaxNode;

public class IntConst implements SyntaxNode {
    private final Token token;

    public IntConst(Token token) {
        this.token = token;
    }

    public int getNum() {
        return Integer.parseInt(this.token.getContent());
    }

    public int getLineNum() {
        return this.token.getLineNum();
    }

    @Override
    public String print() {
        return token.print();
    }
}
