package Lexer;

public class Token {
    public enum Type {
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

    private Type type;
    private String val;
    private int line;
    private int index;

    public Token(Type type, String val, int line, int index) {
        this.type = type;
        this.val = val;
        this.line = line;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public String getVal() {
        return val;
    }

    @Override
    public String toString() {
        return type.name() + " " + val;
    }
}
