package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

val expectedResults = listOf(
    "var aNum = 2;" to listOf(
        Token(VAR, "var", null, 1),
        Token(IDENTIFIER, "aNum", "aNum", 1),
        Token(EQUAL, "=", null, 1),
        Token(NUMBER, "2", 2.0, 1),
        Token(SEMICOLON, ";", null, 1),
        Token(EOF, "", null, 1),
    ),
).toMap()

class ScannerTest {
    @ParameterizedTest
    @MethodSource("expressions")
    fun scanTokens(src: String) {
        val expectedTokens = expectedResults[src]!!
        val sc = Scanner(src, fun(_: Int, s: String) {
            throw Exception(s)
        })
        val tokens = sc.scanTokens()
        assertEquals(expectedTokens, tokens)
    }

    companion object {
        @JvmStatic
        fun expressions() = expectedResults.keys.toList()
    }
}
