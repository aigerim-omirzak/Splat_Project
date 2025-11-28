package splat.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import splat.parser.elements.Declaration;
import splat.parser.elements.FunctionCall;
import splat.parser.elements.FunctionDecl;
import splat.parser.elements.ProgramAST;
import splat.parser.elements.Statement;
import splat.parser.elements.VariableDecl;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class Executor {

    private final ProgramAST program;

    private Map<String, FunctionDecl> functionRegistry;
    private Map<String, Value> globalVariables;

    public Executor(ProgramAST programAst) {
        this.program = programAst;
    }

    public void runProgram() throws ExecutionException {
        setupRuntimeMaps();
        try {
            executeStatements(program.getStmts(), globalVariables);
        } catch (ReturnFromCall rfc) {
            throw new ExecutionException("Return cannot be used outside of a function", 0, 0);
        }
    }

    private void setupRuntimeMaps() throws ExecutionException {
        functionRegistry = new HashMap<>();
        globalVariables = new HashMap<>();
        for (Declaration declaration : program.getDecls()) {
            if (declaration instanceof FunctionDecl) {
                FunctionDecl funcDecl = (FunctionDecl) declaration;
                functionRegistry.put(funcDecl.getLabelLexeme(), funcDecl);
                continue;
            }
            initializeGlobalVariable((VariableDecl) declaration);
        }
    }

    private void initializeGlobalVariable(VariableDecl varDecl) throws ExecutionException {
        globalVariables.put(varDecl.getLabelLexeme(), defaultValueFor(varDecl));
    }

    private void executeStatements(List<Statement> statements, Map<String, Value> scope)
            throws ExecutionException, ReturnFromCall {
        for (Statement stmt : statements) {
            stmt.execute(functionRegistry, scope);
        }
    }

    public Value callFunction(FunctionCall call, Map<String, Value> callerScope)
            throws ExecutionException, ReturnFromCall {
        FunctionDecl functionDecl = findFunction(call);
        validateArgumentCount(call, functionDecl.getParams());
        Map<String, Value> calleeScope = buildCalleeScope(call, callerScope, functionDecl);
        try {
            executeStatements(functionDecl.getStmts(), calleeScope);
        } catch (ReturnFromCall rfc) {
            propagateGlobals(globalVariables, calleeScope);
            return rfc.getValue();
        }
        propagateGlobals(globalVariables, calleeScope);
        try {
            return Value.defaultFor(Type.fromToken(functionDecl.getReturnType()));
        } catch (SemanticAnalysisException sae) {
            throw new ExecutionException(sae.getMessage(), functionDecl.getLine(), functionDecl.getColumn());
        }
    }

    private FunctionDecl findFunction(FunctionCall call) throws ExecutionException {
        FunctionDecl decl = functionRegistry.get(call.getName().getLexeme());
        if (decl == null) {
            throw new ExecutionException("Unknown function '" + call.getName().getLexeme() + "'",
                    call.getLine(), call.getColumn());
        }
        return decl;
    }

    private void validateArgumentCount(FunctionCall call, List<VariableDecl> params) throws ExecutionException {
        if (params.size() != call.getArgs().size()) {
            throw new ExecutionException("Incorrect number of arguments for function", call.getLine(), call.getColumn());
        }
    }

    private Map<String, Value> buildCalleeScope(FunctionCall call, Map<String, Value> callerScope,
                                                FunctionDecl decl) throws ExecutionException, ReturnFromCall {
        Map<String, Value> calleeScope = new HashMap<>();
        populateArguments(call, callerScope, calleeScope, decl.getParams());
        populateLocals(calleeScope, decl.getLocalVars());
        return calleeScope;
    }

    private void populateArguments(FunctionCall call, Map<String, Value> callerScope, Map<String, Value> calleeScope,
                                   List<VariableDecl> params) throws ExecutionException, ReturnFromCall {
        for (int i = 0; i < params.size(); i++) {
            VariableDecl param = params.get(i);
            Value argumentValue = call.getArgs().get(i).evaluate(functionRegistry, callerScope);
            calleeScope.put(param.getLabelLexeme(), argumentValue);
        }
    }

    private void populateLocals(Map<String, Value> calleeScope, List<VariableDecl> locals) throws ExecutionException {
        for (VariableDecl local : locals) {
            calleeScope.put(local.getLabelLexeme(), defaultValueFor(local));
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

    private void propagateGlobals(Map<String, Value> globals, Map<String, Value> calleeMap) {
        for (Map.Entry<String, Value> entry : calleeMap.entrySet()) {
            String label = entry.getKey();
            if (globals.containsKey(label)) {
                globals.put(label, entry.getValue());
            }
        }
    }
}

