package dev.craigfurman.klox


sealed interface Stmt {
    data class Expr(val expr: Expression) : Stmt
    data class Print(val expr: Expression) : Stmt

    interface Visitor<R> {
        fun visitExprStmt(stmt: Expr): R
        fun visitPrintStmt(stmt: Print): R

        fun visit(stmt: Stmt) = when (stmt) {
            is Expr -> visitExprStmt(stmt)
            is Print -> visitPrintStmt(stmt)
        }
    }
}
