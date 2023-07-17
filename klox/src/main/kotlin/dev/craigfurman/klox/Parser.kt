package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

// Program grammar:
//
// program        → statement* EOF ;
//
// statement      → exprStmt
//                | printStmt ;
//
// exprStmt       → expression ";" ;
// printStmt      → "print" expression ";" ;

// Expression grammar:
//
// expression     → equality ;
// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// term           → factor ( ( "-" | "+" ) factor )* ;
// factor         → unary ( ( "/" | "*" ) unary )* ;
// unary          → ( "!" | "-" ) unary
//                | primary ;
// primary        → NUMBER | STRING | "true" | "false" | "nil"
//                | "(" expression ")" ;

class Parser(
    private val tokens: List<Token>,
    private val errorReporter: ErrorReporter
) {
    private var current = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            statements.add(statement())
        }
        return statements
    }

    private fun statement(): Stmt {
        if (match(PRINT)) {
            return printStatement()
        }
        return exprStatement()
    }

    private fun exprStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Expr(value)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Print(value)
    }

    // exposed for tests only
    internal fun expression(): Expression = equality()

    private fun equality() = parseLeftAssociativeBinaryOperators(
        ::comparison, BANG_EQUAL, EQUAL_EQUAL
    )

    private fun comparison() = parseLeftAssociativeBinaryOperators(
        ::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL
    )

    private fun term() =
        parseLeftAssociativeBinaryOperators(::factor, MINUS, PLUS)

    private fun factor() =
        parseLeftAssociativeBinaryOperators(::unary, SLASH, STAR)

    private fun unary(): Expression {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expression.Unary(operator, right)
        }
        return primary()
    }

    private fun primary(): Expression {
        if (match(FALSE)) return Expression.Literal(false)
        if (match(TRUE)) return Expression.Literal(true)
        if (match(NIL)) return Expression.Literal(null)
        if (match(NUMBER, STRING)) return Expression.Literal(previous().literal)

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expression.Grouping(expr)
        }

        throw newError(peek(), "Expect expression.")
    }

    private fun parseLeftAssociativeBinaryOperators(
        leftRule: () -> Expression,
        vararg tokenTypes: TokenType
    ): Expression {
        var expr = leftRule()
        while (match(*tokenTypes)) {
            val operator = previous()
            val right = leftRule()
            expr = Expression.Binary(expr, operator, right)
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

    class ParseError : Exception()
}
