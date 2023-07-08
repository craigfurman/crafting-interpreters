package dev.craigfurman.klox

// Only used for tyre-kicking
class AstPrinter : Expression.Visitor<String> {
    fun print(expr: Expression) = visit(expr)

    private fun parenthesize(name: String, vararg exprs: Expression): String {
        val sb = StringBuilder()
        sb.append("(").append(name)
        for (expr in exprs) {
            sb.append(" ")
            sb.append(print(expr))
        }
        sb.append(")")
        return sb.toString()
    }

    override fun visitAssignExpr(expr: Expression.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(expr: Expression.Binary) =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitCallExpr(expr: Expression.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Expression.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expression.Grouping) =
        parenthesize("group", expr.expr)

    override fun visitLiteralExpr(expr: Expression.Literal) =
        when (expr.value) {
            null -> "nil"
            else -> expr.value.toString()
        }

    override fun visitLogicalExpr(expr: Expression.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(expr: Expression.SetExpr): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expression.Super): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expression.This): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expression.Unary) =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitVariableExpr(expr: Expression.Variable): String {
        TODO("Not yet implemented")
    }
}
