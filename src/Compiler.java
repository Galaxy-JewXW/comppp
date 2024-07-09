import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
        BufferedWriter lexWriter = new BufferedWriter(new FileWriter("output.txt"));

        Lexer lexer = new Lexer(reader);
        ArrayList<Token> tokens = lexer.tokenize();
        for (Token token : tokens) {
            lexWriter.write(token.print());
            lexWriter.flush();
        }
    }
}
