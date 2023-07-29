package dev.craigfurman.klox

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class KotlinExperimentsTest {
    @Test
    fun `identical expressions can make for different map keys`() {
        val expr1 = Expression.Literal(4)
        val expr2 = Expression.Literal(4)
        val map = IdentityHashMap<Expression, Int>()
        map[expr1] = 1
        map[expr2] = 2
        map[expr2] = 3
        assertEquals(2, map.size)
    }
}
