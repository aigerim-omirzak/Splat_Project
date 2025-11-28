package splat.parser.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;
import splat.parser.elements.Statement;

public class FunctionCall extends Expression {
    private final Token name;
    private final List<Expression> args;

    public FunctionCall(Token name, List<Expression> args) {
        super(name);
        this.name = name;
        this.args = args;
    }

    public Token getName() { return name; }
    public List<Expression> getArgs() { return args; }

    public Token getStartToken() {
        return name;
    }

    @Override
    public Type analyzeAndGetType(Map<String, FunctionDecl> funcMap,
                                  Map<String, Type> varAndParamMap) throws SemanticAnalysisException {
        FunctionDecl decl = funcMap.get(name.getLexeme());
        if (decl == null) {
            throw new SemanticAnalysisException("Unknown function '" + name.getLexeme() + "'", name.getLine(), name.getCol());
        }

        List<VariableDecl> params = decl.getParams();
        if (params.size() != args.size()) {
            throw new SemanticAnalysisException("Incorrect number of arguments for function", name.getLine(), name.getCol());
        }

        for (int i = 0; i < params.size(); i++) {
            Type expected = Type.fromToken(params.get(i).getType());
            Type actual = args.get(i).analyzeAndGetType(funcMap, varAndParamMap);
            if (expected != actual) {
                throw new SemanticAnalysisException("Argument type mismatch", args.get(i));
            }
        }

        return Type.fromToken(decl.getReturnType());
    }

    @Override
    public Value evaluate(Map<String, FunctionDecl> funcMap,
                          Map<String, Value> varAndParamMap) throws ExecutionException, ReturnFromCall {
        FunctionDecl decl = resolveFunction(funcMap);
        validateArgCount(decl);

        Map<String, Value> calleeScope = new HashMap<>();
        bindArguments(funcMap, varAndParamMap, decl, calleeScope);
        seedLocals(decl, calleeScope);

        try {
            for (Statement stmt : decl.getStmts()) {
                stmt.execute(funcMap, calleeScope);
            }
        } catch (ReturnFromCall rfc) {
            syncGlobals(varAndParamMap, calleeScope, decl);
            return rfc.getValue();
        }

        syncGlobals(varAndParamMap, calleeScope, decl);
        return defaultReturnValue(decl);
    }

    private FunctionDecl resolveFunction(Map<String, FunctionDecl> funcMap) throws ExecutionException {
        FunctionDecl decl = funcMap.get(name.getLexeme());
        if (decl == null) {
            throw new ExecutionException("Unknown function '" + name.getLexeme() + "'", getLine(), getColumn());
        }
        return decl;
    }

    private void validateArgCount(FunctionDecl decl) throws ExecutionException {
        if (decl.getParams().size() != args.size()) {
            throw new ExecutionException("Incorrect number of arguments for function", getLine(), getColumn());
        }
    }

    private void bindArguments(Map<String, FunctionDecl> funcMap, Map<String, Value> callerScope,
                               FunctionDecl decl, Map<String, Value> calleeScope)
            throws ExecutionException, ReturnFromCall {
        List<VariableDecl> params = decl.getParams();
        for (int i = 0; i < params.size(); i++) {
            VariableDecl param = params.get(i);
            Value argVal = args.get(i).evaluate(funcMap, callerScope);
            calleeScope.put(param.getLabelLexeme(), argVal);
        }
    }

    private void seedLocals(FunctionDecl decl, Map<String, Value> calleeScope) throws ExecutionException {
        for (VariableDecl local : decl.getLocalVars()) {
            calleeScope.put(local.getLabelLexeme(), defaultValueFor(local));
        }
    }

    private Value defaultReturnValue(FunctionDecl decl) throws ExecutionException {
        try {
            return Value.defaultFor(Type.fromToken(decl.getReturnType()));
        } catch (SemanticAnalysisException sae) {
            throw new ExecutionException(sae.getMessage(), decl.getLine(), decl.getColumn());
        }
    }

    private Value defaultValueFor(VariableDecl decl) throws ExecutionException {
        try {
            Type type = Type.fromToken(decl.getType());
            return Value.defaultFor(type);
        } catch (SemanticAnalysisException sae) {
            throw new ExecutionException(sae.getMessage(), decl.getLine(), decl.getColumn());
        }
    }

    private void syncGlobals(Map<String, Value> callerMap, Map<String, Value> calleeMap, FunctionDecl decl) {
        java.util.Set<String> shadowed = gatherShadowedLabels(decl);
        for (Map.Entry<String, Value> entry : calleeMap.entrySet()) {
            String label = entry.getKey();
            if (!shadowed.contains(label) && callerMap.containsKey(label)) {
                callerMap.put(label, entry.getValue());
            }
        }
    }

    private java.util.Set<String> gatherShadowedLabels(FunctionDecl decl) {
        java.util.Set<String> shadowed = new java.util.HashSet<>();
        for (VariableDecl param : decl.getParams()) {
            shadowed.add(param.getLabelLexeme());
        }
        for (VariableDecl local : decl.getLocalVars()) {
            shadowed.add(local.getLabelLexeme());
        }
        return shadowed;
    }


    @Override
    public String toString() {
        return name.getLexeme() + args.toString();
    }
}
