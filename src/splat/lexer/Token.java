package splat.lexer;
import java.util.Objects;

public class Token {
    private final String lexeme;
    private final int line;
    private final int col;

    public Token(String lexeme, int line, int col) {
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return String.format("Token(lexeme=\"%s\", line=%d, col=%d)", lexeme, line, col);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token token = (Token) o;
        return line == token.line && col == token.col && Objects.equals(lexeme, token.lexeme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lexeme, line, col);
    }
}
