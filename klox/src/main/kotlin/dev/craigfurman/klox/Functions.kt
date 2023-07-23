package dev.craigfurman.klox

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class LoxFunction(private val declaration: Stmt.FunctionStmt) : LoxCallable {
    override fun arity() = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(interpreter.globals)
        for ((i, param) in declaration.params.withIndex()) {
            environment.define(param.lexeme, arguments[i])
        }
        interpreter.executeBlock(declaration.body, environment)
        return null
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}
