package lox

import "fmt"

type Environment struct {
	values    map[string]any
	enclosing *Environment
}

func (e *Environment) define(name string, value any) {
	e.values[name] = value
}

func (e *Environment) assign(name Token, value any) error {
	if _, ok := e.values[name.lexeme]; ok {
		e.values[name.lexeme] = value
		return nil
	}
	if e.enclosing != nil {
		return e.enclosing.assign(name, value)
	}
	return RuntimeError{token: name, message: fmt.Sprintf("Undefined variable: '%s'.", name.lexeme)}
}

func (e *Environment) get(name Token) (any, error) {
	if val, ok := e.values[name.lexeme]; ok {
		return val, nil
	}
	if e.enclosing != nil {
		return e.enclosing.get(name)
	}
	return nil, RuntimeError{token: name, message: fmt.Sprintf("Undefined variable: '%s'.", name.lexeme)}
}
