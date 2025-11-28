package splat.parser.elements;

import java.util.List;
import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class Block extends Statement {
    private List<Statement> statements;

    public Block(Token tok, List<Statement> stmts) {
        super(tok);
        this.statements = stmts;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        for (Statement stmt : statements) {
            stmt.analyze(funcMap, varAndParamMap);
        }
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        for (Statement stmt : statements) {
            stmt.execute(funcMap, varAndParamMap);
        }
    }
}
