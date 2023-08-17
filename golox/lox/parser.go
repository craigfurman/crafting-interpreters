package lox

import (
	"errors"
	"fmt"
)

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
	if p.match(TOKEN_FUN) {
		return p.function("function")
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

func (p *parser) function(fnKind string) (Stmt, error) {
	name, err := p.consume(TOKEN_IDENTIFIER, fmt.Sprintf("Expect %s name.", fnKind))
	if err != nil {
		return nil, err
	}

	if _, err := p.consume(TOKEN_LEFT_PAREN, fmt.Sprintf("Expect '(' after %s name.", fnKind)); err != nil {
		return nil, err
	}
	var params []Token
	if !p.currentIs(TOKEN_RIGHT_PAREN) {
		for do := true; do; do = p.match(TOKEN_COMMA) {
			param, err := p.consume(TOKEN_IDENTIFIER, "Expect parameter name.")
			if err != nil {
				return nil, err
			}
			params = append(params, param)
		}
	}
	if _, err := p.consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters."); err != nil {
		return nil, err
	}

	if _, err := p.consume(TOKEN_LEFT_BRACE, fmt.Sprintf("Expect '{' before %s body.", fnKind)); err != nil {
		return nil, err
	}
	body, err := p.block()
	if err != nil {
		return nil, err
	}

	return FuncStmt{name: name, params: params, body: body}, nil
}

func (p *parser) statement() (Stmt, error) {
	if p.match(TOKEN_IF) {
		return p.ifStmt()
	}
	if p.match(TOKEN_PRINT) {
		return p.printStmt()
	}
	if p.match(TOKEN_RETURN) {
		return p.returnStmt()
	}
	if p.match(TOKEN_WHILE) {
		return p.whileStmt()
	}
	if p.match(TOKEN_LEFT_BRACE) {
		stmts, err := p.block()
		if err != nil {
			return nil, err
		}
		return BlockStmt{stmts}, nil
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

func (p *parser) ifStmt() (Stmt, error) {
	if _, err := p.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'if'."); err != nil {
		return nil, err
	}
	condition, err := p.expression()
	if err != nil {
		return nil, err
	}
	if _, err := p.consume(TOKEN_RIGHT_PAREN, "Expect ')' after if condition."); err != nil {
		return nil, err
	}
	thenBr, err := p.statement()
	if err != nil {
		return nil, err
	}

	var elseBr Stmt
	if p.match(TOKEN_ELSE) {
		elseBr, err = p.statement()
		if err != nil {
			return nil, err
		}
	}
	return IfStmt{condition: condition, thenBr: thenBr, elseBr: elseBr}, nil
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

func (p *parser) returnStmt() (Stmt, error) {
	keyword := p.previous()
	var value Expr
	if !p.currentIs(TOKEN_SEMICOLON) {
		var err error
		value, err = p.expression()
		if err != nil {
			return nil, err
		}
	}
	if _, err := p.consume(TOKEN_SEMICOLON, "Expect ';' after return value."); err != nil {
		return nil, err
	}
	return ReturnStmt{keyword: keyword, value: value}, nil
}

func (p *parser) whileStmt() (Stmt, error) {
	if _, err := p.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'while'."); err != nil {
		return nil, err
	}
	cond, err := p.expression()
	if err != nil {
		return nil, err
	}
	if _, err := p.consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition."); err != nil {
		return nil, err
	}
	body, err := p.statement()
	if err != nil {
		return nil, err
	}
	return WhileStmt{condition: cond, body: body}, nil
}

func (p *parser) block() ([]Stmt, error) {
	var statements []Stmt
	for !p.currentIs(TOKEN_RIGHT_BRACE) && !p.isAtEnd() {
		decl, err := p.declaration()
		if err != nil {
			return nil, err
		}
		statements = append(statements, decl)
	}
	if _, err := p.consume(TOKEN_RIGHT_BRACE, "Expect '}' after block."); err != nil {
		return nil, err
	}
	return statements, nil
}

// expressions

func (p *parser) expression() (Expr, error) {
	return p.assignment()
}

func (p *parser) assignment() (Expr, error) {
	expr, err := p.or()
	if err != nil {
		return nil, err
	}

	if p.match(TOKEN_EQUAL) {
		equals := p.previous()
		newValue, err := p.expression()
		if err != nil {
			return nil, err
		}

		switch tkn := expr.(type) {
		case VarExpr:
			return AssignExpr{name: tkn.name, expr: newValue}, nil
		default:
			// Don't return an error here, for reasons I don't actually know
			reportRuntimeError(RuntimeError{token: equals, message: "Invalid assignment target."})
		}
	}

	return expr, nil
}

func (p *parser) or() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.and, func(left, right Expr, op Token) Expr {
		return LogicalExpr{left: left, operator: op, right: right}
	}, TOKEN_OR)
}

func (p *parser) and() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.equality, func(left, right Expr, op Token) Expr {
		return LogicalExpr{left: left, operator: op, right: right}
	}, TOKEN_AND)
}

func (p *parser) equality() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.comparison, func(left, right Expr, op Token) Expr {
		return BinaryExpr{left: left, operator: op, right: right}
	}, TOKEN_EQUAL_EQUAL, TOKEN_BANG_EQUAL)
}

func (p *parser) comparison() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.termExpression, func(left, right Expr, op Token) Expr {
		return BinaryExpr{left: left, operator: op, right: right}
	}, TOKEN_GREATER, TOKEN_GREATER_EQUAL, TOKEN_LESS, TOKEN_LESS_EQUAL)
}

func (p *parser) termExpression() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.factorExpression, func(left, right Expr, op Token) Expr {
		return BinaryExpr{left: left, operator: op, right: right}
	}, TOKEN_PLUS, TOKEN_MINUS)
}

func (p *parser) factorExpression() (Expr, error) {
	return p.parseLeftAssociativeBinaryExprs(p.unaryExpression, func(left, right Expr, op Token) Expr {
		return BinaryExpr{left: left, operator: op, right: right}
	}, TOKEN_STAR, TOKEN_SLASH)
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
	return p.callExpression()
}

func (p *parser) callExpression() (Expr, error) {
	expr, err := p.primaryExpression()
	if err != nil {
		return nil, err
	}

	// chain of calls
	for {
		if p.match(TOKEN_LEFT_PAREN) {
			expr, err = p.finishCall(expr)
			if err != nil {
				return nil, err
			}
		} else {
			break
		}
	}

	return expr, nil
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

func (p *parser) finishCall(callee Expr) (Expr, error) {
	var args []Expr
	if !p.currentIs(TOKEN_RIGHT_PAREN) {
		for do := true; do; do = p.match(TOKEN_COMMA) {
			arg, err := p.expression()
			if err != nil {
				return nil, err
			}
			args = append(args, arg)
		}
	}

	paren, err := p.consume(TOKEN_RIGHT_PAREN, "Expect ')' after arguments")
	if err != nil {
		return nil, err
	}
	return CallExpr{callee: callee, paren: paren, arguments: args}, nil
}

func (p *parser) parseLeftAssociativeBinaryExprs(
	higherPrecedence func() (Expr, error),
	newExpr func(left, right Expr, operator Token) Expr,
	typs ...TokenType,
) (Expr, error) {
	expr, err := higherPrecedence()
	if err != nil {
		return nil, err
	}
	for p.match(typs...) {
		op := p.previous()
		right, err := higherPrecedence()
		if err != nil {
			return nil, err
		}
		expr = newExpr(expr, right, op)
	}
	return expr, nil
}

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
