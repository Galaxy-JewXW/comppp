package Lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class Lexer {
    private BufferedReader input;
    private ArrayList<Token> tokens = new ArrayList<>();
    private int curPos = 0;
    private String curLine = "";
    private boolean inMultiComment = false;
    public Lexer(BufferedReader input) {
        this.input = input;
    }

    public ArrayList<Token> getTokens() {
        return new ArrayList<>(tokens);
    }

    public void tokenize() throws IOException {
        int line = 1;
        while ((curLine = input.readLine()) != null) {
            curPos = 0;
            while (curPos < curLine.length()) {
                Token token = nextToken(line);
                if (token.getType() == Token.TokenType.SINGLE_LINE_COMMENT
                    || token.getType() == Token.TokenType.EMPTY_LINE) {
                    break;
                }
                if (token.getType() != Token.TokenType.MULTI_LINE_COMMENT) {
                    tokens.add(token);
                }
            }
            line++;
        }
    }

    public Token nextToken(int lineNum) {
        StringBuilder sb = new StringBuilder();
        while (curPos < curLine.length() && Character.isWhitespace(curLine.charAt(curPos))) {
            curPos++;
        }
        if (curPos >= curLine.length()) {
            return new Token(Token.TokenType.EMPTY_LINE, "", lineNum, curPos);
        }

        if (inMultiComment) {
            return dealMultiComment();
        }

        if (Character.isLetter(curLine.charAt(curPos)) || curLine.charAt(curPos) == '_') {
            while (Character.isLetterOrDigit(curLine.charAt(curPos)) || curLine.charAt(curPos) == '_') {
                sb.append(curLine.charAt(curPos));
                curPos++;
                if (curPos >= curLine.length()) {
                    break;
                }
            }
            return keyword2Token(sb.toString(), lineNum);
        } else if (curLine.charAt(curPos) == '\"') {
            do {
                sb.append(curLine.charAt(curPos));
                curPos++;
            } while (curLine.charAt(curPos) != '\"');
            sb.append(curLine.charAt(curPos));
            curPos++;
            return new Token(Token.TokenType.STRCON, sb.toString(), lineNum, curPos);
        } else if (Character.isDigit(curLine.charAt(curPos))) {
            do {
                sb.append(curLine.charAt(curPos));
                curPos++;
            } while (curPos < curLine.length() && Character.isDigit(curLine.charAt(curPos)));
            return new Token(Token.TokenType.INTCON, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '!') {
            sb.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                sb.append(curLine.charAt(curPos));
                curPos++;
                return new Token(Token.TokenType.NEQ, sb.toString(), lineNum, curPos);
            }
            else {
                return new Token(Token.TokenType.NOT, sb.toString(), lineNum, curPos);
            }
        } else if (curLine.charAt(curPos) == '/') {
            if (curLine.charAt(curPos + 1) == '/') {
                return new Token(Token.TokenType.SINGLE_LINE_COMMENT, "//", lineNum, curPos);
            } else if (curLine.charAt(curPos + 1) == '*') {
                curPos += 2;
                while (true) {
                    while (curPos < curLine.length() && curLine.charAt(curPos) != '*') {
                        curPos++;
                    }
                    if (curPos >= curLine.length()) {
                        inMultiComment = true;
                        return new Token(Token.TokenType.MULTI_LINE_COMMENT, "", lineNum, curPos);
                    } else if (curLine.charAt(curPos + 1) == '/') {
                        inMultiComment = false;
                        curPos += 2;
                        return new Token(Token.TokenType.MULTI_LINE_COMMENT, "", lineNum, curPos);
                    } else {
                        curPos++;
                    }
                }
            } else {
                sb.append(curLine.charAt(curPos));
                curPos++;
                return new Token(Token.TokenType.DIV, sb.toString(), lineNum, curPos);
            }
        } else {
            return parseOperator(lineNum);
        }
    }

    private Token dealMultiComment() {
        while (true) {
            while (curPos < curLine.length() && curLine.charAt(curPos) != '*') {
                curPos++;
            }
            if (curPos >= curLine.length()) {
                return new Token(Token.TokenType.MULTI_LINE_COMMENT, "", -1, curPos);
            } else if (curLine.charAt(curPos + 1) == '/') {
                inMultiComment = false;
                curPos += 2;
                return new Token(Token.TokenType.MULTI_LINE_COMMENT, "", -1, curPos);
            } else {
                curPos++;
            }
        }
    }

    private Token keyword2Token(String string, int lineNum) {
        switch (string) {
            case "main":
                return new Token(Token.TokenType.MAINTK, "main", lineNum, curPos);
            case "const":
                return new Token(Token.TokenType.CONSTTK, "const", lineNum, curPos);
            case "int":
                return new Token(Token.TokenType.INTTK, "int", lineNum, curPos);
            case "break":
                return new Token(Token.TokenType.BREAKTK, "break", lineNum, curPos);
            case "continue":
                return new Token(Token.TokenType.CONTINUETK, "continue", lineNum, curPos);
            case "if":
                return new Token(Token.TokenType.IFTK, "if", lineNum, curPos);
            case "else":
                return new Token(Token.TokenType.ELSETK, "else", lineNum, curPos);
            case "for":
                return new Token(Token.TokenType.FORTK, "for", lineNum, curPos);
            case "getint":
                return new Token(Token.TokenType.GETINTTK, "getint", lineNum, curPos);
            case "printf":
                return new Token(Token.TokenType.PRINTFTK, "printf", lineNum, curPos);
            case "return":
                return new Token(Token.TokenType.RETURNTK, "return", lineNum, curPos);
            case "void":
                return new Token(Token.TokenType.VOIDTK, "void", lineNum, curPos);
            default:
                return new Token(Token.TokenType.IDENFR, string, lineNum, curPos);
        }
    }

    private Token parseOperator(int lineNum) {
        StringBuilder sb = new StringBuilder();
        if (curLine.charAt(curPos) == '&' &&
                curLine.charAt(curPos + 1) == '&') {
            sb.append("&&");
            curPos += 2;
            return new Token(Token.TokenType.AND, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '|' &&
                curLine.charAt(curPos + 1) == '|') {
            sb.append("||");
            curPos += 2;
            return new Token(Token.TokenType.OR, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '+') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.PLUS, "+", lineNum, curPos);
        } else if (curLine.charAt(curPos) == '-') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.MINU, "-", lineNum, curPos);
        } else if (curLine.charAt(curPos) == '*') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.MULT, "*", lineNum, curPos);
        } else if (curLine.charAt(curPos) == '%') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.MOD, "%", lineNum, curPos);
        } else if (curLine.charAt(curPos) == '<') {
            sb.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                sb.append(curLine.charAt(curPos++));
                return new Token(Token.TokenType.LEQ, sb.toString(), lineNum, curPos);
            } else {
                return new Token(Token.TokenType.LSS, sb.toString(), lineNum, curPos);
            }
        } else if (curLine.charAt(curPos) == '>') {
            sb.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                sb.append(curLine.charAt(curPos++));
                return new Token(Token.TokenType.GEQ, sb.toString(), lineNum, curPos);
            } else {
                return new Token(Token.TokenType.GRE, sb.toString(), lineNum, curPos);
            }
        } else if (curLine.charAt(curPos) == '=') {
            sb.append(curLine.charAt(curPos++));
            if (curLine.charAt(curPos) == '=') {
                sb.append(curLine.charAt(curPos++));
                return new Token(Token.TokenType.EQL, sb.toString(), lineNum, curPos);
            } else {
                return new Token(Token.TokenType.ASSIGN, sb.toString(), lineNum, curPos);
            }
        } else if (curLine.charAt(curPos) == ';') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.SEMICN, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == ',') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.COMMA, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '(') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.LPARENT, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == ')') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.RPARENT, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '[') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.LBRACK, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == ']') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.RBRACK, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '{') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.LBRACE, sb.toString(), lineNum, curPos);
        } else if (curLine.charAt(curPos) == '}') {
            sb.append(curLine.charAt(curPos++));
            return new Token(Token.TokenType.RBRACE, sb.toString(), lineNum, curPos);
        }
        return new Token(Token.TokenType.EMPTY_LINE, "", lineNum, curPos);
    }
}
