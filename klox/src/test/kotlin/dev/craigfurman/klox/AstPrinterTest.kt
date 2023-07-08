package dev.craigfurman.klox

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AstPrinterTest {
    @Test
    fun print() {
        val expr = Expression.Binary(
            Expression.Unary(
                Token(TokenType.MINUS, "-", null, 1),
                Expression.Literal(123),
            ),
            Token(TokenType.STAR, "*", null, 1),
            Expression.Grouping(Expression.Literal(45.67)),
        )
        val printer = AstPrinter()
        assertEquals("(* (- 123) (group 45.67))", printer.print(expr))
    }
}
