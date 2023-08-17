package lox

import "fmt"

type Environment struct {
	values    map[string]any
	enclosing *Environment
}

func newEnvironment(enclosing *Environment) *Environment {
	return &Environment{enclosing: enclosing, values: map[string]any{}}
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
	return RuntimeError{token: name, message: fmt.Sprintf("Undefined variable '%s'.", name.lexeme)}
}

func (e *Environment) assignAt(distance int, name Token, value any) {
	e.ancestor(distance).values[name.lexeme] = value
}

func (e *Environment) get(name Token) (any, error) {
	if val, ok := e.values[name.lexeme]; ok {
		return val, nil
	}
	if e.enclosing != nil {
		return e.enclosing.get(name)
	}
	return nil, RuntimeError{token: name, message: fmt.Sprintf("Undefined variable '%s'.", name.lexeme)}
}

func (e *Environment) getAt(distance int, name string) any {
	return e.ancestor(distance).values[name]
}

func (e *Environment) ancestor(distance int) *Environment {
	env := e
	for i := 0; i < distance; i++ {
		env = env.enclosing
	}
	return env
}
