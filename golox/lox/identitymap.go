package lox

import "reflect"

type IdentityMap[T any] struct {
	mapp map[uintptr]T
}

func NewIdentityMap[T any]() IdentityMap[T] {
	return IdentityMap[T]{mapp: map[uintptr]T{}}
}

func (m *IdentityMap[T]) Put(key any, val T) {
	m.mapp[reflect.ValueOf(key).Pointer()] = val
}

func (m *IdentityMap[T]) Get(key any) (T, bool) {
	val, ok := m.mapp[reflect.ValueOf(key).Pointer()]
	return val, ok
}
