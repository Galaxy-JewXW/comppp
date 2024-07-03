import java.io.*;

public class Compiler {
    public static void main(String[] args) {
        try (
                BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"));
                BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"))
        ) {
            Lexer lexer = new Lexer(reader, output);
            lexer.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
