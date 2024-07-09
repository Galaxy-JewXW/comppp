package frontend.parser.expressions.combexp;

import frontend.lexer.Token;
import frontend.lexer.TokenType;
import frontend.parser.expressions.unary.UnaryExp;
import middle.symbols.SymbolTable;

import java.util.ArrayList;

public class MulExp extends CombExp<UnaryExp> {
    public MulExp(UnaryExp first, ArrayList<Token> operators, ArrayList<UnaryExp> operands) {
        super(first, operands, operators, "<MulExp>");
    }
    
    public int getDimension() {
        return getFirst().getDimension();
    }
    
    @Override
    public int calc(SymbolTable symbolTable) {
        int ans = this.getFirst().calc(symbolTable);
        int l = getOperands().size();
        for (int i = 0; i < l; i++) {
            if (this.getOperators().get(i).getType() == TokenType.MULT) {
                ans = ans * this.getOperands().get(i).calc(symbolTable);
            } else if (this.getOperators().get(i).getType() == TokenType.DIV) {
                ans = ans / this.getOperands().get(i).calc(symbolTable);
            } else if (this.getOperators().get(i).getType() == TokenType.MOD) {
                ans = ans % this.getOperands().get(i).calc(symbolTable);
            }
        }
        return ans;
    }
}
