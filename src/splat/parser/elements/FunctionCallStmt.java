package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class FunctionCallStmt extends Statement {
    private final FunctionCall call;

    public FunctionCallStmt(FunctionCall call) {
        super(call.getStartToken());
        this.call = call;
    }

    public FunctionCall getCall() {
        return call;
    }

    @Override
    public void analyze(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        call.analyzeAndGetType(funcMap, varAndParamMap);
    }

    @Override
    public void execute(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ReturnFromCall, ExecutionException {
        call.evaluate(funcMap, varAndParamMap);
    }


    @Override
    public String toString() {
        return "FunctionCallStmt(" + call + ")";
    }
}
