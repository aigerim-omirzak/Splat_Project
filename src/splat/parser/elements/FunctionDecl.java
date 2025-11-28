package splat.parser.elements;

import java.util.List;
import splat.lexer.Token;

public class FunctionDecl extends Declaration {
    private final List<VariableDecl> params;
    private final Token returnType;
    private final List<VariableDecl> localVars;
    private final List<Statement> body;

    public FunctionDecl(Token name, List<VariableDecl> params, Token returnType,
                        List<VariableDecl> localVars, List<Statement> body) {
        super(name);
        this.params = params;
        this.returnType = returnType;
        this.localVars = localVars;
        this.body = body;
    }

    public Token getName() {
        return getLabel();
    }

    public List<VariableDecl> getParams() {
        return params;
    }

    public List<VariableDecl> getLocalVars() {
        return localVars;
    }

    public List<Statement> getStmts() {
        return body;
    }

    public List<Statement> getBody() {
        return body;
    }

    public Token getReturnType() {
        return returnType;
    }


    @Override
    public String toString() {
        String ret = (returnType == null) ? "void" : returnType.getLexeme();
        return String.format(
                "FunctionDecl(name=%s, params=%s, returnType=%s)",
                getName().getLexeme(), params, ret
        );
    }
}
