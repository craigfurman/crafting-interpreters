package dev.craigfurman.klox

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    return when (args.size) {
        0 -> {
            val lox = Lox(true)
            lox.runPrompt()
        }

        1 -> {
            val lox = Lox()
            lox.runFile(args[0])
        }

        else -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
    }
}

