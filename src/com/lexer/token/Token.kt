package com.lexer.token

data class Token(val type: TokenType, val image: String, val line: Int, val column: Int) {
    override fun toString(): String {
        return "'$image' (type: $type) (line: $line, column: $column)"
    }
}
