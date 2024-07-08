import Lexer.Lexer;
import Parser.ASTNode;
import Parser.Parser;
import Parser.ErrorVisitor;

import java.io.*;

public class Compiler {
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
        BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"));
        BufferedWriter output2 = new BufferedWriter(new FileWriter("error.txt"));
        Lexer lexer = new Lexer(reader);
        lexer.tokenize();
        Parser parser = new Parser(lexer.getTokens());
        ASTNode root = parser.parse();
        ASTNode.printAST(root, output);
        ErrorVisitor errorVisitor = new ErrorVisitor(root);
        errorVisitor.visit();
        errorVisitor.print(output2);
    }
}
