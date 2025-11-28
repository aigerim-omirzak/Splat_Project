package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class VariableRef extends Expression {
    private final Token name;

    public VariableRef(Token name) {
        super(name);
        this.name = name;
    }

    public Token getName() { return name; }

    @Override
    public Type analyzeAndGetType(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        String label = name.getLexeme();
        if (!varAndParamMap.containsKey(label)) {
            throw new SemanticAnalysisException("Unknown variable '" + label + "'", name.getLine(), name.getCol());
        }
        return varAndParamMap.get(label);
    }

    @Override
    public Value evaluate(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ExecutionException, ReturnFromCall {
        String label = name.getLexeme();
        if (!varAndParamMap.containsKey(label)) {
            throw new ExecutionException("Unknown variable '" + label + "'", name.getLine(), name.getCol());
        }
        return varAndParamMap.get(label);
    }

    @Override
    public String toString() {
        return name.getLexeme();
    }
}
