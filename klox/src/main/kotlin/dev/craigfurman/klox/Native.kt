package dev.craigfurman.klox

abstract class NativeFunction : LoxCallable {
    override fun toString() = "<native fn>"
}

object Clock : NativeFunction() {
    override fun arity() = 0
    override fun call(interpreter: Interpreter, arguments: List<Any?>) =
        System.currentTimeMillis() / 1000.0
}

object LoxList : LoxClass("List", mapOf()) {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        return LoxListInstance(this)
    }

    private class LoxListInstance(klass: LoxClass) : LoxInstance(klass) {
        private val storage = ArrayList<Any?>()
        override fun get(name: Token): Any? {
            return when (name.lexeme) {
                "length" -> object : NativeFunction() {
                    override fun arity() = 0
                    override fun call(interpreter: Interpreter, arguments: List<Any?>) =
                        storage.size.toDouble()
                }

                "get" -> object : NativeFunction() {
                    override fun arity() = 1
                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                        val idx = arguments.first()
                        if (idx !is Double) throw Error("Unexpected type of argument to List.get: $idx")
                        return storage[idx.toInt()]
                    }
                }

                "append" -> object : NativeFunction() {
                    override fun arity() = 1
                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                        val element = arguments.first()
                        storage.add(element)
                        return this
                    }
                }

                else -> null
            }
        }
    }
}
