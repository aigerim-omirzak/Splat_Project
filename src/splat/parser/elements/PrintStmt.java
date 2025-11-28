package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class PrintStmt extends Statement {
    private final Expression expr;

    public PrintStmt(Token tok, Expression expr) {
        super(tok);
        this.expr = expr;
    }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        if (expr != null) {
            expr.analyzeAndGetType(funcMap, varAndParamMap);
        }
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        if (expr == null) {
            System.out.println();
            return;
        }
        Value v = expr.evaluate(funcMap, varAndParamMap);
        System.out.print(v.getRaw());
    }

    @Override
    public String toString() {
        return "print " + expr;
    }
}
