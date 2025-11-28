package splat.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import splat.lexer.Token;
import splat.parser.elements.*;

public class Parser {

    private final List<Token> tokens;
    private int position;

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "program", "begin", "end", "if", "then", "else",
            "while", "loop", "do", "return", "is",
            "print", "print_line", "and", "or", "not",
            "true", "false", "for"
    ));

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public ProgramAST parse() throws ParseException {
        Token programToken = expect("program");
        List<Declaration> declarations = parseDeclarations();
        expect("begin");
        List<Statement> statements = parseStatementList("end");
        expect("end");
        match(";");
        ensureEOF();
        return new ProgramAST(declarations, statements, programToken);
    }

    private List<Declaration> parseDeclarations() throws ParseException {
        List<Declaration> decls = new ArrayList<>();
        while (!isAtEnd() && isIdentifier(peek())) {
            decls.add(parseDeclaration());
        }
        return decls;
    }

    private Declaration parseDeclaration() throws ParseException {
        Token name = consumeIdentifier("identifier at start of declaration");
        if (check(":")) {
            return parseVariableDecl(name);
        } else if (check("(")) {
            return parseFunctionDecl(name);
        }
        Token next = peek();
        throw new ParseException("Unexpected token after identifier '" + name.getLexeme() + "'", next != null ? next : name);
    }

    private VariableDecl parseVariableDecl(Token nameToken) throws ParseException {
        expect(":");
        Token typeToken = consumeIdentifier("type name");
        expect(";");
        return new VariableDecl(nameToken, typeToken);
    }

    private FunctionDecl parseFunctionDecl(Token nameToken) throws ParseException {
        List<VariableDecl> params = new ArrayList<>();
        expect("(");
        if (!check(")")) {
            do {
                Token paramName = consumeIdentifier("parameter name");
                expect(":");
                Token paramType = consumeIdentifier("parameter type");
                params.add(new VariableDecl(paramName, paramType));
            } while (match(","));
        }
        expect(")");
        expect(":");
        Token returnType = consumeIdentifier("return type");
        expect("is");

        List<VariableDecl> locals = new ArrayList<>();
        while (isIdentifier(peek()) && ":".equals(lookAheadLexeme(1))) {
            Token localName = consumeIdentifier("local variable name");
            locals.add(parseVariableDecl(localName));
        }

        expect("begin");
        List<Statement> body = parseStatementList("end");
        expect("end");
        if (check(nameToken.getLexeme())) {
            advance();
        }
        expect(";");
        return new FunctionDecl(nameToken, params, returnType, locals, body);
    }

    private List<Statement> parseStatementList(String... terminators) throws ParseException {
        Set<String> stops = new HashSet<>(Arrays.asList(terminators));
        List<Statement> statements = new ArrayList<>();
        while (!isAtEnd() && (terminators.length == 0 || !stops.contains(peek().getLexeme()))) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private Statement parseStatement() throws ParseException {
        Token token = peek();
        if (token == null) {
            throw new ParseException("Unexpected end of input in statement", lastToken());
        }
        String lexeme = token.getLexeme();
        switch (lexeme) {
            case "if":
                return parseIf();
            case "while":
                return parseWhile();
            case "print":
            case "print_line":
                return parsePrint();
            case "return":
                return parseReturn();
            case "begin":
                return parseBlock();
            default:
                if (isIdentifier(token) && ":=".equals(lookAheadLexeme(1))) {
                    return parseAssignment();
                } else if (isIdentifier(token) && "(".equals(lookAheadLexeme(1))) {
                    FunctionCall call = parseFunctionCall();
                    expect(";");
                    return new FunctionCallStmt(call);
                }
        }
        throw new ParseException("Unexpected token in statement: " + lexeme, token);
    }

    private Assignment parseAssignment() throws ParseException {
        Token name = consumeIdentifier("variable name");
        expect(":=");
        ensureParenthesizedArithmeticInAssignment();
        Expression expr = parseExpression();
        expect(";");
        return new Assignment(name, expr);
    }

    private IfThenElse parseIf() throws ParseException {
        Token ifToken = expect("if");
        Expression condition = parseExpression();
        expect("then");
        List<Statement> thenPart = parseStatementList("else", "end");
        List<Statement> elsePart = new ArrayList<>();
        if (match("else")) {
            elsePart = parseStatementList("end");
        }
        expect("end");
        expect("if");
        expect(";");
        return new IfThenElse(ifToken, condition, thenPart, elsePart);
    }

    private WhileLoop parseWhile() throws ParseException {
        Token whileToken = expect("while");
        Expression condition = parseExpression();
        if (!(match("do") || match("loop"))) {
            Token err = peek();
            throw new ParseException("Expected 'do' or 'loop' after while condition", err != null ? err : whileToken);
        }
        List<Statement> body = parseStatementList("end");
        expect("end");
        if (!(match("while") || match("loop"))) {
            Token err = peek();
            throw new ParseException("Expected 'while' or 'loop' after 'end'", err != null ? err : whileToken);
        }
        expect(";");
        return new WhileLoop(whileToken, condition, body);
    }

    private PrintStmt parsePrint() throws ParseException {
        Token printToken = advance();
        Expression expr = null;
        boolean isPrintLine = printToken.getLexeme().equals("print_line");
        if (isPrintLine) {
            if (startsExpression()) {
                Token err = peek();
                throw new ParseException("print_line does not take an argument", err != null ? err : printToken);
            }
        } else {
            if (!startsExpression()) {
                Token err = peek();
                throw new ParseException("print requires an expression", err != null ? err : printToken);
            }
            if (isParenthesizedStringLiteral()) {
                Token err = tokens.get(position + 1);
                throw new ParseException("print string literals must not be parenthesized", err);
            }
            expr = parseExpression();
        }
        expect(";");
        return new PrintStmt(printToken, expr);
    }

    private ReturnStmt parseReturn() throws ParseException {
        Token returnToken = expect("return");
        Expression expr = null;
        if (startsExpression()) {
            expr = parseExpression();
        }
        expect(";");
        return new ReturnStmt(returnToken, expr);
    }

    private Block parseBlock() throws ParseException {
        Token beginToken = expect("begin");
        List<Statement> stmts = parseStatementList("end");
        expect("end");
        expect(";");
        return new Block(beginToken, stmts);
    }

    private FunctionCall parseFunctionCall() throws ParseException {
        Token name = consumeIdentifier("function name");
        return finishFunctionCall(name);
    }

    private FunctionCall finishFunctionCall(Token name) throws ParseException {
        expect("(");
        List<Expression> args = new ArrayList<>();
        if (!check(")")) {
            do {
                args.add(parseExpression());
            } while (match(","));
        }
        expect(")");
        return new FunctionCall(name, args);
    }

    private Expression parseExpression() throws ParseException {
        return parseOr();
    }

    private Expression parseOr() throws ParseException {
        Expression expr = parseAnd();
        while (match("or")) {
            Token op = previous();
            Expression right = parseAnd();
            expr = new BinaryOp(expr, op, right);
        }
        return expr;
    }

    private Expression parseAnd() throws ParseException {
        Expression expr = parseComparison();
        while (match("and")) {
            Token op = previous();
            Expression right = parseComparison();
            expr = new BinaryOp(expr, op, right);
        }
        return expr;
    }

    private Expression parseComparison() throws ParseException {
        Expression expr = parseAddSub();
        while (check("<") || check("<=") || check(">") || check(">=") || check("==") || check("!=")) {
            Token op = advance();
            Expression right = parseAddSub();
            expr = new BinaryOp(expr, op, right);
        }
        return expr;
    }

    private Expression parseAddSub() throws ParseException {
        Expression expr = parseMulDiv();
        while (check("+") || check("-")) {
            Token op = advance();
            Expression right = parseMulDiv();
            expr = new BinaryOp(expr, op, right);
        }
        return expr;
    }

    private Expression parseMulDiv() throws ParseException {
        Expression expr = parseUnary();
        while (check("*") || check("/") || check("%")) {
            Token op = advance();
            Expression right = parseUnary();
            expr = new BinaryOp(expr, op, right);
        }
        return expr;
    }

    private Expression parseUnary() throws ParseException {
        if (match("not") || match("-")) {
            Token op = previous();
            Expression right = parseUnary();
            return new UnaryOp(op, right);
        }
        return parsePrimary();
    }

    private Expression parsePrimary() throws ParseException {
        Token token = peek();
        if (token == null) {
            throw new ParseException("Unexpected end of input in expression", lastToken());
        }
        String lexeme = token.getLexeme();
        if (match("(")) {
            Expression expr = parseExpression();
            expect(")");
            return expr;
        }
        if (isLiteralToken(token)) {
            advance();
            return new Literal(token);
        }
        if (isIdentifier(token)) {
            Token identifier = advance();
            if (check("(")) {
                return finishFunctionCall(identifier);
            }
            return new VariableRef(identifier);
        }
        throw new ParseException("Unexpected token in expression: " + lexeme, token);
    }

    private boolean isLiteralToken(Token token) {
        if (token == null) {
            return false;
        }
        String lexeme = token.getLexeme();
        return lexeme.matches("\\d+") ||
                (lexeme.startsWith("\"") && lexeme.endsWith("\"")) ||
                (lexeme.startsWith("'") && lexeme.endsWith("'")) ||
                lexeme.equals("true") || lexeme.equals("false");
    }

    private boolean isIdentifier(Token token) {
        if (token == null) {
            return false;
        }
        String lexeme = token.getLexeme();
        if (lexeme.isEmpty() || KEYWORDS.contains(lexeme)) {
            return false;
        }
        return lexeme.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private Token consumeIdentifier(String context) throws ParseException {
        Token token = peek();
        if (!isIdentifier(token)) {
            throw new ParseException("Expected " + context + ", found '" + (token != null ? token.getLexeme() : "<eof>") + "'",
                    token != null ? token : lastToken());
        }
        position++;
        return token;
    }

    private boolean isAtEnd() {
        return position >= tokens.size();
    }

    private Token peek() {
        if (isAtEnd()) {
            return null;
        }
        return tokens.get(position);
    }

    private Token advance() throws ParseException {
        if (isAtEnd()) {
            throw new ParseException("Unexpected end of input", lastToken());
        }
        return tokens.get(position++);
    }

    private boolean match(String lexeme) {
        if (check(lexeme)) {
            position++;
            return true;
        }
        return false;
    }

    private boolean check(String lexeme) {
        Token token = peek();
        return token != null && token.getLexeme().equals(lexeme);
    }

    private Token expect(String lexeme) throws ParseException {
        Token token = peek();
        if (token == null || !token.getLexeme().equals(lexeme)) {
            throw new ParseException("Expected '" + lexeme + "'", token != null ? token : lastToken());
        }
        position++;
        return token;
    }

    private String lookAheadLexeme(int offset) {
        int index = position + offset;
        if (index >= tokens.size() || index < 0) {
            return null;
        }
        return tokens.get(index).getLexeme();
    }

    private Token previous() {
        return tokens.get(position - 1);
    }

    private void ensureEOF() throws ParseException {
        if (!isAtEnd()) {
            Token token = peek();
            throw new ParseException("Unexpected token after end of program: " + token.getLexeme(), token);
        }
    }

    private Token lastToken() {
        if (tokens.isEmpty()) {
            return new Token("<eof>", 0, 0);
        }
        return tokens.get(Math.max(0, Math.min(position, tokens.size() - 1)));
    }

    private boolean startsExpression() {
        Token token = peek();
        if (token == null) {
            return false;
        }
        String lexeme = token.getLexeme();
        return lexeme.equals("(") || lexeme.equals("not") || lexeme.equals("-") ||
                isLiteralToken(token) || isIdentifier(token);
    }

    private void ensureParenthesizedArithmeticInAssignment() throws ParseException {
        if (!check("(") && hasTopLevelArithmeticOperatorAhead()) {
            Token err = peek();
            throw new ParseException("Arithmetic expressions must be enclosed in parentheses",
                    err != null ? err : lastToken());
        }
    }

    private boolean hasTopLevelArithmeticOperatorAhead() {
        int depth = 0;
        for (int i = position; i < tokens.size(); i++) {
            String lexeme = tokens.get(i).getLexeme();
            if (lexeme.equals("(")) {
                depth++;
            } else if (lexeme.equals(")")) {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && isArithmeticOperator(lexeme)) {
                return true;
            } else if (lexeme.equals(";") || lexeme.equals("end") || lexeme.equals("else")) {
                break;
            }
        }
        return false;
    }

    private boolean isArithmeticOperator(String lexeme) {
        return lexeme.equals("+") || lexeme.equals("-") || lexeme.equals("*") ||
                lexeme.equals("/") || lexeme.equals("%");
    }

    private boolean isParenthesizedStringLiteral() {
        if (!check("(")) {
            return false;
        }
        if (position + 2 >= tokens.size()) {
            return false;
        }
        Token literal = tokens.get(position + 1);
        Token closing = tokens.get(position + 2);
        return isStringLiteral(literal) && ")".equals(closing.getLexeme());
    }

    private boolean isStringLiteral(Token token) {
        if (token == null) {
            return false;
        }
        String lexeme = token.getLexeme();
        return lexeme.startsWith("\"") && lexeme.endsWith("\"");
    }
}
