package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class ReturnStmt extends Statement {
    private Expression expr;

    public ReturnStmt(Token tok, Expression expr) {
        super(tok);
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    public Token getReturnToken() {
        return super.getToken();
    }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        if (!varAndParamMap.containsKey(Statement.RETURN_TYPE_SLOT)) {
            throw new SemanticAnalysisException("Return cannot be used outside of a function",
                    getLine(), getColumn());
        }

        Type expected = varAndParamMap.get(Statement.RETURN_TYPE_SLOT);
        if (expected == Type.VOID && expr != null) {
            throw new SemanticAnalysisException("Void functions cannot return a value",
                    getLine(), getColumn());
        }

        if (expected != Type.VOID && expr == null) {
            throw new SemanticAnalysisException("Non-void functions must return a value",
                    getLine(), getColumn());
        }

        if (expr != null) {
            Type actual = expr.analyzeAndGetType(funcMap, varAndParamMap);
            if (actual != expected) {
                throw new SemanticAnalysisException("Return type mismatch", expr.getLine(), expr.getColumn());
            }
        }
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        Value val = expr == null ? Value.voidValue() : expr.evaluate(funcMap, varAndParamMap);
        throw new ReturnFromCall(val);
    }

    @Override
    public String toString() {
        return "return " + expr;
    }
}
