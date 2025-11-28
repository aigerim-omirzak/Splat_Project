package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class UnaryOp extends Expression {
    private final Token op;
    private final Expression expr;

    public UnaryOp(Token op, Expression expr) {
        super(op);
        this.op = op;
        this.expr = expr;
    }

    public Token getOperator() {
        return op;
    }

    public Expression getExpr() {
        return expr;
    }

    @Override
    public Type analyzeAndGetType(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        Type inner = expr.analyzeAndGetType(funcMap, varAndParamMap);
        if (op.getLexeme().equals("-")) {
            if (inner != Type.INTEGER) {
                throw new SemanticAnalysisException("Unary - requires integer", op.getLine(), op.getCol());
            }
            return Type.INTEGER;
        }
        if (op.getLexeme().equals("not")) {
            if (inner != Type.BOOLEAN) {
                throw new SemanticAnalysisException("not requires boolean", op.getLine(), op.getCol());
            }
            return Type.BOOLEAN;
        }
        throw new SemanticAnalysisException("Unknown unary operator", op.getLine(), op.getCol());
    }

    @Override
    public Value evaluate(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ExecutionException, ReturnFromCall {
        Value v = expr.evaluate(funcMap, varAndParamMap);
        if (op.getLexeme().equals("-")) {
            return Value.ofInteger(-v.asInt());
        }
        if (op.getLexeme().equals("not")) {
            return Value.ofBoolean(!v.asBoolean());
        }
        throw new ExecutionException("Unknown unary operator", op.getLine(), op.getCol());
    }

    @Override
    public String toString() {
        return "(" + op.getLexeme() + " " + expr + ")";
    }
}
