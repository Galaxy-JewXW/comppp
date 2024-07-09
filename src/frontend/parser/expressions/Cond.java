package frontend.parser.expressions;

import frontend.parser.SyntaxNode;

public class Cond implements SyntaxNode {
    private final String name = "<Cond>";
    private LOrExp lorExp;

    public Cond(LOrExp lorExp) {
        this.lorExp = lorExp;
    }

    @Override
    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append(lorExp.print());
        sb.append(name).append("\n");
        return sb.toString();
    }

    public LOrExp getLorExp() {
        return lorExp;
    }
}
