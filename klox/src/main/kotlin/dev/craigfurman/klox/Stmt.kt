package dev.craigfurman.klox

sealed interface Stmt {
    data class Block(val statements: List<Stmt>) : Stmt
    data class ClassStmt(
        val name: Token,
        val superclass: Expression.Variable,
        val methods: List<FunctionStmt>
    ) : Stmt

    data class Expr(val expr: Expression) : Stmt
    data class FunctionStmt(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt
    data class If(val condition: Expression, val thenBranch: Stmt, val elseBranch: Stmt) : Stmt
    data class Print(val expr: Expression) : Stmt
    data class Return(val keyword: Token, val value: Expression) : Stmt
    data class Var(val name: Token, val initializer: Expression) : Stmt
    data class While(val condition: Expression, val body: Stmt) : Stmt

    interface Visitor<R> {
        fun visitBlockStmt(stmt: Block): R
        fun visitClassStmt(stmt: ClassStmt): R
        fun visitExprStmt(stmt: Expr): R
        fun visitFunctionStmt(stmt: FunctionStmt?): R
        fun visitIfStmt(stmt: If): R
        fun visitPrintStmt(stmt: Print): R
        fun visitReturnStmt(stmt: Return): R
        fun visitVarStmt(stmt: Var): R
        fun visitWhileStmt(stmt: While): R

        fun visit(stmt: Stmt) = when (stmt) {
            is Block -> visitBlockStmt(stmt)
            is ClassStmt -> visitClassStmt(stmt)
            is Expr -> visitExprStmt(stmt)
            is FunctionStmt -> visitFunctionStmt(stmt)
            is If -> visitIfStmt(stmt)
            is Print -> visitPrintStmt(stmt)
            is Return -> visitReturnStmt(stmt)
            is Var -> visitVarStmt(stmt)
            is While -> visitWhileStmt(stmt)
        }
    }
}
