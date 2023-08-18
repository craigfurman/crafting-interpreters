package lox

import "fmt"

type LoxClass struct {
	name       string
	superclass *LoxClass
	methods    map[string]*LoxFunction
}

func (c *LoxClass) Arity() int {
	init := c.findMethod("init")
	if init != nil {
		return init.Arity()
	}
	return 0
}

func (c *LoxClass) Call(interpreter *Interpreter, arguments []any) (any, error) {
	instance := &LoxInstance{class: c, fields: map[string]any{}}

	// run initializer
	if init := c.findMethod("init"); init != nil {
		// initializers can't return values
		_, err := init.bind(instance).Call(interpreter, arguments)
		if err != nil {
			return nil, err
		}
	}

	return instance, nil
}

func (c *LoxClass) findMethod(name string) *LoxFunction {
	return c.methods[name]
}

func (c LoxClass) String() string {
	return c.name
}

type LoxInstance struct {
	class  *LoxClass
	fields map[string]any
}

func (i *LoxInstance) get(name Token) (any, error) {
	if prop, ok := i.fields[name.lexeme]; ok {
		return prop, nil
	}

	method := i.class.findMethod(name.lexeme)
	if method != nil {
		return method.bind(i), nil
	}

	return nil, RuntimeError{name, fmt.Sprintf("Undefined property '%s'.", name.lexeme)}
}

func (i *LoxInstance) set(name Token, value any) {
	i.fields[name.lexeme] = value
}

func (i LoxInstance) String() string {
	return i.class.name + " instance"
}