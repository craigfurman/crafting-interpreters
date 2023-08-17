package lox

type Stack[T any] struct {
	stack []T
}

func (s *Stack[T]) Push(t T) {
	s.stack = append(s.stack, t)
}

func (s *Stack[T]) Pop() T {
	lastIdx := len(s.stack) - 1
	t := s.stack[lastIdx]
	s.stack = s.stack[:lastIdx]
	return t
}

func (s *Stack[T]) Peek() T {
	lastIdx := len(s.stack) - 1
	t := s.stack[lastIdx]
	return t
}

func (s *Stack[T]) PeekI(i int) T {
	return s.stack[i]
}

func (s *Stack[T]) IsEmpty() bool {
	return len(s.stack) == 0
}

func (s *Stack[T]) Size() int {
	return len(s.stack)
}
