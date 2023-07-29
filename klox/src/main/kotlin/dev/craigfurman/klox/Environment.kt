package dev.craigfurman.klox

class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Any?>()

    // TODO does this handle explicit/implicit nil binding?
    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) return values[name.lexeme]
        if (enclosing != null) return enclosing.get(name)
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun getAt(distance: Int, name: String) = ancestor(distance).values[name]

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    private fun ancestor(distance: Int): Environment {
        var env = this
        repeat(distance) { env = env.enclosing!! }
        return env
    }
}
