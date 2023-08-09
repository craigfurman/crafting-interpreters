package lox

import "errors"

func parse(tokens []Token) Expr {
	p := &parser{tokens: tokens}
	return p.parse()
}

type parser struct {
	tokens  []Token
	current int
}

var ParseError = errors.New("parse error")

func (p *parser) parse() Expr {
	expr, err := p.expression()

	// TODO add synchronization
	must(err)

	return expr
}

// expressions

func (p *parser) expression() (Expr, error) {
	return p.termExpression()
}

func (p *parser) termExpression() (Expr, error) {
	expr, err := p.factorExpression()
	if err != nil {
		return nil, err
	}
	for p.match(TOKEN_PLUS, TOKEN_MINUS) {
		op := p.previous()
		right, err := p.factorExpression()
		if err != nil {
			return nil, err
		}
		expr = BinaryExpr{left: expr, operator: op, right: right}
	}
	return expr, nil
}

func (p *parser) factorExpression() (Expr, error) {
	expr, err := p.unaryExpression()
	if err != nil {
		return nil, err
	}
	for p.match(TOKEN_STAR, TOKEN_SLASH) {
		op := p.previous()
		right, err := p.unaryExpression()
		if err != nil {
			return nil, err
		}
		expr = BinaryExpr{left: expr, operator: op, right: right}
	}
	return expr, nil
}

func (p *parser) unaryExpression() (Expr, error) {
	if p.match(TOKEN_BANG, TOKEN_MINUS) {
		op := p.previous()
		right, err := p.unaryExpression()
		if err != nil {
			return nil, err
		}
		return UnaryExpr{operator: op, right: right}, nil
	}
	return p.primaryExpression()
}

func (p *parser) primaryExpression() (Expr, error) {
	if p.match(TOKEN_FALSE) {
		return LiteralExpr{false}, nil
	}
	if p.match(TOKEN_TRUE) {
		return LiteralExpr{true}, nil
	}
	if p.match(TOKEN_NIL) {
		return LiteralExpr{nil}, nil
	}
	if p.match(TOKEN_NUMBER, TOKEN_STRING) {
		return LiteralExpr{p.previous().literal}, nil
	}
	if p.match(TOKEN_LEFT_PAREN) {
		expr, err := p.expression()
		if err != nil {
			return nil, err
		}
		if _, err := p.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression."); err != nil {
			return nil, err
		}
		return GroupingExpr{expr}, nil
	}

	tokenError(p.peek(), "Expect expression.")
	return nil, ParseError
}

// helpers

func (p *parser) match(typs ...TokenType) bool {
	for _, typ := range typs {
		if p.currentIs(typ) {
			p.advance()
			return true
		}
	}
	return false
}

func (p parser) currentIs(typ TokenType) bool {
	if p.isAtEnd() {
		return false
	}
	return p.peek().typ == typ
}

func (p *parser) consume(typ TokenType, message string) (Token, error) {
	if p.currentIs(typ) {
		return p.advance(), nil
	}

	tokenError(p.peek(), message)
	return Token{}, ParseError
}

func (p *parser) advance() Token {
	if !p.isAtEnd() {
		p.current++
	}
	return p.previous()
}

func (p parser) peek() Token {
	return p.tokens[p.current]
}

func (p parser) previous() Token {
	return p.tokens[p.current-1]
}

func (p parser) isAtEnd() bool {
	return p.peek().typ == TOKEN_EOF
}
