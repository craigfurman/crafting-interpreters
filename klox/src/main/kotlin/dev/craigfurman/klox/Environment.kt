package dev.craigfurman.klox

class Environment {
    private val values = HashMap<String, Any?>()

    // TODO does this handle explicit/implicit nil binding?
    fun get(name: Token): Any = when (val value = values[name.lexeme]) {
        null -> throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        else -> value
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
