package frontend.parser.expressions.unary;

import frontend.parser.SyntaxNode;
import middle.symbols.SymbolTable;
import middle.symbols.ValueNode;

public class UnaryExp implements SyntaxNode, ValueNode {
    private final String name = "<UnaryExp>";
    private final UnaryExpElement unaryExpElement;

    public UnaryExp(UnaryExpElement unaryExpElement) {
        this.unaryExpElement = unaryExpElement;
    }

    @Override
    public String print() {
        return this.unaryExpElement.print() +
                this.name + "\n";
    }

    public int getDimension() {
        return this.unaryExpElement.getDimension();
    }

    @Override
    public int calc(SymbolTable symbolTable) {
        return this.unaryExpElement.calc(symbolTable);
    }

    public UnaryExpElement getUnaryExpElement() {
        return unaryExpElement;
    }

}
