package Lexer;

import Lexer.Symbol;

public class Token {
    private Symbol symbol;
    private String value;
    int lineNumber;
    private int index;

    public Token(Symbol symbol, String value, int l, int index) {
        this.symbol = symbol;
        this.value = value;
        this.lineNumber = l;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Symbol getSymbol() {
        return this.symbol;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return symbol + " " + value;
    }
}
