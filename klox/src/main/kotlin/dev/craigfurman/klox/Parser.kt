package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

// Program grammar:
//
// program        → declaration* EOF ;
// declaration    → classDecl
//                | funDecl
//                | varDecl
//                | statement ;
// classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
// funDecl        → "fun" function ;
// function       → IDENTIFIER "(" parameters? ")" block ;
// parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
// varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
// statement      → exprStmt
//                | forStmt
//                | ifStmt
//                | printStmt
//                | returnStmt
//                | whileStmt
//                | breakStmt
//                | block ;
// exprStmt       → expression ";" ;
// forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
//                 expression? ";"
//                 expression? ")" statement ;
// ifStmt         → "if" "(" expression ")" statement
//                ( "else" statement )? ;
// printStmt      → "print" expression ";" ;
// returnStmt     → "return" expression? ";" ;
// whileStmt      → "while" "(" expression ")" statement ;
// block          → "{" declaration* "}" ;

// Expression grammar:
//
// expression     → assignment ;
// assignment     → ( call "." )? IDENTIFIER "=" assignment
//                | pipe ;
// pipe           → logic_or ( "|" call )* // I went off-book, not sure if this is the right grammar
// logic_or       → logic_and ( "or" logic_and )* ;
// logic_and      → equality ( "and" equality )* ;
// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// term           → factor ( ( "-" | "+" ) factor )* ;
// factor         → unary ( ( "/" | "*" ) unary )* ;
// unary          → ( "!" | "-" ) unary | call ;
// call           → access ( "(" arguments? ")" | "." IDENTIFIER )* ;
// access         → access ( "[" expression "]" )* | primary
// arguments      → expression ( "," expression )* ;
// primary        → NUMBER | STRING | "true" | "false" | "nil"
//                | "(" expression ")" | IDENTIFIER | list | "super" "." IDENTIFIER ;
// list           → "[" ( expression "," )* "]"

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

    private fun declaration(): Stmt? {
        try {
            if (match(CLASS)) return classDeclaration()
            if (match(FUN)) return function(FunctionKind.FUNCTION)
            if (match(VAR)) return varDeclaration()
            return statement()
        } catch (err: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect class name.")

        var superClass: Expression.Variable? = null
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.")
            superClass = Expression.Variable(previous())
        }

        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = ArrayList<Stmt.FunctionStmt>()
        while (!currentTokenHasType(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function(FunctionKind.METHOD))
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.ClassStmt(name, superClass, methods)
    }

    private fun function(kind: FunctionKind): Stmt.FunctionStmt {
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

    private fun statement(): Stmt {
        if (match(FOR)) return forStatement()
        if (match(IF)) return ifStatement()
        if (match(PRINT)) return printStatement()
        if (match(RETURN)) return returnStatement()
        if (match(WHILE)) return whileStatement()
        if (match(BREAK)) return breakStatement()
        if (match(LEFT_BRACE)) return Stmt.Block(block())
        return exprStatement()
    }

    private fun exprStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expr(value)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        if (lookAhead(IN, RIGHT_PAREN)) {
            return forInListStatement()
        }

        val initializer =
            if (match(SEMICOLON)) null else if (match(VAR)) varDeclaration() else exprStatement()
        val condition =
            if (currentTokenHasType(SEMICOLON)) Expression.Literal(true) else expression()
        consume(SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (currentTokenHasType(RIGHT_PAREN)) null else expression()
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        // Desugar to build up the for statement with each clause being optional
        var body = statement()

        // Increment executes after each loop iteration
        if (increment != null) body = Stmt.Block(listOf(body, Stmt.Expr(increment)))

        // condition defaults to true above
        body = Stmt.While(condition, body)

        if (initializer != null) body = Stmt.Block(listOf(initializer, body))
        return body
    }

    // desugar for-in to while loop
    // for (e in list) Stmt;
    // ->
    // {
    //     var i = 0;
    //     while (i < list.length()) {
    //         var e = list.get(i);
    //         stmt;
    //         i = i + 1;
    //     }
    // }
    private fun forInListStatement(): Stmt {
        val iter = consume(IDENTIFIER, "Expect for-in loop to start with an identifier.")

        consume(IN, "Expected for-in loop.")
        val list = expression()
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")
        val body = statement()

        // Index initialisation
        // Use an illegal token name to avoid collision
        val idxToken = Token(IDENTIFIER, "\$i", "\$i", iter.line)
        val initializer = Stmt.Var(idxToken, Expression.Literal(0.0))

        // Iter variable initialisation
        val tokenParen = Token(LEFT_PAREN, "(", "(", iter.line)
        val getMethod = Token(IDENTIFIER, "get", "get", iter.line)
        val getListElement =
            Expression.Call(
                Expression.Get(list, getMethod),
                tokenParen,
                listOf(Expression.Variable(idxToken))
            )

        // Condition
        val tokenLT = Token(LESS, "<", "<", iter.line)
        val lengthMethod = Token(IDENTIFIER, "length", "length", iter.line)
        val condition = Expression.Binary(
            Expression.Variable(idxToken),
            tokenLT,
            Expression.Call(Expression.Get(list, lengthMethod), tokenParen, listOf()),
        )

        // Increment
        val tokenPlus = Token(PLUS, "+", "+", iter.line)
        val increment = Stmt.Expr(
            Expression.Assign(
                idxToken,
                Expression.Binary(Expression.Variable(idxToken), tokenPlus, Expression.Literal(1.0))
            )
        )

        return Stmt.Block(
            listOf(
                initializer,
                Stmt.While(
                    condition,
                    Stmt.Block(listOf(Stmt.Var(iter, getListElement), body, increment))
                )
            )
        )
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition")
        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (currentTokenHasType(SEMICOLON)) null else expression()
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()
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

    private fun block(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!currentTokenHasType(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    // exposed for tests only
    internal fun expression(): Expression = assignment()

    private fun assignment(): Expression {
        val expr = or()

        if (match(PIPE)) {
            return finishPipe(expr)
        } else if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expression.Variable) {
                val name = expr.name
                return Expression.Assign(name, value)
            } else if (expr is Expression.Get) {
                return Expression.SetExpr(expr.obj, expr.name, value)
            }

            newError(equals, "Invalid assignment target.")
        }

        return expr
    }

    // desugar pipe expressions into chains of function calls
    private fun finishPipe(expr: Expression): Expression {
        val token = previous()
        var pipe = expr
        do {
            val fn = call()
            if (fn !is Expression.Call) {
                errorReporter.error(token, "Expected valid pipeline.")
                return pipe
            }
            pipe = Expression.Call(fn, fn.paren, listOf(pipe))
        } while (match(PIPE))
        return pipe
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
        var expr = access()
        while (true) {
            expr = if (match(LEFT_PAREN)) {
                finishCall(expr)
            } else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                Expression.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    // desugar list access
    private fun access(): Expression {
        var expr = primary()
        while (match(LEFT_BRACKET)) {
            val bracket = previous()
            val idx = expression()
            consume(RIGHT_BRACKET, "Expect ']' after list access.")
            expr = Expression.Call(
                Expression.Get(expr, Token(IDENTIFIER, "get", "get", bracket.line)),
                bracket,
                listOf(idx),
            )
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

        if (match(SUPER)) {
            val keyword = previous()
            consume(DOT, "Expect '.' after 'super'.")
            val method = consume(IDENTIFIER, "Expect superclass method name.")
            return Expression.Super(keyword, method)
        }

        if (match(THIS)) return Expression.This(previous())
        if (match(IDENTIFIER)) return Expression.Variable(previous())

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expression.Grouping(expr)
        }

        // List literal
        if (match(LEFT_BRACKET)) {
            val bracket = previous()
            val elements = ArrayList<Expression>()
            while (!currentTokenHasType(RIGHT_BRACKET) && !isAtEnd()) {
                elements.add(expression())
                if (!currentTokenHasType(RIGHT_BRACKET)) {
                    consume(COMMA, "Expect ',' between list elements.")
                }
            }
            consume(RIGHT_BRACKET, "Expect ']' after list literal.")
            return Expression.ListExpr(bracket, elements)
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

    // This is not in the book. It seems a shame to introduce this relatively ugly function, but
    // I couldn't figure out how to introduce for-in loops without it.
    private fun lookAhead(target: TokenType, end: TokenType): Boolean {
        var i = 0;
        while (true) {
            if (isAtEnd()) return false
            val token = tokens[current + i]
            if (token.type == end) return false
            if (token.type == target) return true
            i++
        }
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

    enum class FunctionKind { FUNCTION, METHOD }

    class ParseError : Exception()
}
