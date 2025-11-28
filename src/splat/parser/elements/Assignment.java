package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class Assignment extends Statement {
    private Token variable;
    private Expression expr;

    public Assignment(Token variable, Expression expr) {
        super(variable);
        this.variable = variable;
        this.expr = expr;
    }

    public Expression getExpression() { return expr; }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        String name = variable.getLexeme();
        if (!varAndParamMap.containsKey(name)) {
            throw new SemanticAnalysisException("Unknown variable '" + name + "'", variable.getLine(), variable.getCol());
        }
        Type expected = varAndParamMap.get(name);
        Type actual = expr.analyzeAndGetType(funcMap, varAndParamMap);
        if (expected != actual) {
            throw new SemanticAnalysisException("Type mismatch in assignment", variable.getLine(), variable.getCol());
        }
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        Value value = expr.evaluate(funcMap, varAndParamMap);
        varAndParamMap.put(variable.getLexeme(), value);
    }

    @Override
    public String toString() {
        return variable.getLexeme() + " := " + expr;
    }
}

