package frontend.parser.parsers;

import frontend.lexer.Token;
import frontend.lexer.TokenListIterator;
import frontend.parser.FuncRParams;
import frontend.parser.expressions.unary.UnaryExpFunc;
import frontend.parser.terminals.Ident;
import middle.symbols.SymbolTable;

public class UnaryExpFuncParser {
    private TokenListIterator iterator;
    private Ident ident = null;
    private FuncRParams funcRParams = null;
    private Token left;
    private Token right;
    private UnaryExpFunc unaryExpFunc = null;
    private SymbolTable SymbolTable;
    private int dimension;

    public UnaryExpFuncParser(TokenListIterator iterator, SymbolTable symbolTable) {
        this.iterator = iterator;
        this.SymbolTable = symbolTable;
    }
}
