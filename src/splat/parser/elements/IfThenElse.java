package splat.parser.elements;

import java.util.List;
import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class IfThenElse extends Statement {
    private Expression condition;
    private List<Statement> thenStmts;
    private List<Statement> elseStmts;
    private Token ifToken;

    public IfThenElse(Token tok, Expression condition,
                      List<Statement> thenStmts, List<Statement> elseStmts) {
        super(tok);
        this.ifToken = tok;
        this.condition = condition;
        this.thenStmts = thenStmts;
        this.elseStmts = elseStmts;
    }

    public Token getIfToken() {
        return ifToken;
    }

    public Expression getCondition() { return condition; }
    public List<Statement> getThenStmts() { return thenStmts; }
    public List<Statement> getElseStmts() { return elseStmts; }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        Type condType = condition.analyzeAndGetType(funcMap, varAndParamMap);
        if (condType != Type.BOOLEAN) {
            throw new SemanticAnalysisException("If condition must be boolean", ifToken.getLine(), ifToken.getCol());
        }
        for (Statement stmt : thenStmts) {
            stmt.analyze(funcMap, varAndParamMap);
        }
        for (Statement stmt : elseStmts) {
            stmt.analyze(funcMap, varAndParamMap);
        }
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        Value cond = condition.evaluate(funcMap, varAndParamMap);
        List<Statement> branch = cond.asBoolean() ? thenStmts : elseStmts;
        for (Statement stmt : branch) {
            stmt.execute(funcMap, varAndParamMap);
        }
    }
}
