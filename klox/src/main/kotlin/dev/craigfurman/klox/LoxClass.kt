package dev.craigfurman.klox

open class LoxClass(val name: String, private val methods: Map<String, LoxFunction>) : LoxCallable {
    override fun arity() = when (val init = findMethod("init")) {
        null -> 0
        else -> init.arity()
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        findMethod("init")?.let { it.bind(instance).call(interpreter, arguments) }
        return instance
    }

    fun findMethod(name: String) = methods[name]

    override fun toString() = name
}

open class LoxInstance(private val klass: LoxClass) {
    private val fields = HashMap<String, Any?>()

    open fun get(name: Token): Any? {
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
