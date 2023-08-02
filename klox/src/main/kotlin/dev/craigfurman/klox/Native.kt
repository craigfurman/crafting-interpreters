package dev.craigfurman.klox

abstract class NativeFunction : LoxCallable {
    override fun toString() = "<native fn>"
}

object Clock : NativeFunction() {
    override fun arity() = 0
    override fun call(interpreter: Interpreter, arguments: List<Any?>) =
        System.currentTimeMillis() / 1000.0
}

object LoxList : LoxClass("List", null, mapOf()) {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        return Instance()
    }

    class Instance(private val storage: MutableList<Any?> = ArrayList()) : LoxInstance(LoxList) {
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

                "set" -> object : NativeFunction() {
                    override fun arity() = 2

                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                        val idx = arguments[0] as Double
                        val element = arguments[1]
                        storage[idx.toInt()] = element
                        return this
                    }
                }

                "remove" -> object : NativeFunction() {
                    override fun arity() = 1

                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                        storage.removeAt((arguments.first() as Double).toInt())
                        return this
                    }
                }

                else -> null
            }
        }
    }
}
