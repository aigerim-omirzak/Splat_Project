package splat.parser.elements;

import splat.lexer.Token;

public class VariableDecl extends Declaration {
    private final Token name;
    private final Token type;

    public VariableDecl(Token name, Token type) {
        super(name);
        this.name = name;
        this.type = type;
    }

    public Token getName() {
        return name;
    }

    public Token getType() {
        return type;
    }


    @Override
    public String toString() {
        return String.format("VarDecl(name=%s, type=%s)", name.getLexeme(), type.getLexeme());
    }
}
