@file:Suppress("NOTHING_TO_INLINE")

package com.lexer

import com.common.CommonException
import com.common.SpecChars
import com.common.errors.ILogger
import com.lexer.token.Token
import com.lexer.token.TokenType

class Lexer(private val src: String, private val logger: ILogger) {
    private val maxSourceLength = 10000
    private val maxLexemeLength = 20

    private var positionState = LexerPosition()

    private val char: Char
        inline get() = src[positionState.position]
    private val nextChar: Char
        inline get() = src[positionState.position + 1]

    init {
        if (src.length > maxSourceLength) {
            throw CommonException("source text is too long")
        }

        assert(src.substring(src.length - 2).all { it == SpecChars.fileEnd }) {
            "source must have '\\0\\0' at the end"
        }
    }

    fun savePosition(): LexerPosition {
        return positionState.clone()
    }

    fun restorePosition(lexerPosition: LexerPosition) {
        positionState = lexerPosition
    }

    inline fun skip(amount: Int): Lexer {
        repeat(amount) { findNextToken() }
        return this
    }

    fun findNextToken(): Token {
        var token = skipIgnoredChars()
            ?: getAlphabetLexeme()
            ?: getNumericLexeme()
            ?: getSymbolLexeme()

        if (token === null) {
            logger.error("token not found for char '$char'", positionState.line, positionState.column)
            token = Token(TokenType.ERROR, "$char", positionState.line, positionState.column)
            positionState.increaseColumn()
        }

        return token
    }

