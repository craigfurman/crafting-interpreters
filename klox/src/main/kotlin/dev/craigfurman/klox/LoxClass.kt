package dev.craigfurman.klox

class LoxClass(val name: String, private val methods: Map<String, LoxFunction>) : LoxCallable {
    override fun arity() = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        return LoxInstance(this)
    }

    fun findMethod(name: String) = methods[name]

    override fun toString() = name
}

class LoxInstance(private val klass: LoxClass) {
    private val fields = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) return fields[name.lexeme]

        val method = klass.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString() = klass.name + " instance"
}
