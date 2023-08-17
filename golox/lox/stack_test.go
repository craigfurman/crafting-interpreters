package lox_test

import (
	"testing"

	"github.com/craigfurman/crafting-interpreters/golox/lox"
	"github.com/stretchr/testify/assert"
)

func TestStack(t *testing.T) {
	stack := &lox.Stack[int]{}
	stack.Push(1)
	stack.Push(2)
	stack.Push(3)

	assert.Equal(t, 3, stack.Pop())
	assert.Equal(t, 2, stack.Peek())
	assert.Equal(t, 2, stack.Pop())
	assert.Equal(t, 1, stack.Pop())
}
