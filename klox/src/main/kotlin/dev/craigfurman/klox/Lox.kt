package dev.craigfurman.klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
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

    private fun runSource(src: String) {
        val scanner = Scanner(src, ::error)
        val tokens = scanner.scanTokens()
        for (token in tokens) {
            println(token)
        }
    }

    private fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        this.hadError = true
    }
}

typealias ReportError = (Int, String) -> Unit
