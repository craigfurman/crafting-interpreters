package dev.craigfurman.klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox : ErrorReporter {
    private var hadError = false

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
        if (this.hadError) {
            exitProcess(65)
        }
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

    private fun runSource(src: String) {
        val scanner = Scanner(src, this)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens, this)
        val expr = parser.parse()
        if (hadError) return

        // Code paths that have not set hadError _should_ not have thrown an
        // exception, therefore the only case in which expr is null _should_
        // not have occurred.
        println(AstPrinter().print(expr!!))
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
