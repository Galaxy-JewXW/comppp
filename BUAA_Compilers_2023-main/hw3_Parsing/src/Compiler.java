import Lexer.Lexer;

import java.io.*;

public class Compiler {
    public static void main(String[] args) {
        try (
                BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
                BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"));
                BufferedWriter output1 = new BufferedWriter(new FileWriter("output_word.txt"));
                // BufferedWriter  output2 = new BufferedWriter(new FileWriter("output_image.txt"))
        ) {
            Lexer lexer = new Lexer(reader, output1);
            lexer.parse();
            Parser parser = new Parser(lexer.getAllTokens());
            ASTNode root = parser.parse();
            // output the AST to output.txt
            parser.printAST(root, output);
            // parser.printASTImage(root, output2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
