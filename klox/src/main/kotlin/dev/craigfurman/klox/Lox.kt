package dev.craigfurman.klox

import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox(replSession: Boolean = false) : ErrorReporter, RuntimeErrorReporter {
    private var hadError = false
    private var hadRuntimeError = false
    private val interpreter: Interpreter

    init {
        this.interpreter = Interpreter(replSession, ::runtimeError)
    }

    fun runPrompt() {
        val terminal = TerminalBuilder.builder().system(true).build()
        val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .history(DefaultHistory())
            .parser(DefaultParser())
            .build()

        do {
            val line = lineReader.readLine("> ")
            runSource(line)
            this.hadError = false
        } while (line != null)

        terminal.close()
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
        System.err.println(error.message + "\n[line ${error.token.line}]")
        this.hadRuntimeError = true
    }

    private fun runSource(src: String) {
        val scanner = Scanner(src, this)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens, this)
        val statements = parser.parse()
        if (hadError) return

        val resolver = Resolver(interpreter, this)
        resolver.resolve(statements)
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
