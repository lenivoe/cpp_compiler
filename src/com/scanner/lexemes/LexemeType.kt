package com.scanner.lexemes

enum class LexemeType {
    // идентификаторы
    IDENTIFIER,
    CONST8,
    CONST10,
    CONST16,

    // типы
    CHAR,
    SHORT_INT,
    LONG_INT,
    INT,

    // ключевые слова
    CLASS,
    MAIN_FUNC,
    WHILE,

    // операции
    ASSIGNMENT,
    EQUAL,
    NOT_EQUAL,
    LESS_OR_EQUAL,
    GREATER_OR_EQUAL,
    LESS,
    GREATER,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    PLUS,
    MINUS,
    MULTIPLICATION,
    DIVISION,
    REMAINDER,
    INCREMENT,
    DECREMENT,

    // спецсимволы
    SEMICOLON,
    COMMA,
    BRACE_LEFT,
    BRACE_RIGHT,
    BRACKET_LEFT,
    BRACKET_RIGHT,
    PARENTHESIS_LEFT,
    PARENTHESIS_RIGHT,

    // дополнительные символы
    END,
    ERROR
}
