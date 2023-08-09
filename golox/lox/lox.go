package lox

import (
	"bufio"
	"fmt"
	"os"

	"github.com/davecgh/go-spew/spew"
)

var (
	hadError = false
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
	return 0
}

func runSource(source string) {
	tokens := scan(source)
	expr := parse(tokens)

	// TODO remove when we have an interpreter
	spew.Dump(expr)
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

func must(err error) {
	if err != nil {
		panic(err)
	}
}
