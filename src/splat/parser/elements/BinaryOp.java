package splat.parser.elements;

import java.util.Map;

import splat.executor.ExecutionException;
import splat.executor.ReturnFromCall;
import splat.executor.Value;
import splat.lexer.Token;
import splat.semanticanalyzer.SemanticAnalysisException;
import splat.semanticanalyzer.Type;

public class BinaryOp extends Expression {
    private final Expression left;
    private final Token op;
    private final Expression right;

    public BinaryOp(Expression left, Token op, Expression right) {
        super(op);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public Type analyzeAndGetType(Map<String, FunctionDecl> funcMap, Map<String, Type> varAndParamMap)
            throws SemanticAnalysisException {
        Type leftType = left.analyzeAndGetType(funcMap, varAndParamMap);
        Type rightType = right.analyzeAndGetType(funcMap, varAndParamMap);
        String lexeme = op.getLexeme();

        switch (lexeme) {
            case "+": case "-": case "*": case "/": case "%":
                if (leftType != Type.INTEGER || rightType != Type.INTEGER) {
                    throw new SemanticAnalysisException("Arithmetic operands must be integers", op.getLine(), op.getCol());
                }
                return Type.INTEGER;
            case "<": case "<=": case ">": case ">=":
                if (leftType != Type.INTEGER || rightType != Type.INTEGER) {
                    throw new SemanticAnalysisException("Comparison operands must be integers", op.getLine(), op.getCol());
                }
                return Type.BOOLEAN;
            case "==": case "!=":
                if (leftType != rightType) {
                    throw new SemanticAnalysisException("Equality operands must match", op.getLine(), op.getCol());
                }
                return Type.BOOLEAN;
            case "and": case "or":
                if (leftType != Type.BOOLEAN || rightType != Type.BOOLEAN) {
                    throw new SemanticAnalysisException("Logical operands must be boolean", op.getLine(), op.getCol());
                }
                return Type.BOOLEAN;
            default:
                throw new SemanticAnalysisException("Unknown operator '" + lexeme + "'", op.getLine(), op.getCol());
        }
    }

    @Override
    public Value evaluate(Map<String, FunctionDecl> funcMap, Map<String, Value> varAndParamMap)
            throws ExecutionException, ReturnFromCall {
        Value l = left.evaluate(funcMap, varAndParamMap);
        Value r = right.evaluate(funcMap, varAndParamMap);
        String lexeme = op.getLexeme();

        switch (lexeme) {
            case "+":
                return Value.ofInteger(l.asInt() + r.asInt());
            case "-":
                return Value.ofInteger(l.asInt() - r.asInt());
            case "*":
                return Value.ofInteger(l.asInt() * r.asInt());
            case "/":
                if (r.asInt() == 0) {
                    throw new ExecutionException("Divide by zero", op.getLine(), op.getCol());
                }
                return Value.ofInteger(l.asInt() / r.asInt());
            case "%":
                if (r.asInt() == 0) {
                    throw new ExecutionException("Divide by zero", op.getLine(), op.getCol());
                }
                return Value.ofInteger(l.asInt() % r.asInt());
            case "<":
                return Value.ofBoolean(l.asInt() < r.asInt());
            case "<=":
                return Value.ofBoolean(l.asInt() <= r.asInt());
            case ">":
                return Value.ofBoolean(l.asInt() > r.asInt());
            case ">=":
                return Value.ofBoolean(l.asInt() >= r.asInt());
            case "==":
                return Value.ofBoolean(l.getRaw().equals(r.getRaw()));
            case "!=":
                return Value.ofBoolean(!l.getRaw().equals(r.getRaw()));
            case "and":
                return Value.ofBoolean(l.asBoolean() && r.asBoolean());
            case "or":
                return Value.ofBoolean(l.asBoolean() || r.asBoolean());
            default:
                throw new ExecutionException("Unknown operator", op.getLine(), op.getCol());
        }
    }

    @Override
    public String toString() {
        return "(" + left + " " + op.getLexeme() + " " + right + ")";
    }
}
