package dev.craigfurman.klox

val panicOnError = object : ErrorReporter {
    override fun error(line: Int, message: String) {
        throw Exception(message)
    }

    override fun error(token: Token, message: String) {
        throw Exception(message)
    }
}
