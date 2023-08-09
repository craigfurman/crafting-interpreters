package lox

import "fmt"

type Interpreter struct{}

func (i *Interpreter) interpret(statements []Stmt) {
	for _, stmt := range statements {
		if err := i.execute(stmt); err != nil {
			if runtimeErr, ok := err.(RuntimeError); ok {
				reportRuntimeError(runtimeErr)
				return
			}

			// panic all other errors
			must(err)
		}
	}
}

func (i *Interpreter) execute(stmt Stmt) error {
	return stmt.Accept(i)
}

func (i *Interpreter) evaluate(expr Expr) (any, error) {
	return expr.Accept(i)
}

// Expressions might have side effects. We evaluate it and discard the value in
// these statements.
func (i *Interpreter) VisitExprStmt(stmt ExprStmt) error {
	_, err := i.evaluate(stmt.expr)
	return err
}

func (i *Interpreter) VisitPrintStmt(stmt PrintStmt) error {
	value, err := i.evaluate(stmt.expr)
	if err != nil {
		return err
	}
	fmt.Println(value)
	return nil
}

func (i *Interpreter) VisitBinaryExpr(expr BinaryExpr) (any, error) {
	left, err := i.evaluate(expr.left)
	if err != nil {
		return nil, err
	}
	right, err := i.evaluate(expr.right)
	if err != nil {
		return nil, err
	}

	switch expr.operator.typ {
	case TOKEN_PLUS:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "")
		if err == nil {
			return leftNum + rightNum, nil
		}
		leftStr, rightStr, err := checkOperands[string](expr.operator, left, right, "")
		if err == nil {
			return leftStr + rightStr, nil
		}
		return nil, RuntimeError{token: expr.operator, message: "Operands must be two numbers or two strings."}
	case TOKEN_MINUS:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum - rightNum, nil
	case TOKEN_STAR:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum * rightNum, nil
	case TOKEN_SLASH:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum / rightNum, nil

	// If we end up here, we have a parser error
	default:
		return nil, fmt.Errorf("unexpected binary operator: %s", expr.operator.lexeme)
	}
}

func (i *Interpreter) VisitGroupingExpr(expr GroupingExpr) (any, error) {
	return i.evaluate(expr.expr)
}

func (i *Interpreter) VisitLiteralExpr(expr LiteralExpr) (any, error) {
	return expr.value, nil
}

func (i *Interpreter) VisitUnaryExpr(expr UnaryExpr) (any, error) {
	right, err := i.evaluate(expr.right)
	if err != nil {
		return nil, err
	}
	switch expr.operator.typ {
	case TOKEN_MINUS:
		num, err := checkNumberOperand(expr.operator, right)
		if err != nil {
			return nil, err
		}
		return -num, nil

	// If we end up here, we have a parser error
	default:
		return nil, fmt.Errorf("unexpected unary operator: %s", expr.operator.lexeme)
	}
}

func checkNumberOperand(operator Token, value any) (float64, error) {
	switch v := value.(type) {
	case float64:
		return v, nil
	default:
		return 0, RuntimeError{token: operator, message: "Operand must be a number."}
	}
}

func checkOperands[T any](operator Token, value1, value2 any, message string) (T, T, error) {
	cast1, ok1 := value1.(T)
	cast2, ok2 := value2.(T)
	if !(ok1 && ok2) {
		return cast1, cast2, RuntimeError{token: operator, message: message}
	}
	return cast1, cast2, nil
}
