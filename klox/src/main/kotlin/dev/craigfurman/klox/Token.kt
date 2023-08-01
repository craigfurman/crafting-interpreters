package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
)

enum class TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, DOT, MINUS, PIPE, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens
    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, BREAK, CLASS, ELSE, FALSE, FUN, FOR, IF, IN, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
}

val keywords = arrayOf(
    AND, BREAK, CLASS, ELSE, FALSE, FUN, FOR, IF, IN, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
).associateBy { it.name.lowercase() }
