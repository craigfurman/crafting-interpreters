package dev.craigfurman.klox

abstract class NativeFunction : LoxCallable {
    override fun toString() = "<native fn>"
}

object Clock : NativeFunction() {
    override fun arity() = 0
    override fun call(interpreter: Interpreter, arguments: List<Any?>) =
        System.currentTimeMillis() / 1000.0
}
