package dev.craigfurman.klox

class Resolver(private val interpreter: Interpreter, private val errorReporter: ErrorReporter) :
    Expression.Visitor<Unit>,
    Stmt.Visitor<Unit> {
    private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentLoop = LoopType.NONE

    fun resolve(statements: List<Stmt>) {
        for (stmt in statements) {
            resolve(stmt)
        }
    }

    override fun visitAssignExpr(expr: Expression.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expression.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expression.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGetExpr(expr: Expression.Get) {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expression.Grouping) {
        resolve(expr.expr)
    }

    override fun visitLiteralExpr(expr: Expression.Literal) {
    }

    override fun visitLogicalExpr(expr: Expression.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expression.SetExpr) {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expression.Super) {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expression.This) {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expression.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expression.Variable) {
        val inOwnInitializer = when (val initialized = scopes.lastOrNull()?.get(expr.name.lexeme)) {
            null -> false
            else -> !initialized
        }
        if (inOwnInitializer) {
            errorReporter.error(expr.name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitClassStmt(stmt: Stmt.ClassStmt) {
        TODO("Not yet implemented")
    }

    override fun visitExprStmt(stmt: Stmt.Expr) {
        resolve(stmt.expr)
    }

    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visitJumpStmt(stmt: Stmt.Jump) {
        if (currentLoop != LoopType.BREAKABLE) {
            errorReporter.error(stmt.keyword, "Can only break out of loops.")
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            errorReporter.error(stmt.keyword, "Can't return from top-level code.")
        }
        stmt.value?.let { resolve(it) }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        val enclosingLoop = currentLoop
        currentLoop = LoopType.BREAKABLE

        resolve(stmt.condition)
        resolve(stmt.body)

        currentLoop = enclosingLoop
    }

    private fun resolve(expr: Expression) = visit(expr)
    private fun resolve(stmt: Stmt) = visit(stmt)

    private fun beginScope() = scopes.addLast(HashMap())
    private fun endScope() = scopes.removeLast()

    private fun declare(name: Token) {
        val scope = scopes.lastOrNull() ?: return
        if (scope.containsKey(name.lexeme)) {
            errorReporter.error(name, "Already a variable with this name in this scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        val scope = scopes.lastOrNull() ?: return
        scope[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expression, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.FunctionStmt, funcType: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = funcType

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private enum class FunctionType {
        NONE, FUNCTION,
    }

    private enum class LoopType {
        NONE, BREAKABLE,
    }
}
