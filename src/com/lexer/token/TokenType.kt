package com.lexer.token

enum class TokenType {
    // идентификаторы
    IDENTIFIER /** например ab, _1t ... */,
    CONST8 /** например 01, 000471 ... */,
    CONST10 /** например 1, 123, 9999 ... */,
    CONST16 /** например 0x1, 0XaBcDeF ... */,

    // типы
    CHAR /** char*/,
    SHORT_INT /** short или short int*/,
    LONG_INT /** long или long int */,
    INT /** int */,

    // ключевые слова
    CLASS /** class */,
    MAIN_FUNC /** main */,
    WHILE /** while */,
    RETURN /** return */,

    // операции
    ASSIGNMENT /** = */,
    EQUAL /** == */,
    NOT_EQUAL /** != */,
    LESS_OR_EQUAL /** <= */,
    GREATER_OR_EQUAL /** >= */,
    LESS /** < */,
    GREATER /** > */,
    SHIFT_LEFT /** << */,
    SHIFT_RIGHT /** >> */,
    PLUS /** + */,
    MINUS /** - */,
    MULTIPLICATION /** * */,
    DIVISION /** / */,
    REMAINDER /** % */,
    INCREMENT /** ++ */,
    DECREMENT /** -- */,

    // спецсимволы
    SEMICOLON /** ; */,
    COMMA /** , */,
    DOT /** . */,
    BRACE_LEFT /** { */,
    BRACE_RIGHT /** } */,
    BRACKET_LEFT /** [ */,
    BRACKET_RIGHT /** ] */,
    PARENTHESIS_LEFT /** ( */,
    PARENTHESIS_RIGHT /** ) */,

    // дополнительные символы
    END /** \0 */,
    ERROR /** ошибка */
}

val TokenType.isBasicDataType: Boolean
    inline get() = this in TokenType.CHAR..TokenType.INT
