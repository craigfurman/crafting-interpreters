package lox

import (
	"fmt"
	"strconv"
	"unicode"
)

func scan(source string) []Token {
	sc := &scanner{src: []rune(source), line: 1}
	return sc.scan()
}

type scanner struct {
	src            []rune
	start, current int
	line           int
	tokens         []Token
}

func (sc *scanner) scan() []Token {
	for !sc.isAtEnd() {
		sc.start = sc.current
		sc.scanOne()
	}
	sc.makeToken(TOKEN_EOF, nil)
	return sc.tokens
}

func (sc *scanner) scanOne() {
	c := sc.advance()
	switch c {
	case '(':
		sc.makeToken(TOKEN_LEFT_PAREN, nil)
	case ')':
		sc.makeToken(TOKEN_RIGHT_PAREN, nil)
	case '{':
		sc.makeToken(TOKEN_LEFT_BRACE, nil)
	case '}':
		sc.makeToken(TOKEN_RIGHT_BRACE, nil)
	case ',':
		sc.makeToken(TOKEN_COMMA, nil)
	case '.':
		sc.makeToken(TOKEN_DOT, nil)
	case '-':
		sc.makeToken(TOKEN_MINUS, nil)
	case '+':
		sc.makeToken(TOKEN_PLUS, nil)
	case ';':
		sc.makeToken(TOKEN_SEMICOLON, nil)
	case '*':
		sc.makeToken(TOKEN_STAR, nil)

	case '!':
		if sc.match('=') {
			sc.makeToken(TOKEN_BANG_EQUAL, nil)
		} else {
			sc.makeToken(TOKEN_BANG, nil)
		}
	case '=':
		if sc.match('=') {
			sc.makeToken(TOKEN_EQUAL_EQUAL, nil)
		} else {
			sc.makeToken(TOKEN_EQUAL, nil)
		}
	case '<':
		if sc.match('=') {
			sc.makeToken(TOKEN_LESS_EQUAL, nil)
		} else {
			sc.makeToken(TOKEN_LESS, nil)
		}
	case '>':
		if sc.match('=') {
			sc.makeToken(TOKEN_GREATER_EQUAL, nil)
		} else {
			sc.makeToken(TOKEN_GREATER, nil)
		}

	case '/':
		// Consume line comments
		if sc.match('/') {
			for sc.peek() != '\n' && !sc.isAtEnd() {
				sc.advance()
			}
		} else {
			sc.makeToken(TOKEN_SLASH, nil)
		}

		// Ignore whitspace
	case ' ', '\r', '\t':
	case '\n':
		sc.line++

	case '"':
		sc.scanString()

	default:
		if unicode.IsDigit(c) {
			sc.scanNumber()
			return
		}
		if isIdentifierStartingChar(c) {
			sc.scanIdentifier()
			return
		}

		reportError(sc.line, "", fmt.Sprintf("Unexpected character: %c", c))
	}
}

func (sc *scanner) scanString() {
	for sc.peek() != '"' && !sc.isAtEnd() {

		// Support multi-line strings for free, but remember to increment line
		if sc.peek() == '\n' {
			sc.line++
		}

		sc.advance()
	}
	if sc.isAtEnd() {
		reportError(sc.line, "", "Unterminated string.")
		return
	}

	sc.advance() // consume the closing quote

	// Trim the quotes
	value := string(sc.src[sc.start+1 : sc.current-1])
	sc.makeToken(TOKEN_STRING, value)
}

// All lox numbers are float64s, so scan one optional dot followed by more
// numbers. Any trailing tokens will be picked up later as parser errors.
func (sc *scanner) scanNumber() {
	for unicode.IsDigit(sc.peek()) {
		sc.advance()
	}

	if sc.peek() == '.' && unicode.IsDigit(sc.peekNext()) {
		sc.advance() // consume the dot
		for unicode.IsDigit(sc.peek()) {
			sc.advance()
		}
	}

	value, err := strconv.ParseFloat(string(sc.src[sc.start:sc.current]), 64)
	must(err)
	sc.makeToken(TOKEN_NUMBER, value)
}

func (sc *scanner) scanIdentifier() {
	for isIdentifierStartingChar(sc.peek()) || unicode.IsDigit(sc.peek()) {
		sc.advance()
	}

	id := string(sc.src[sc.start:sc.current])
	if typ, ok := keywords[id]; ok {
		sc.makeToken(typ, nil)
		return
	}
	sc.makeToken(TOKEN_IDENTIFIER, id)
}

func (sc *scanner) makeToken(typ TokenType, literal any) {
	tkn := Token{
		typ:     typ,
		lexeme:  string(sc.src[sc.start:sc.current]),
		literal: literal,
		line:    sc.line,
	}
	sc.tokens = append(sc.tokens, tkn)
}

func (sc *scanner) match(expected rune) bool {
	if sc.isAtEnd() {
		return false
	}
	if sc.src[sc.current] != expected {
		return false
	}
	sc.current++
	return true
}

func (sc *scanner) peek() rune {
	if sc.isAtEnd() {
		return 0
	}
	return sc.src[sc.current]
}

func (sc *scanner) peekNext() rune {
	if sc.current+1 >= len(sc.src) {
		return 0
	}
	return sc.src[sc.current+1]
}

func (sc *scanner) advance() rune {
	c := sc.peek()
	sc.current++
	return c
}

func (sc *scanner) isAtEnd() bool {
	return sc.current >= len(sc.src)
}

func isIdentifierStartingChar(c rune) bool {
	return unicode.IsLetter(c) || c == '_'
}