    private inline fun skipIgnoredChars(): Token? {
        while (true) {
            when (char) { // аналог switch
                ' ', '\t' -> positionState.increaseColumn()
                '\n' -> positionState.increaseLine()
                '/' -> {
                    if (nextChar == '/') { // обработка однострочного комментария
                        positionState.increaseColumn(2)
                        while (char != SpecChars.fileEnd && char != '\n') {
                            positionState.increaseColumn()
                        }
                    } else if (nextChar == '*') { // обработка многострочного комментария
                        // на случай незакрытого комментария в конце файла
                        val commentBegin = savePosition()

                        positionState.increaseColumn(2)
                        while (char != SpecChars.fileEnd && !(char == '*' && nextChar == '/')) {
                            if (char == '\n') {
                                positionState.increaseLine()
                            } else {
                                positionState.increaseColumn()
                            }
                        }

                        if (nextChar == SpecChars.fileEnd) {
                            logger.error("unclosed multiline comment", commentBegin.line, commentBegin.column)
                            return Token(TokenType.ERROR, "/*", commentBegin.line, commentBegin.column)
                        } else {
                            positionState.increaseColumn(2)
                        }
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
        return null
    }

    private inline fun getAlphabetLexeme(): Token? {
        if (!char.isLetter() && char != '_') {
            return null
        }

        val lexBegin = positionState.position
        while ((char.isLetterOrDigit() || char == '_') && (positionState.position - lexBegin <= maxLexemeLength)) {
            positionState.increaseColumn()
        }

        val image = src.substring(lexBegin, positionState.position)

        val type = if (image.length > maxLexemeLength) {
            logger.error("too long token", positionState.line, positionState.column - image.length)
            TokenType.ERROR
        } else {
            when (image) {
                "char" -> TokenType.CHAR
                "short" -> TokenType.SHORT_INT
                "long" -> TokenType.LONG_INT
                "int" -> TokenType.INT
                "class" -> TokenType.CLASS
                "main" -> TokenType.MAIN_FUNC
                "while" -> TokenType.WHILE
                "return" -> TokenType.RETURN
                else -> TokenType.IDENTIFIER
            }
        }

        return Token(type, image, positionState.line, positionState.column - image.length)
    }

    private inline fun getNumericLexeme(): Token? {
        if (!isDecimal(char)) {
            return null
        }

        val numberBegin = savePosition()

        var type = if (char == '0') {
            if(nextChar == 'x' || nextChar == 'X') {
                positionState.increaseColumn(2)
                if (isHex(char)) {
                    TokenType.CONST16
                } else {
                    logger.error("invalid digit '$char' in hex constant",
                        numberBegin.line, numberBegin.column)
                    TokenType.ERROR
                }
            } else {
                positionState.increaseColumn()
                TokenType.CONST8
            }
        } else {
            TokenType.CONST10
        }

        val scanNumber = { isRightDigit: (Char) -> Boolean ->
            while (isRightDigit(char) && (positionState.position - numberBegin.position <= maxLexemeLength)) {
                positionState.increaseColumn()
            }
        }

        when (type) {
            TokenType.CONST8 -> {
                scanNumber(::isOctal)
                if (char in '8'..'9') {
                    logger.error("invalid digit '$char' in octal constant",
                        numberBegin.line, numberBegin.column)
                    type = TokenType.ERROR
                }
            }
            TokenType.CONST10 -> scanNumber(::isDecimal)
            TokenType.CONST16 -> scanNumber(::isHex)
            else -> Unit // иначе ничего не происходит
        }

        val image = src.substring(numberBegin.position, positionState.position)
        if (image.length > maxLexemeLength) {
            logger.error("too long token", numberBegin.line, numberBegin.column)
            type = TokenType.ERROR
        }

        return Token(type, image, numberBegin.line, numberBegin.column)
    }

    private inline fun getSymbolLexeme(): Token? {
        var image = "$char"

        val type = when (char) {
            '=' -> {
                if (nextChar == '=') {
                    image += "$nextChar"
                    TokenType.EQUAL
                }
                else TokenType.ASSIGNMENT
            }
            '!' -> {
                if (nextChar == '=') {
                    image += "$nextChar"
                    TokenType.NOT_EQUAL
                }
                else {
                    logger.error("invalid token '$image'", positionState.line, positionState.column)
                    TokenType.ERROR
                }
            }
            '<' -> {
                when (nextChar) {
                    '=' -> {
                        image += "$nextChar"
                        TokenType.LESS_OR_EQUAL
                    }
                    '<' -> {
                        image += "$nextChar"
                        TokenType.SHIFT_LEFT
                    }
                    else -> TokenType.LESS
                }
            }
            '>' -> {
                when (nextChar) {
                    '=' -> {
                        image += "$nextChar"
                        TokenType.GREATER_OR_EQUAL
                    }
                    '>' -> {
                        image += "$nextChar"
                        TokenType.SHIFT_RIGHT
                    }
                    else -> TokenType.GREATER
                }
            }
            '+' -> {
                if (nextChar == '+') {
                    image += "$nextChar"
                    TokenType.INCREMENT
                }
                else TokenType.PLUS
            }
            '-' -> {
                if (nextChar == '-') {
                    image += "$nextChar"
                    TokenType.DECREMENT
                }
                else TokenType.MINUS
            }
            '*' -> TokenType.MULTIPLICATION
            '/' -> TokenType.DIVISION
            '%' -> TokenType.REMAINDER
            ';' -> TokenType.SEMICOLON
            ',' -> TokenType.COMMA
            '.' -> TokenType.DOT
            '{' -> TokenType.BRACE_LEFT
            '}' -> TokenType.BRACE_RIGHT
            '[' -> TokenType.BRACKET_LEFT
            ']' -> TokenType.BRACKET_RIGHT
            '(' -> TokenType.PARENTHESIS_LEFT
            ')' -> TokenType.PARENTHESIS_RIGHT
            SpecChars.fileEnd -> {
                image = ""
                TokenType.END
            }

            else -> return null
        }

        val token = Token(type, image, positionState.line, positionState.column)
        positionState.increaseColumn(image.length)
        return token
    }
}


private inline fun isOctal(ch: Char): Boolean
    = ch in '0'..'7'

private inline fun isDecimal(ch: Char): Boolean
    = ch.isDigit()

private inline fun isHex(ch: Char): Boolean
    = isDecimal(ch) || ch in 'a'..'f' || ch in 'A'..'F'
