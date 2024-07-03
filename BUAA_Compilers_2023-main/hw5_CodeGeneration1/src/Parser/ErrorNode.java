package Parser;

import Lexer.Token;
import java.util.HashMap;

public class ErrorNode extends ASTNode {
    private ErrorType errorType;
    private int lineNum;

    public ErrorNode(ErrorType errorType, int lineNum, ASTNode parent, int depth) {
        super(GrammerSymbol.Error, parent, depth);
        this.errorType = errorType;
        this.lineNum = lineNum;
    }

    public String toString(HashMap<ErrorType, String> errorType2Symbol) {
        return lineNum + " " + errorType2Symbol.get(errorType);
    }
}
