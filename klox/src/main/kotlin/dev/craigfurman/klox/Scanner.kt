package dev.craigfurman.klox

import dev.craigfurman.klox.TokenType.*

class Scanner(private val src: String, private val reportError: ReportError) {
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
        val tkn = when (c) {
            '(' -> newToken(LEFT_PAREN)
            ')' -> newToken(RIGHT_PAREN)
            '{' -> newToken(LEFT_BRACE)
            '}' -> newToken(RIGHT_BRACE)
            ',' -> newToken(COMMA)
            '.' -> newToken(DOT)
            '-' -> newToken(MINUS)
            '+' -> newToken(PLUS)
            ';' -> newToken(SEMICOLON)
            '*' -> newToken(STAR)

            '!' -> newToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> newToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> newToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> newToken(if (match('=')) GREATER_EQUAL else GREATER)

            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance()
                    }
                    null
                } else {
                    newToken(SLASH)
                }
            }

            ' ', '\r', '\t' -> null

            '\n' -> {
                line++
                null
            }

            '"' -> string()
            in '0'..'9' -> number()
            in 'a'..'z', in 'A'..'Z', '_' -> identifier()

            else -> {
                this.reportError(line, "Unexpected character: $c")
                null
            }
        }
        if (tkn != null) {
            tokens.add(tkn)
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

    private fun newToken(type: TokenType) = newToken(type, null)
    private fun newToken(type: TokenType, literal: Any?): Token {
        val text = src.substring(start until current)
        return Token(type, text, literal, line)
    }

    private fun string(): Token? {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
            }
            advance()
        }
        if (isAtEnd()) {
            reportError(line, "Unterminated string.")
            return null
        }
        advance() // consume the closing quote

        // trim the surrounding quotes
        val str = src.substring(start + 1 until current - 1)
        return newToken(STRING, str)
    }

    private fun number(): Token {
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
        return newToken(NUMBER, numStr.toDouble())
    }

    private fun identifier(): Token {
        while (isAlpha(peek())) {
            advance()
        }

        val text = src.substring(start until current)
        val keyword = keywords[text]
        return when (keyword) {
            null -> newToken(IDENTIFIER, text)
            else -> newToken(keyword)
        }
    }
}

private fun isDigit(c: Char) = c in '0'..'9'
private fun isAlpha(c: Char) = c in 'a'..'z' ||
        c in 'A'..'Z' ||
        c == '_'
