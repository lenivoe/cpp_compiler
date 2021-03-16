package com.common

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class SourceLoader {
    var source: String = ""
        private set

    fun loadFile(path: String) {
        try {
            val src = Files.readString(Paths.get(path))
            loadSource(src)
        } catch (ex: IOException) {
            throw CommonException("bad file exception: $ex")
        }
    }

    fun loadSource(source: String) {
        this.source = prepareSource(source)
    }
}

private fun prepareSource(source: String): String {
    var src = source.replace("\r", "")
    src += "${SpecChars.fileEnd}".repeat(2)
    return src
}
