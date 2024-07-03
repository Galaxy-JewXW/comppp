import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;

public class Lexer {
    private StringBuilder token;
    private Symbol symbol;
    private boolean inMultLineComment;
    private int curPos; // 当前指针的位置
    private String curLine; // 存放读入的当前行

    private BufferedReader input;
    private BufferedWriter output;

    public Lexer(BufferedReader input, BufferedWriter output) {
        this.input = input;
        this.output = output;
        this.curPos = 0;
        this.inMultLineComment = false; // 判断是否在多行注释中
        this.token = new StringBuilder();
    }

    private Symbol keywordTokenDeal(String str) {
        if (Objects.equals(str, "main")) {
            return Symbol.MAINTK;
        } else if (Objects.equals(str, "const")) {
            return Symbol.CONSTTK;
        } else if (Objects.equals(str, "int")) {
            return Symbol.INTTK;
        } else if (Objects.equals(str, "break")) {
            return Symbol.BREAKTK;
        } else if (Objects.equals(str, "continue")) {
            return Symbol.CONTINUETK;
        } else if (Objects.equals(str, "if")) {
            return Symbol.IFTK;
        } else if (Objects.equals(str, "else")) {
            return Symbol.ELSETK;
        } else if (Objects.equals(str, "for")) {
            return Symbol.FORTK;
        } else if (Objects.equals(str, "getint")) {
            return Symbol.GETINTTK;
        } else if (Objects.equals(str, "printf")) {
            return Symbol.PRINTFTK;
        } else if (Objects.equals(str, "return")) {
            return Symbol.RETURNTK;
        } else if (Objects.equals(str, "void")) {
            return Symbol.VOIDTK;
        } else {
            return Symbol.IDENFR;
        }
    }

    public void parse() throws IOException {
        int lineNum = 1;
        while ((curLine = input.readLine()) != null) {
            curPos = 0;
            while (curPos < curLine.length()) {
                symbol = nextSymbol(lineNum);
                if (symbol == Symbol.SINGLE_LINE_COMMENT ||
                symbol == Symbol.EMPTY) {
                    break;
                }
                if (symbol != Symbol.MULTI_LINE_COMMENT) {
                    print();
                }
            }
            lineNum++;
        }
        output.flush();
    }

    public Symbol nextSymbol(int lineNum) {
        this.token.setLength(0); // clear token
        while (curPos < curLine.length() && Character.isWhitespace(curLine.charAt(curPos))) {
            curPos++;
        }
        if (curPos >= curLine.length()) {
            return Symbol.EMPTY;
        }

        if (!this.inMultLineComment) { // 该行文本不在注释中
            if (Character.isLetter(curLine.charAt(curPos))  // ident or reserved word
                    || curLine.charAt(curPos) == '_') {
                while (Character.isLetterOrDigit(curLine.charAt(curPos))
                        || curLine.charAt(curPos) == '_') {
                    token.append(curLine.charAt(curPos++));
                    if (curPos >= curLine.length()) {
                        break;
                    }
                }
                String tokenStr = token.toString();
                return keywordTokenDeal(tokenStr);
            }
            // 字符串
            else if (curLine.charAt(curPos) == '\"') {
                // System.out.println("curLine is:" + curLine);
                // System.out.println("lineNum is:" + lineNum);
                do {
                    token.append(curLine.charAt(curPos++));
                } while (curLine.charAt(curPos) != '\"');
                token.append(curLine.charAt(curPos++));
                return Symbol.STRCON;
            }

            else if (curLine.charAt(curPos) == '/') {
                // 单行注释
                if (curLine.charAt(curPos + 1) == '/') {
                    return Symbol.SINGLE_LINE_COMMENT;
                } // 多行注释
                else if (curLine.charAt(curPos + 1) == '*') {
                    curPos += 2;
                    while (true) {
                        while (curPos < curLine.length() && curLine.charAt(curPos) != '*') {
                            curPos++;
                        }
                        if (curPos >= curLine.length()) { // 多行注释不在这一行结束
                            inMultLineComment = true;
                            return Symbol.MULTI_LINE_COMMENT;
                        } else if (curLine.charAt(curPos + 1) == '/') {
                            curPos += 2;
                            return Symbol.MULTI_LINE_COMMENT; // 行中注释或单行注释
                        } else { // 单独出现一个'*'
                            curPos++;
                        }
                    }
                }
                else {
                    token.append(curLine.charAt(curPos++));
                    return Symbol.DIV;
                }
            }
            // 数字，未检测前导0
            else if (Character.isDigit(curLine.charAt(curPos))) {
                do {
                    token.append(curLine.charAt(curPos++));
                } while (curPos < curLine.length() && Character.isDigit(curLine.charAt(curPos)));
                return Symbol.INTCON;
            }

            else if (curLine.charAt(curPos) == '!') {
                token.append(curLine.charAt(curPos++));
                if (curLine.charAt(curPos) == '=') {
                    token.append(curLine.charAt(curPos++));
                    return Symbol.NEQ;
                }
                else {
                    return Symbol.NOT;
                }
            }

            else {
                return judgeSingleOrTwice();
            }
        }
        else { // error manipulate
            errorMani();
        }
        return Symbol.EMPTY;
    }

