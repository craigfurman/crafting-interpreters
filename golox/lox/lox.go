package lox

import (
	"bufio"
	"fmt"
	"os"
)

var (
	hadError        = false
	hadRuntimeError = false
	interpreter     = &Interpreter{}
)

func REPL() {
	scanner := bufio.NewScanner(os.Stdin)

	fmt.Print("> ")
	for scanner.Scan() {
		runSource(scanner.Text())
		hadError = false
		fmt.Print("> ")
	}
	must(scanner.Err())
}

func RunFile(path string) int {
	source, err := os.ReadFile(path)
	must(err)
	runSource(string(source))
	if hadError {
		return 65
	}
	if hadRuntimeError {
		return 70
	}
	return 0
}

func runSource(source string) {
	tokens := scan(source)
	expr := parse(tokens)
	if hadError {
		return
	}

	// TODO the interpreter is just an arithmetic expression evaluator for now
	val, err := interpreter.evaluate(expr)
	must(err) // TODO no
	fmt.Println(val)
}

func tokenError(token Token, message string) {
	if token.typ == TOKEN_EOF {
		reportError(token.line, " at end", message)
	} else {
		reportError(token.line, fmt.Sprintf(" at '%s'", token.lexeme), message)
	}
}

func reportError(line int, where string, message string) {
	fmt.Fprintf(os.Stderr, "[line %d] Error%s: %s\n", line, where, message)
	hadError = true
}

type RuntimeError struct {
	token   Token
	message string
}

func (e RuntimeError) Error() string {
	return e.message
}

func reportRuntimeError(err RuntimeError) {
	fmt.Fprintf(os.Stderr, "%s \n[line %d]", err.message, err.token.line)
	hadRuntimeError = true
}

func must(err error) {
	if err != nil {
		panic(err)
	}
}
