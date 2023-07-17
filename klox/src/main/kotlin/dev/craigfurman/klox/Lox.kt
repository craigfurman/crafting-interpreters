package dev.craigfurman.klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox : ErrorReporter, RuntimeErrorReporter {
    private var hadError = false
    private var hadRuntimeError = false
    private val interpreter = Interpreter(::runtimeError)

    fun runPrompt() {
        val stdinReader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            print("> ")
            val line = stdinReader.readLine() ?: break
            runSource(line)
            this.hadError = false
        }
    }

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        runSource(bytes.toString(Charset.defaultCharset()))
        if (this.hadError) exitProcess(65)
        if (this.hadRuntimeError) exitProcess(70)

    }

    override fun error(line: Int, message: String) {
        report(line, "", message)
    }

    override fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '${token.lexeme}'", message)
        }
    }

    override fun runtimeError(error: RuntimeError) {
        println(error.message + "\n[line ${error.token.line}]")
        this.hadRuntimeError = true
    }

    private fun runSource(src: String) {
        val scanner = Scanner(src, this)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens, this)
        val statements = parser.parse()
        if (hadError) return
        
        interpreter.interpret(statements)
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        this.hadError = true
    }
}

interface ErrorReporter {
    fun error(line: Int, message: String)
    fun error(token: Token, message: String)
}

fun interface RuntimeErrorReporter {
    fun runtimeError(error: RuntimeError)
}
