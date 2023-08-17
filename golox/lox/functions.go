package lox

import "fmt"

type LoxCallable interface {
	Arity() int
	Call(interpreter *Interpreter, arguments []any) (any, error)
}

type LoxFunction struct {
	declaration   FuncStmt
	closure       *Environment
	isInitializer bool
}

func (f LoxFunction) Arity() int {
	return len(f.declaration.params)
}

func (f *LoxFunction) Call(interpreter *Interpreter, arguments []any) (any, error) {
	env := newEnvironment(f.closure)
	for i, arg := range arguments {
		env.define(f.declaration.params[i].lexeme, arg)
	}
	err := interpreter.executeBlock(f.declaration.body, env)
	if err != nil {
		if returnVal, ok := err.(Return); ok {
			// Initializers are special in that they always return their "this"
			// instance. The resolver has already ensured that no explicit return
			// values are allowed in initializers.
			if f.isInitializer {
				return f.closure.getAt(0, "this"), nil
			}

			return returnVal.value, nil
		}
		return nil, err
	}
	return nil, nil
}

func (f *LoxFunction) bind(instance *LoxInstance) *LoxFunction {
	env := newEnvironment(f.closure)
	env.define("this", instance)
	return &LoxFunction{declaration: f.declaration, closure: env}
}

func (f LoxFunction) String() string {
	return fmt.Sprintf("<fn %s>", f.declaration.name.lexeme)
}
