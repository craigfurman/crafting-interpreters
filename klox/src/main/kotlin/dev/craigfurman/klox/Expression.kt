package dev.craigfurman.klox

sealed interface Expression {
    data class Assign(val name: Token, val value: Expression) : Expression

    data class Binary(
        val left: Expression,
        val operator: Token,
        val right: Expression
    ) : Expression

    data class Call(
        val callee: Expression,
        val paren: Token,
        val arguments: List<Expression>
    ) : Expression

    data class Get(val obj: Expression, val name: Token) : Expression
    data class Grouping(val expr: Expression) : Expression
    data class Literal(val value: Any?) : Expression

    data class Logical(
        val left: Expression,
        val operator: Token,
        val right: Expression
    ) : Expression

    data class SetExpr(
        val obj: Expression,
        val name: Token,
        val value: Expression
    ) : Expression

    data class Super(val keyword: Token, val method: Token) : Expression
    data class This(val keyword: Token) : Expression
    data class Unary(val operator: Token, val right: Expression) : Expression
    data class Variable(val name: Token) : Expression

    // The book uses a more-traditional visitor pattern, with an accept(Visitor)
    // method defined on the Expression interface. It seemed a shame to generate
    // all the boilerplate for that, and I wanted to try when...is statements
    // with this sealed class. I might measure the performance and see how it
    // compares to the proper visitor pattern later.
    interface Visitor<R> {
        fun visitAssignExpr(expr: Assign): R
        fun visitBinaryExpr(expr: Binary): R
        fun visitCallExpr(expr: Call): R
        fun visitGetExpr(expr: Get): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitSetExpr(expr: SetExpr): R
        fun visitSuperExpr(expr: Super): R
        fun visitThisExpr(expr: This): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R

        fun visit(expr: Expression) = when (expr) {
            is Assign -> visitAssignExpr(expr)
            is Binary -> visitBinaryExpr(expr)
            is Call -> visitCallExpr(expr)
            is Get -> visitGetExpr(expr)
            is Grouping -> visitGroupingExpr(expr)
            is Literal -> visitLiteralExpr(expr)
            is Logical -> visitLogicalExpr(expr)
            is SetExpr -> visitSetExpr(expr)
            is Super -> visitSuperExpr(expr)
            is This -> visitThisExpr(expr)
            is Unary -> visitUnaryExpr(expr)
            is Variable -> visitVariableExpr(expr)
        }
    }
}
