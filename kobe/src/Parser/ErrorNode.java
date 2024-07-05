package Parser;

import java.util.HashMap;

public class ErrorNode extends ASTNode {
    private final ErrorType errorType;
    private final int lineNum;

    public ErrorNode(ErrorType errorType, int lineNum, ASTNode parent, int depth) {
        super(GrammarSymbol.Error, parent, depth);
        this.errorType = errorType;
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        return lineNum + " " + errorType;
    }
}
