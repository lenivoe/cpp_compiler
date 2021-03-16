package com.scanner

class ScannerPosition(position: Int = 0, line: Int = 1, column: Int = 1) {
    var position: Int = position
        private set

    var line: Int = line
        private set

    var column: Int = column
        private set

    fun clone() = ScannerPosition(position, line, column)

    fun increaseLine(additive: Int = 1) {
        column = 1
        line += additive
        position += additive
    }

    fun increaseColumn(additive: Int = 1) {
        column += additive
        position += additive
    }
}
