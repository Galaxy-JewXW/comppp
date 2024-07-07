package Lexer;

public class Token {
    public enum TokenType {
        /* 保留字 */
        MAINTK, // main
        CONSTTK, // const
        INTTK, // int
        BREAKTK, // break
        CONTINUETK, // continue
        IFTK, // if
        ELSETK, // else

        FORTK, // for
        GETINTTK, // getint
        PRINTFTK, // printf
        RETURNTK, // return
        VOIDTK, // void

        /* 其他 */
        IDENFR, // Ident
        INTCON, // IntConst
        STRCON, // FormatString
        NOT, // !
        AND, // &&
        OR, // ||
        PLUS, // +
        MINU, // -
        MULT, // *
        DIV, // /
        MOD, // %
        LSS, // <
        LEQ, // <=
        GRE, // >
        GEQ, // >=
        EQL, // ==
        NEQ, // !=
        ASSIGN, // =
        SEMICN, // ;
        COMMA, // ,
        LPARENT, // (
        RPARENT, // )
        LBRACK, // [
        RBRACK, // ]
        LBRACE, // {
        RBRACE,  // }
        SINGLE_LINE_COMMENT, // 单行注释
        MULTI_LINE_COMMENT, // 多行注释
        EMPTY_LINE // 空行
    }

    private final Token.TokenType tokenType;
    private String val;
    private int line;
    private int index;

    public Token(Token.TokenType tokenType, String val, int line, int index) {
        this.tokenType = tokenType;
        this.val = val;
        this.line = line;
        this.index = index;
    }

    public Token.TokenType getType() {
        return tokenType;
    }

    public String getValue() {
        return val;
    }

    public int getIndex() {
        return index;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return tokenType.name() + " " + val;
    }
}
