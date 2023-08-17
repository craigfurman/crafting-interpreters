package lox_test

import (
	"testing"

	"github.com/craigfurman/crafting-interpreters/golox/lox"
	"github.com/stretchr/testify/assert"
)

type someObj struct {
	val string
}

func TestIdentityMap_ReturnsValueForIdenticalObject(t *testing.T) {
	o1 := &someObj{"a value"}
	mapp := lox.NewIdentityMap[int]()
	mapp.Put(o1, 4)
	val, ok := mapp.Get(o1)
	assert.True(t, ok)
	assert.Equal(t, 4, val)
}

func TestIdentityMap_TreatsEqualButNonIdenticalObjectsAsDistinctKeys(t *testing.T) {
	o1 := &someObj{"a value"}
	o2 := &someObj{"a value"}
	mapp := lox.NewIdentityMap[int]()
	mapp.Put(o1, 4)
	mapp.Put(o2, 5)

	v1, _ := mapp.Get(o1)
	assert.Equal(t, 4, v1)
	v2, _ := mapp.Get(o2)
	assert.Equal(t, 5, v2)
}
