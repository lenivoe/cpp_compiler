package com.common.errors

import java.io.Closeable

interface IErrorWriter: Closeable {
    fun write(msg: String)
    fun write(msg: String, line: Int, column: Int)
}
