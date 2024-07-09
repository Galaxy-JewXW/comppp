package frontend.lexer;

import frontend.parser.SyntaxNode;

public class Token implements SyntaxNode {
    private TokenType type;
    private int lineNum;
    private String content;

    public Token(TokenType type, String content, int lineNum) {
        this.type = type;
        this.lineNum = lineNum;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public int getLineNum() {
        return lineNum;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
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
