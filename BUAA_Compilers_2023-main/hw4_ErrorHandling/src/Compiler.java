import Lexer.Lexer;
import Parser.ASTNode;
import Parser.Parser;

import java.io.*;
import java.util.HashMap;
import Parser.ErrorType;

public class Compiler {
    private static final HashMap<ErrorType, String> errorType2Symbol = new HashMap<>();

    private static void initMap() {
        errorType2Symbol.put(ErrorType.IllegalChar, "a");
        errorType2Symbol.put(ErrorType.IdentRedefined, "b");
        errorType2Symbol.put(ErrorType.IdentUndefined, "c");
        errorType2Symbol.put(ErrorType.ParaNumNotMatch, "d");
        errorType2Symbol.put(ErrorType.ParaTypeNotMatch, "e");
        errorType2Symbol.put(ErrorType.ReturnTypeError, "f");
        errorType2Symbol.put(ErrorType.ReturnMissing, "g");
        errorType2Symbol.put(ErrorType.ConstAssign, "h");
        errorType2Symbol.put(ErrorType.SEMICNMissing, "i"); // ; missing
        errorType2Symbol.put(ErrorType.RPARENTMissing, "j"); // ) missing
        errorType2Symbol.put(ErrorType.RBRACKMissing, "k"); // ] missing
        errorType2Symbol.put(ErrorType.PrintfFormatStrNumNotMatch, "l");
        errorType2Symbol.put(ErrorType.BreakContinueNotInLoop, "m");
    }

    public static void main(String[] args) {
        try (
                BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
                BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"));
                BufferedWriter output1 = new BufferedWriter(new FileWriter("output_word.txt"));
                BufferedWriter output2 = new BufferedWriter(new FileWriter("error.txt"));
                // BufferedWriter  output2 = new BufferedWriter(new FileWriter("output_image.txt"))
        ) {
            Lexer lexer = new Lexer(reader, output1);
            lexer.parse();
            Parser parser = new Parser(lexer.getAllTokens(), false);
            ASTNode root = parser.parse();
            // output the AST to output.txt
            parser.printAST(root, output);
            // parser.printASTImage(root, output2);

            // System.out.println("Parser finished.");
            // mistake process
            Compiler.initMap();
            Visitor visitor = new Visitor(root, true, errorType2Symbol);
            visitor.visitCompUnit(root);
            visitor.printToFile(output2);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
