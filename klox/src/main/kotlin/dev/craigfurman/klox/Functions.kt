package dev.craigfurman.klox

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class LoxFunction(
    private val declaration: Stmt.FunctionStmt,
    private val closure: Environment,
    private val isInitializer: Boolean = false,
) : LoxCallable {
    override fun arity() = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for ((i, param) in declaration.params.withIndex()) {
            environment.define(param.lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnVal: Interpreter.Return) {
            // Initializers are allowed to return, just not return values. The resolver has ensured
            // that an early return from an initializer will always return nil. But, we special-case
            // initializers to ensure that they always return this.
            return if (isInitializer) closure.getAt(0, "this") else returnVal.value
        }

        return if (isInitializer) closure.getAt(0, "this") else null
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val env = Environment(closure)
        env.define("this", instance)
        return LoxFunction(declaration, env, isInitializer)
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}
