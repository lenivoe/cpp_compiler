package com.common.errors

import java.io.File
import java.io.FileWriter

class FileErrorWriter(path: String) : IErrorWriter {
    init {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    private var fileWriter = FileWriter(path, true)

    override fun write(msg: String) {
        val errorLine = "error: $msg"
        fileWriter.appendLine(errorLine)
    }

    override fun write(msg: String, line: Int, column: Int) {
        val errorLine = "error: $msg (line: $line) (column: $column)"
        fileWriter.appendLine(errorLine)
    }

    override fun close() {
        fileWriter.close()
    }
}