    private Symbol judgeSingleOrTwice() {
        if (curLine.charAt(curPos) == '&' &&
                curLine.charAt(curPos + 1) == '&') {
            token.append("&&");
            curPos += 2;
            return Symbol.AND;
        } else if (curLine.charAt(curPos) == '|' &&
                curLine.charAt(curPos + 1) == '|') {
            token.append("||");
            curPos += 2;
            return Symbol.OR;
        }

        else if (curLine.charAt(curPos) == '+') {
            token.append(curLine.charAt(curPos++));
            return Symbol.PLUS;
        } else if (curLine.charAt(curPos) == '-') {
            token.append(curLine.charAt(curPos++));
            return Symbol.MINU;
        } else if (curLine.charAt(curPos) == '*') {
            token.append(curLine.charAt(curPos++));
            return Symbol.MULT;
        } else if (curLine.charAt(curPos) == '%') {
            token.append(curLine.charAt(curPos++));
            return Symbol.MOD;
        } else if (curLine.charAt(curPos) == '<') {
            token.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                token.append(curLine.charAt(curPos++));
                return Symbol.LEQ;
            } else {
                return Symbol.LSS;
            }
        } else if (curLine.charAt(curPos) == '>') {
            token.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                token.append(curLine.charAt(curPos++));
                return Symbol.GEQ;
            } else {
                return Symbol.GRE;
            }
        } else if (curLine.charAt(curPos) == '=') {
            token.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                token.append(curLine.charAt(curPos++));
                return Symbol.EQL;
            } else {
                return Symbol.ASSIGN;
            }
        } else if (curLine.charAt(curPos) == ';') {
            token.append(curLine.charAt(curPos++));
            return Symbol.SEMICN;
        } else if (curLine.charAt(curPos) == ',') {
            token.append(curLine.charAt(curPos++));
            return Symbol.COMMA;
        } else if (curLine.charAt(curPos) == '(') {
            token.append(curLine.charAt(curPos++));
            return Symbol.LPARENT;
        } else if (curLine.charAt(curPos) == ')') {
            token.append(curLine.charAt(curPos++));
            return Symbol.RPARENT;
        } else if (curLine.charAt(curPos) == '[') {
            token.append(curLine.charAt(curPos++));
            return Symbol.LBRACK;
        } else if (curLine.charAt(curPos) == ']') {
            token.append(curLine.charAt(curPos++));
            return Symbol.RBRACK;
        } else if (curLine.charAt(curPos) == '{') {
            token.append(curLine.charAt(curPos++));
            return Symbol.LBRACE;
        } else if (curLine.charAt(curPos) == '}') {
            token.append(curLine.charAt(curPos++));
            return Symbol.RBRACE;
        }
        return Symbol.EMPTY;
    }

    private Symbol errorMani() {
        while (true) {
            while (curPos < curLine.length() && curLine.charAt(curPos) != '*') {
                curPos++;
            }
            if (curPos >= curLine.length()) { // 多行注释不在这一行结束
                return Symbol.MULTI_LINE_COMMENT;
            } else if (curLine.charAt(curPos + 1) == '/') {
                this.inMultLineComment = false;
                curPos += 2;
                return Symbol.MULTI_LINE_COMMENT; // 行中注释或单行注释
            } else { // 单独出现一个'*'
                curPos++;
            }
        }
    }

    public void print() throws IOException {
        output.write(symbol.name() + " " + token + "\n");
    }
}
