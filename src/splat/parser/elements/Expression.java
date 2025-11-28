package splat.parser.elements;

import java.util.Map;

import splat.executor.Value;
import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public abstract class Expression extends ASTElement {

    public Expression(Token tok) {
        super(tok);
    }

    public abstract Type analyzeAndGetType(Map<String, FunctionDecl> funcMap,
                                           Map<String, Type> varAndParamMap) throws SemanticAnalysisException;

    public abstract Value evaluate(Map<String, FunctionDecl> funcMap,
                                   Map<String, Value> varAndParamMap) throws ExecutionException, ReturnFromCall;
}
