package main

import (
	"fmt"
	"os"

	"github.com/craigfurman/crafting-interpreters/golox/lox"
)

func main() {
	switch len(os.Args) {
	case 1:
		lox.REPL()
	case 2:
		os.Exit(lox.RunFile(os.Args[1]))
	default:
		fmt.Fprintln(os.Stderr, "Usage: golox [script]")
		os.Exit(64)
	}
}
