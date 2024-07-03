import Lexer.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
        System.setOut(new PrintStream("output.txt"));
        Lexer lexer = new Lexer(reader);
        lexer.parse();
        Parser parser = new Parser(lexer.getTokens());
    }
}
