package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

class Interpreter(
    private val replSession: Boolean,
    private val reportError: (RuntimeError) -> Unit
) :
    Expression.Visitor<Any?>,
    Stmt.Visitor<Unit> {
    internal val globals = Environment()
    private var environment = globals

    init {
        globals.define("clock", object : LoxCallable {
            override fun arity() = 0

            override fun call(interpreter: Interpreter, arguments: List<Any?>) =
                System.currentTimeMillis() / 1000.0

            override fun toString() = "<native fn>"
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                if (replSession && statement is Stmt.Expr) {
                    val evaluated = evaluate(statement.expr)
                    println(stringify(evaluated))
                }
                execute(statement)
            }
        } catch (err: RuntimeError) {
            reportError(err)
        }
    }

    override fun visitAssignExpr(expr: Expression.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitBinaryExpr(expr: Expression.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            MINUS -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum - rightNum
            }

            PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }
                if (left is String && right is String) {
                    return left + right
                }
                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }

            SLASH -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum / rightNum
            }

            STAR -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum * rightNum
            }

            GREATER -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum > rightNum
            }

            GREATER_EQUAL -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum >= rightNum
            }

            LESS -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum < rightNum
            }

            LESS_EQUAL -> {
                val (leftNum, rightNum) = checkNumberOperands(expr.operator, left, right)
                return leftNum <= rightNum
            }

            BANG_EQUAL -> left != right
            EQUAL_EQUAL -> left == right
            else -> throw Exception("unexpected binary operator: ${expr.operator.type}")
        }
    }

    override fun visitCallExpr(expr: Expression.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = ArrayList<Any?>()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
        if (arguments.size != callee.arity()) {
            throw RuntimeError(
                expr.paren,
                "Expected ${callee.arity()} arguments but got ${arguments.size}."
            )
        }

        return callee.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expression.Get): Any {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expression.Grouping) = evaluate(expr.expr)

    override fun visitLiteralExpr(expr: Expression.Literal) = expr.value

    override fun visitLogicalExpr(expr: Expression.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitSetExpr(expr: Expression.SetExpr): Any {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expression.Super): Any {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expression.This): Any {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expression.Unary): Any {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> -checkNumberOperand(expr.operator, right)
            else -> throw Exception("unexpected unary operator: ${expr.operator.type}")
        }
    }

    override fun visitVariableExpr(expr: Expression.Variable): Any? {
        return environment.get(expr.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitClassStmt(stmt: Stmt.ClassStmt) {
        TODO("Not yet implemented")
    }

    override fun visitExprStmt(stmt: Stmt.Expr) {
        evaluate(stmt.expr)
    }

    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt) {
        val function = LoxFunction(stmt)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitJumpStmt(stmt: Stmt.Jump) {
        throw Jump(stmt.keyword)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = if (stmt.value == null) null else evaluate(stmt.value)
        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = when (stmt.initializer) {
            null -> null
            else -> evaluate(stmt.initializer)
        }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body)
            } catch (jump: Jump) {
                if (jump.keyword.type == BREAK) break
                // TODO: restrict creation of Jumps to avoid this case
                else throw RuntimeError(jump.keyword, "Parser error: only 'break' may jump.")
            }
        }
    }

    internal fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            null -> false
            else -> true
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?): Double =
        when (val maybeNumber = operand as? Double) {
            null -> throw RuntimeError(operator, "Operand must be a number.")
            else -> maybeNumber
        }

    private fun checkNumberOperands(
        operator: Token,
        left: Any?,
        right: Any?
    ): Pair<Double, Double> {
        if (left is Double && right is Double) {
            return Pair(left, right)
        }
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(value: Any?): String {
        if (value == null) {
            return "nil"
        }
        if (value is Double) {
            var text = value.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }
        return value.toString()
    }

    private fun evaluate(expr: Expression) = visit(expr)
    private fun execute(statement: Stmt) = visit(statement)

    // We're just using these for control flow, no need for stack traces or suppression
    class Return(val value: Any?) : Exception(null, null, false, false)
    class Jump(val keyword: Token) : Exception(null, null, false, false)
}

class RuntimeError(val token: Token, msg: String) : Exception(msg)
