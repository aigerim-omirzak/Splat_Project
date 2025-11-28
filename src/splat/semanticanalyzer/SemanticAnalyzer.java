package splat.semanticanalyzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import splat.parser.elements.Block;
import splat.parser.elements.Declaration;
import splat.parser.elements.FunctionDecl;
import splat.parser.elements.IfThenElse;
import splat.parser.elements.ProgramAST;
import splat.parser.elements.ReturnStmt;
import splat.parser.elements.Statement;
import splat.parser.elements.VariableDecl;
import splat.parser.elements.WhileLoop;

public class SemanticAnalyzer {

    private final ProgramAST program;
    private final Map<String, FunctionDecl> functionByName;
    private final Map<String, Type> globalVariableTypes;

    public SemanticAnalyzer(ProgramAST program) {
        this.program = program;
        this.functionByName = new HashMap<>();
        this.globalVariableTypes = new HashMap<>();
    }

    public void analyze() throws SemanticAnalysisException {
        collectGlobalDeclarations();
        analyzeFunctions();
        analyzeProgramBody();
    }

    private void collectGlobalDeclarations() throws SemanticAnalysisException {
        Set<String> declaredLabels = new HashSet<>();
        for (Declaration decl : program.getDecls()) {
            String label = decl.getLabelLexeme();
            ensureUniqueGlobalLabel(declaredLabels, decl, label);
            registerGlobalDeclaration(decl, label);
        }
    }

    private void analyzeFunctions() throws SemanticAnalysisException {
        for (FunctionDecl functionDecl : functionByName.values()) {
            analyzeFunction(functionDecl);
        }
    }

    private void analyzeFunction(FunctionDecl functionDecl) throws SemanticAnalysisException {
        Map<String, Type> typeEnvironment = new HashMap<>();
        Set<String> namesInFunction = new HashSet<>();

        populateVariableTypes(functionDecl.getParams(), "Parameters cannot be declared with type void",
                typeEnvironment, namesInFunction);
        populateVariableTypes(functionDecl.getLocalVars(), "Local variables cannot be declared with type void",
                typeEnvironment, namesInFunction);

        Type returnType = Type.fromToken(functionDecl.getReturnType());
        typeEnvironment.put(Statement.RETURN_TYPE_SLOT, returnType);

        List<Statement> body = functionDecl.getBody();
        if (body != null) {
            for (Statement stmt : body) {
                stmt.analyze(functionByName, typeEnvironment);
            }
        }

        if (returnType != Type.VOID && !containsReturn(body)) {
            throw new SemanticAnalysisException(
                    "Function '" + functionDecl.getName().getLexeme() + "' must return a value",
                    functionDecl.getLine(), functionDecl.getColumn());
        }
    }

    private void analyzeProgramBody() throws SemanticAnalysisException {
        Map<String, Type> scope = new HashMap<>(globalVariableTypes);
        for (Statement stmt : program.getStmts()) {
            stmt.analyze(functionByName, scope);
        }
    }

    private void ensureUniqueGlobalLabel(Set<String> labels, Declaration decl, String label)
            throws SemanticAnalysisException {
        if (!labels.add(label)) {
            throw new SemanticAnalysisException(
                    "Duplicate declaration: '" + label + "'",
                    decl.getLine(), decl.getColumn());
        }
    }

    private void registerGlobalDeclaration(Declaration decl, String label) throws SemanticAnalysisException {
        if (decl instanceof VariableDecl) {
            registerGlobalVariable((VariableDecl) decl, label);
        } else if (decl instanceof FunctionDecl) {
            functionByName.put(label, (FunctionDecl) decl);
        }
    }

    private void registerGlobalVariable(VariableDecl varDecl, String label) throws SemanticAnalysisException {
        Type type = Type.fromToken(varDecl.getType());
        if (type == Type.VOID) {
            throw new SemanticAnalysisException(
                    "Variables cannot be declared with type void",
                    varDecl.getLine(), varDecl.getColumn());
        }
        globalVariableTypes.put(label, type);
    }

    private void registerLocalName(VariableDecl decl, Set<String> names) throws SemanticAnalysisException {
        String label = decl.getName().getLexeme();
        if (!names.add(label)) {
            throw new SemanticAnalysisException(
                    "Duplicate declaration inside function: '" + label + "'",
                    decl.getLine(), decl.getColumn());
        }
    }

    private void ensureNotFunctionName(String name, int line, int column) throws SemanticAnalysisException {
        if (functionByName.containsKey(name)) {
            throw new SemanticAnalysisException(
                    "Identifier '" + name + "' conflicts with an existing function name",
                    line, column);
        }
    }

    private void populateVariableTypes(List<VariableDecl> declarations,
                                       String voidErrorMessage,
                                       Map<String, Type> typeEnvironment,
                                       Set<String> namesInFunction) throws SemanticAnalysisException {
        if (declarations == null) {
            return;
        }

        for (VariableDecl declaration : declarations) {
            registerLocalName(declaration, namesInFunction);
            ensureNotFunctionName(declaration.getName().getLexeme(),
                    declaration.getLine(), declaration.getColumn());

            Type type = Type.fromToken(declaration.getType());
            if (type == Type.VOID) {
                throw new SemanticAnalysisException(
                        voidErrorMessage,
                        declaration.getLine(), declaration.getColumn());
            }

            typeEnvironment.put(declaration.getName().getLexeme(), type);
        }
    }

    private boolean containsReturn(List<Statement> statements) {
        if (statements == null) {
            return false;
        }

        for (Statement stmt : statements) {
            if (statementContainsReturn(stmt)) {
                return true;
            }
        }

        return false;
    }

    private boolean statementContainsReturn(Statement statement) {
        if (statement instanceof ReturnStmt) {
            return true;
        }

        if (statement instanceof IfThenElse) {
            IfThenElse ite = (IfThenElse) statement;
            return containsReturn(ite.getThenStmts()) || containsReturn(ite.getElseStmts());
        }

        if (statement instanceof WhileLoop) {
            return containsReturn(((WhileLoop) statement).getBody());
        }

        if (statement instanceof Block) {
            return containsReturn(((Block) statement).getStatements());
        }

        return false;
    }
}