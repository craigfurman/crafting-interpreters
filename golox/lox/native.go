package lox

import "time"

type NativeFnClock struct{}

func (NativeFnClock) Arity() int { return 0 }

func (NativeFnClock) Call(interpreter *Interpreter, arguments []any) (any, error) {
	return float64(time.Now().UnixMilli()) / 1000, nil
}
