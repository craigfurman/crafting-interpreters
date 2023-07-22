package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

class Interpreter(private val reportError: (RuntimeError) -> Unit) : Expression.Visitor<Any?>,
    Stmt.Visitor<Unit> {
    private var environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
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

    override fun visitCallExpr(expr: Expression.Call): Any {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Expression.Get): Any {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expression.Grouping) = evaluate(expr.expr)

    override fun visitLiteralExpr(expr: Expression.Literal) = expr.value

    override fun visitLogicalExpr(expr: Expression.Logical): Any {
        TODO("Not yet implemented")
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

    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt?) {
        TODO("Not yet implemented")
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        TODO("Not yet implemented")
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        TODO("Not yet implemented")
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = when (stmt.initializer) {
            null -> null
            else -> evaluate(stmt.initializer)
        }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        TODO("Not yet implemented")
    }

    private fun executeBlock(statements: List<Stmt>, environment: Environment) {
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
}

class RuntimeError(val token: Token, msg: String) : Exception(msg)
