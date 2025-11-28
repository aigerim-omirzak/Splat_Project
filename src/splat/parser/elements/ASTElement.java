package splat.parser.elements;

import splat.lexer.Token;

public abstract class ASTElement {

    private final Token token;
    private int line;
    private int column;

    public ASTElement(Token tok) {
        this.token = tok;
        this.line = tok.getLine();
        this.column = tok.getCol();
    }

    public Token getToken() {
        return token;
    }

    /**
     * Convenience accessor for the starting token of this AST element.
     */
    public Token getStartToken() {
        return token;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
