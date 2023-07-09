package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

class Scanner(
    private val src: String,
    private val errorReporter: ErrorReporter
) {
    private val tokens: MutableList<Token> = ArrayList()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)

            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)

            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance()
                    }
                } else {
                    addToken(SLASH)
                }
            }

            ' ', '\r', '\t' -> {
                // Ignore whitespace
            }

            '\n' -> {
                line++
            }

            '"' -> string()
            in '0'..'9' -> number()
            in 'a'..'z', in 'A'..'Z', '_' -> identifier()

            else -> {
                this.errorReporter.error(line, "Unexpected character: $c")
            }
        }
    }

    private fun advance() = src[current++]
    private fun isAtEnd() = current >= src.length

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) {
            return false
        }
        if (src[current] != expected) {
            return false
        }
        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) {
            return Char.MIN_VALUE
        }
        return src[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= src.length) {
            return Char.MIN_VALUE
        }
        return src[current + 1]
    }

    private fun addToken(type: TokenType) = addToken(type, null)
    private fun addToken(type: TokenType, literal: Any?) {
        val text = src.substring(start until current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
            }
            advance()
        }
        if (isAtEnd()) {
            this.errorReporter.error(line, "Unterminated string.")
            return
        }
        advance() // consume the closing quote

        // trim the surrounding quotes
        val str = src.substring(start + 1 until current - 1)
        addToken(STRING, str)
    }

    private fun number() {
        while (isDigit(peek())) {
            advance()
        }

        // all lox numbers are floating point at runtime, check for a decimal
        // point and handle this case
        if (peek() == '.' && isDigit(peekNext())) {
            advance() // consume the dot
            while (isDigit(peek())) {
                advance()
            }
        }

        val numStr = src.substring(start until current)
        addToken(NUMBER, numStr.toDouble())
    }

    private fun identifier() {
        while (isAlpha(peek())) {
            advance()
        }

        val text = src.substring(start until current)
        val keyword = keywords[text]
        return when (keyword) {
            null -> addToken(IDENTIFIER, text)
            else -> addToken(keyword)
        }
    }
}

private fun isDigit(c: Char) = c in '0'..'9'
private fun isAlpha(c: Char) = c in 'a'..'z' ||
        c in 'A'..'Z' ||
        c == '_'
