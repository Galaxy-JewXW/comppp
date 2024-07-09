package frontend.parser;

import frontend.lexer.TokenListIterator;

import java.util.ArrayList;

public class Parser {
    private TokenListIterator iterator;
    private ArrayList<Decl> decls;
    private ArrayList<FuncDef> funcDefs;
    private MainFuncDef mainFuncDef;
    private SymbolTable symbolTable;
}
