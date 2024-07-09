package frontend.parser.expressions.combexp;

import frontend.lexer.Token;
import frontend.parser.SyntaxNode;
import middle.symbols.SymbolTable;
import middle.symbols.ValueNode;

import java.util.ArrayList;

public class CombExp<T extends SyntaxNode> implements SyntaxNode, ValueNode {
    private final T first;
    private final String name;
    private final ArrayList<T> operands;
    private final ArrayList<Token> operators;

    public CombExp(T first, ArrayList<T> operands, ArrayList<Token> operators, String name) {
        this.first = first;
        this.operands = operands;
        this.operators = operators;
        this.name = name;
    }

    @Override
    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append(first.print());
        sb.append(name).append("\n");
        if (operators != null && operands != null && operators.size() == operands.size()) {
            int l = operators.size();
            for (int i = 0; i < l; i++) {
                sb.append(operators.get(i).print()).append(operands.get(i).print());
                sb.append(name).append("\n");
            }
        }
        return sb.toString();
    }

    public T getFirst() {
        return first;
    }

    @Override
    public int calc(SymbolTable symbolTable) {
        return 0;
    }

    public ArrayList<T> getOperands() {
        return operands;
    }

    public ArrayList<Token> getOperators() {
        return operators;
    }

    public ArrayList<T> getAllOperands() {
        ArrayList<T> ret = new ArrayList<>();
        ret.add(this.getFirst());
        if (this.operands != null && !this.operands.isEmpty()) {
            ret.addAll(this.operands);
        }
        return ret;
    }
}
