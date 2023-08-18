package main

import (
	"fmt"
	"log"
	"net/http"
	_ "net/http/pprof"
	"os"

	"github.com/craigfurman/crafting-interpreters/golox/lox"
)

func main() {
	// Optionally enable pprof handlers
	if os.Getenv("GOLOX_PROFILE") != "" {
		go func() {
			log.Println(http.ListenAndServe("localhost:6060", nil))
		}()
	}

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
