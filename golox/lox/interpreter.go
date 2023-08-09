package lox

import "fmt"

type Interpreter struct {
	environment *Environment
}

func newInterpreter() *Interpreter {
	return &Interpreter{
		environment: newEnvironment(nil),
	}
}

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

func (i *Interpreter) VisitBlockStmt(stmt BlockStmt) error {
	return i.executeBlock(stmt.statements, newEnvironment(i.environment))
}

// Expressions might have side effects. We evaluate it and discard the value in
// these statements.
func (i *Interpreter) VisitExprStmt(stmt ExprStmt) error {
	_, err := i.evaluate(stmt.expr)
	return err
}

func (i *Interpreter) VisitIfStmt(stmt IfStmt) error {
	cond, err := i.evaluate(stmt.condition)
	if err != nil {
		return err
	}
	if isTruthy(cond) {
		return i.execute(stmt.thenBr)
	} else if stmt.elseBr != nil {
		return i.execute(stmt.elseBr)
	}
	return nil
}

func (i *Interpreter) VisitPrintStmt(stmt PrintStmt) error {
	value, err := i.evaluate(stmt.expr)
	if err != nil {
		return err
	}
	fmt.Println(value)
	return nil
}

func (i *Interpreter) VisitVarStmt(stmt VarStmt) error {
	var value any
	if stmt.initializer != nil {
		var err error
		value, err = i.evaluate(stmt.initializer)
		if err != nil {
			return err
		}
	}
	i.environment.define(stmt.name.lexeme, value)
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

	case TOKEN_GREATER:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum > rightNum, nil
	case TOKEN_GREATER_EQUAL:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum >= rightNum, nil
	case TOKEN_LESS:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum < rightNum, nil
	case TOKEN_LESS_EQUAL:
		leftNum, rightNum, err := checkOperands[float64](expr.operator, left, right, "Operands must be numbers.")
		if err != nil {
			return nil, err
		}
		return leftNum <= rightNum, nil

	case TOKEN_EQUAL_EQUAL:
		return left == right, nil
	case TOKEN_BANG_EQUAL:
		return left != right, nil

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

func (i *Interpreter) VisitLogicalExpr(expr LogicalExpr) (any, error) {
	left, err := i.evaluate(expr.left)
	if err != nil {
		return nil, err
	}
	if expr.operator.typ == TOKEN_OR {
		if isTruthy(left) {
			return left, nil
		}
	} else {
		if !isTruthy(left) {
			return left, nil
		}
	}
	return i.evaluate(expr.right)
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

func (i *Interpreter) VisitVarExpr(expr VarExpr) (any, error) {
	return i.environment.get(expr.name)
}

func (i *Interpreter) executeBlock(stmts []Stmt, env *Environment) error {
	previous := i.environment
	i.environment = env
	defer func() { i.environment = previous }()

	for _, stmt := range stmts {
		if err := i.execute(stmt); err != nil {
			return err
		}
	}
	return nil
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

func isTruthy(val any) bool {
	if val == nil {
		return false
	}
	if boolVal, ok := val.(bool); ok {
		return boolVal
	}
	return true
}
