@file:Suppress("NOTHING_TO_INLINE")

package com.common.errors

import java.io.File
import java.io.FileWriter

class FileLogger(path: String) : ILogger {
    init {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun log(msg: String) = write("[log] $msg")
    override fun log(msg: String, line: Int, column: Int) {
        write("[log] $msg", line, column)
    }

    override fun error(msg: String) = write("[error] $msg")
    override fun error(msg: String, line: Int, column: Int) {
        write("[error] $msg", line, column)
    }

    override fun close() = fileWriter.close()


    private val fileWriter = FileWriter(path, true)

    private inline fun write(msg: String) {
        fileWriter.appendLine(msg)
    }
    private inline fun write(msg: String, line: Int, column: Int) {
        write("$msg (line: $line, column: $column)")
    }
}
