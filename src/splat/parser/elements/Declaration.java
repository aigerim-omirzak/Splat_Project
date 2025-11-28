package splat.parser.elements;

import splat.lexer.Token;

public abstract class Declaration extends ASTElement {

    private final Token label;

    public Declaration(Token tok) {
        super(tok);
        this.label = tok;
    }

    public Token getLabel() {
        return label;
    }

    public String getLabelLexeme() {
        return label.getLexeme();
    }
}
