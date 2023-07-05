package dev.craigfurman.klox

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val lox = Lox()
    return when (args.size) {
        0 ->
            lox.runPrompt()

        1 ->
            lox.runFile(args[0])

        else -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
    }
}

