package lox

import "errors"

func parse(tokens []Token) []Stmt {
	p := &parser{tokens: tokens}
	return p.parse()
}

type parser struct {
	tokens  []Token
	current int
}

var ParseError = errors.New("parse error")

func (p *parser) parse() []Stmt {
	var statements []Stmt
	for !p.isAtEnd() {
		stmt, err := p.declaration()
		if err != nil {
			if errors.Is(err, ParseError) {
				p.synchronize()
				continue
			}
		}
		statements = append(statements, stmt)
	}
	return statements
}

// statements

func (p *parser) declaration() (Stmt, error) {
	if p.match(TOKEN_VAR) {
		return p.varDecl()
	}
	return p.statement()
}

func (p *parser) varDecl() (Stmt, error) {
	name, err := p.consume(TOKEN_IDENTIFIER, "Expect variable name.")
	if err != nil {
		return nil, err
	}
	var initializer Expr = nil
	if p.match(TOKEN_EQUAL) {
		initializer, err = p.expression()
		if err != nil {
			return nil, err
		}
	}
	if _, err := p.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration."); err != nil {
		return nil, err
	}
	return VarStmt{name: name, initializer: initializer}, nil
}

func (p *parser) statement() (Stmt, error) {
	if p.match(TOKEN_PRINT) {
		return p.printStmt()
	}

	return p.exprStmt()
}

func (p *parser) exprStmt() (Stmt, error) {
	expr, err := p.expression()
	if err != nil {
		return nil, err
	}
	if _, err := p.consume(TOKEN_SEMICOLON, "Expect ';' after expression."); err != nil {
		return nil, err
	}
	return ExprStmt{expr}, nil
}

func (p *parser) printStmt() (Stmt, error) {
	value, err := p.expression()
	if err != nil {
		return nil, err
	}
	if _, err := p.consume(TOKEN_SEMICOLON, "Expect ';' after expression."); err != nil {
		return nil, err
	}
	return PrintStmt{value}, nil
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
	if p.match(TOKEN_IDENTIFIER) {
		return VarExpr{p.previous()}, nil
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

// error recovery
var startOfDeclTypes = []TokenType{TOKEN_CLASS, TOKEN_FUN, TOKEN_VAR, TOKEN_FOR, TOKEN_IF, TOKEN_WHILE, TOKEN_PRINT, TOKEN_RETURN}

func (p *parser) synchronize() {
	p.advance()
	for !p.isAtEnd() {
		if p.previous().typ == TOKEN_SEMICOLON {
			return
		}
		if contains(startOfDeclTypes, p.peek().typ) {
			return
		}
		p.advance()
	}
}
