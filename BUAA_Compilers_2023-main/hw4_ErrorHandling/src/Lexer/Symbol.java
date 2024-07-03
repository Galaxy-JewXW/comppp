package Lexer;

public enum Symbol {
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
    EMPTY // 空行
}
