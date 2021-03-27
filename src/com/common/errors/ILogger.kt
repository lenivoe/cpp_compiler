package com.common.errors

import java.io.Closeable

interface ILogger: Closeable {
    fun log(msg: String)
    fun log(msg: String, line: Int, column: Int)
    fun error(msg: String)
    fun error(msg: String, line: Int, column: Int)
}
