package splat.semanticanalyzer;

import splat.lexer.Token;

public enum Type {
    INTEGER("Integer"),
    BOOLEAN("Boolean"),
    STRING("String"),
    VOID("void");

    private final String displayName;

    Type(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Type fromToken(Token token) throws SemanticAnalysisException {
        if (token == null) {
            return Type.VOID;
        }
        return fromLexeme(token.getLexeme(), token.getLine(), token.getCol());
    }

    public static Type fromLexeme(String lexeme, int line, int column) throws SemanticAnalysisException {
        if (lexeme == null) {
            throw new SemanticAnalysisException("Unknown type", line, column);
        }

        switch (lexeme.toLowerCase()) {
            case "integer":
                return INTEGER;
            case "boolean":
                return BOOLEAN;
            case "string":
                return STRING;
            case "void":
                return VOID;
            default:
                throw new SemanticAnalysisException("Unknown type '" + lexeme + "'", line, column);
        }
    }
}