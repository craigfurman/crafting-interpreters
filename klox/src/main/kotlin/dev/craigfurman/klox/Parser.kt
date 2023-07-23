package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

// Program grammar:
//
// program        → declaration* EOF ;
// declaration    → funDecl
//                → varDecl
//                | statement ;
// funDecl        → "fun" function ;
// function       → IDENTIFIER "(" parameters? ")" block ;
// parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
// varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
// statement      → exprStmt
//                | forStmt
//                | ifStmt
//                | printStmt
//                | whileStmt
//                | breakStmt // Not always allowed, but I don't know how to express this without bloating the grammar
//                | block ;
// exprStmt       → expression ";" ;
// forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
//                 expression? ";"
//                 expression? ")" statement ;
// ifStmt         → "if" "(" expression ")" statement
//                ( "else" statement )? ;
// printStmt      → "print" expression ";" ;
// whileStmt      → "while" "(" expression ")" statement ;
// block          → "{" declaration* "}" ;

// Expression grammar:
//
// expression     → assignment ;
// assignment     → IDENTIFIER "=" assignment
//                | logic_or ;
// logic_or       → logic_and ( "or" logic_and )* ;
// logic_and      → equality ( "and" equality )* ;
// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// term           → factor ( ( "-" | "+" ) factor )* ;
// factor         → unary ( ( "/" | "*" ) unary )* ;
// unary          → ( "!" | "-" ) unary | call ;
// call           → primary ( "(" arguments? ")" )* ;
// arguments      → expression ( "," expression )* ;
// primary        → NUMBER | STRING | "true" | "false" | "nil"
//                | "(" expression ")" | IDENTIFIER;

class Parser(
    private val tokens: List<Token>,
    private val errorReporter: ErrorReporter
) {
    private var current = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(allowJumps: Boolean = false): Stmt? {
        try {
            if (match(FUN)) return function(FunctionKind.FUNCTION)
            if (match(VAR)) return varDeclaration()
            return statement(allowJumps)
        } catch (err: ParseError) {
            synchronize()
            return null
        }
    }

    private fun function(kind: FunctionKind): Stmt {
        val name = consume(IDENTIFIER, "Expect ${kind.name.lowercase()} name.")
        consume(LEFT_PAREN, "Expect '(' after ${kind.name.lowercase()} name.")
        val parameters = ArrayList<Token>()
        if (!currentTokenHasType(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    newError(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")
        consume(LEFT_BRACE, "Expect '{' before ${kind.name.lowercase()} body.")
        val body = block()
        return Stmt.FunctionStmt(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        var initializer: Expression? = null
        if (match(EQUAL)) {
            initializer = expression()
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(allowJumps: Boolean = false): Stmt {
        if (match(FOR)) return forStatement()
        if (match(IF)) return ifStatement(allowJumps)
        if (match(PRINT)) return printStatement()
        if (match(WHILE)) return whileStatement()
        if (match(LEFT_BRACE)) return Stmt.Block(block(allowJumps))
        if (allowJumps && match(BREAK)) return breakStatement()
        return exprStatement()
    }

    private fun exprStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Expr(value)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer =
            if (match(SEMICOLON)) null else if (match(VAR)) varDeclaration() else exprStatement()
        val condition =
            if (currentTokenHasType(SEMICOLON)) Expression.Literal(true) else expression()
        consume(SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (currentTokenHasType(RIGHT_PAREN)) null else expression()
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        // Desugar to build up the for statement with each clause being optional
        var body = statement(true)

        // Increment executes after each loop iteration
        if (increment != null) body = Stmt.Block(listOf(body, Stmt.Expr(increment)))

        // condition defaults to true above
        body = Stmt.While(condition, body)

        if (initializer != null) body = Stmt.Block(listOf(initializer, body))
        return body
    }

    private fun ifStatement(allowJumps: Boolean = false): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition")
        val thenBranch = statement(allowJumps)
        val elseBranch = if (match(ELSE)) statement(allowJumps) else null
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Print(value)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement(true)
        return Stmt.While(condition, body)
    }

    // Chapter 9 challenge 3
    // I tried adding continue too, but this was much harder to support in for loops due to the need
    // to still execute the increment statement. I might come back and add this later.
    private fun breakStatement(): Stmt {
        val jump = Stmt.Jump(previous())
        consume(SEMICOLON, "Expect ';' after 'break'.")
        return jump
    }

    private fun block(allowJumps: Boolean = false): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!currentTokenHasType(RIGHT_BRACE) && !isAtEnd()) {
            declaration(allowJumps)?.let { statements.add(it) }
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    // exposed for tests only
    internal fun expression(): Expression = assignment()

    private fun assignment(): Expression {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expression.Variable) {
                val name = expr.name
                return Expression.Assign(name, value)
            }

            newError(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or() = parseLeftAssociativeBinaryOperators(::and, Expression::Logical, OR)
    private fun and() = parseLeftAssociativeBinaryOperators(::equality, Expression::Logical, AND)

    private fun equality() = parseLeftAssociativeBinaryOperators(
        ::comparison, Expression::Binary, BANG_EQUAL, EQUAL_EQUAL
    )

    private fun comparison() = parseLeftAssociativeBinaryOperators(
        ::term, Expression::Binary, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL
    )

    private fun term() =
        parseLeftAssociativeBinaryOperators(::factor, Expression::Binary, MINUS, PLUS)

    private fun factor() =
        parseLeftAssociativeBinaryOperators(::unary, Expression::Binary, SLASH, STAR)

    private fun unary(): Expression {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expression.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expression {
        var expr = primary()
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments = ArrayList<Expression>()
        if (!currentTokenHasType(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    newError(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(COMMA))
        }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return Expression.Call(callee, paren, arguments)
    }

    private fun primary(): Expression {
        if (match(FALSE)) return Expression.Literal(false)
        if (match(TRUE)) return Expression.Literal(true)
        if (match(NIL)) return Expression.Literal(null)
        if (match(NUMBER, STRING)) return Expression.Literal(previous().literal)
        if (match(IDENTIFIER)) return Expression.Variable(previous())

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expression.Grouping(expr)
        }

        throw newError(peek(), "Expect expression.")
    }

    // Logical operators are binary operators, but we parse them as different, in order to more
    // easily implement short-circuiting logic
    private fun parseLeftAssociativeBinaryOperators(
        higherPrecedenceProduction: () -> Expression,
        exprConstructor: (left: Expression, operator: Token, right: Expression) -> Expression,
        vararg tokenTypes: TokenType,
    ): Expression {
        var expr = higherPrecedenceProduction()
        while (match(*tokenTypes)) {
            val operator = previous()
            val right = higherPrecedenceProduction()
            expr = exprConstructor(expr, operator, right)
        }
        return expr
    }

    private fun match(vararg tokenTypes: TokenType): Boolean {
        for (tokenType in tokenTypes) {
            if (currentTokenHasType(tokenType)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun currentTokenHasType(tokenType: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type == tokenType
    }

    private fun isAtEnd() = peek().type == EOF
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]

    private fun consume(tokenType: TokenType, message: String): Token {
        if (currentTokenHasType(tokenType)) {
            return advance()
        }
        throw newError(peek(), message)
    }

    private fun newError(token: Token, message: String): Exception {
        this.errorReporter.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }

    enum class FunctionKind { FUNCTION }

    class ParseError : Exception()
}
