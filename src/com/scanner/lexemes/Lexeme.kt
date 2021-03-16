package com.scanner.lexemes

data class Lexeme(val type: LexemeType, val image: String, val line: Int, val column: Int) {
    override fun toString(): String {
        return "'$image' (type: $type) (line: $line, column: $column)"
    }
}
