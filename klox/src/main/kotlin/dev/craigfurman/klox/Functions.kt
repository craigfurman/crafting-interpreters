package dev.craigfurman.klox

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class LoxFunction(private val declaration: Stmt.FunctionStmt, private val closure: Environment) :
    LoxCallable {
    override fun arity() = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for ((i, param) in declaration.params.withIndex()) {
            environment.define(param.lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnVal: Interpreter.Return) {
            return returnVal.value
        }
        return null
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}
