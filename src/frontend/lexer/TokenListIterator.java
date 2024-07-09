package frontend.lexer;

import java.util.ArrayList;
import java.util.ListIterator;

public class TokenListIterator {
    private final ListIterator<Token> iterator;
    private Token last;

    public TokenListIterator(ArrayList<Token> tokens) {
        iterator = tokens.listIterator();
    }

    public ListIterator<Token> getIterator() {
        return iterator;
    }

    public Token next() {
        return last = iterator.next();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public void unreadToken(int cnt) {
        int i = cnt;
        while (i > 0) {
            i--;
            if (iterator.hasPrevious()) {
                last = iterator.previous();
            } else {
                break;
            }
        }
    }

    @Override
    public String toString() {
        return last.toString();
    }
}
