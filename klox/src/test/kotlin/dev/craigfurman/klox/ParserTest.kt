package dev.craigfurman.klox

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun `factors have higher precedence than terms`() {
        assertEquals("(+ 1.0 (* 2.0 3.0))", srcToExpr("1 + 2 * 3"))
        assertEquals("(+ (* 2.0 3.0) 1.0)", srcToExpr("2 * 3 + 1"))
    }

    @Test
    fun `factors are left-associative`() {
        assertEquals("(/ (* 2.0 3.0) 4.0)", srcToExpr("2 * 3 / 4"))
    }

    @Test
    fun `equality is left-associative and has lower precedence than terms`() {
        assertEquals(
            "(== (== (+ 2.0 2.0) 4.0) true)",
            srcToExpr("2 + 2 == 4 == true")
        )
    }

    private fun srcToExpr(src: String): String {
        val tokens = Scanner(src, panicOnError).scanTokens()
        val expr = Parser(tokens, panicOnError).parse()
        return AstPrinter().print(expr!!)
    }
}
