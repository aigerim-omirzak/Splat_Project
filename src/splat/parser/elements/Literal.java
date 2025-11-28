package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class Literal extends Expression {
    private String value;

    public Literal(Token token) {
        super(token);
        this.value = token.getLexeme();
    }

    public String getValue() {
        return value;
    }

    public String getStringValue() {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public boolean isStringLiteral() {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    public boolean isIntegerLiteral() {
        return value.matches("\\d+");
    }

    public boolean isBooleanLiteral() {
        return value.equals("true") || value.equals("false");
    }

    @Override
    public Type analyzeAndGetType(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        if (isIntegerLiteral()) {
            return Type.INTEGER;
        }
        if (isBooleanLiteral()) {
            return Type.BOOLEAN;
        }
        if (isStringLiteral()) {
            return Type.STRING;
        }
        throw new SemanticAnalysisException("Unknown literal", getLine(), getColumn());
    }

    @Override
    public Value evaluate(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ExecutionException, ReturnFromCall {
        if (isIntegerLiteral()) {
            return Value.ofInteger(Integer.parseInt(value));
        }
        if (isBooleanLiteral()) {
            return Value.ofBoolean(Boolean.parseBoolean(value));
        }
        return Value.ofString(getStringValue());
    }
}
