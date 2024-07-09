package frontend.parser.expressions.unary;

import frontend.parser.SyntaxNode;
import middle.symbols.ValueNode;

public interface UnaryExpElement extends SyntaxNode, ValueNode {
    int getDimension();
}
