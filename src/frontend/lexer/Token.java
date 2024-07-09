package frontend.lexer;

import frontend.parser.SyntaxNode;

public class Token implements SyntaxNode {
    private final TokenType type;
    private final int lineNum;
    private final String content;

    public Token(TokenType type, String content, int lineNum) {
        this.type = type;
        this.lineNum = lineNum;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public TokenType getType() {
        return type;
    }

    public int getLineNum() {
        return lineNum;
    }

    @Override
    public String print() {
        return this.getType() + " " + this.getContent() + "\n";
    }

    @Override
    public String toString() {
        return this.getContent();
    }
}
