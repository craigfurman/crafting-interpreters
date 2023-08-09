package lox

func contains[T comparable](arr []T, v T) bool {
	for _, e := range arr {
		if e == v {
			return true
		}
	}
	return false
}
