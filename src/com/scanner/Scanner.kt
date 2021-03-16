@file:Suppress("NOTHING_TO_INLINE")

package com.scanner

import com.common.CommonException
import com.common.SpecChars
import com.common.errors.IErrorWriter
import com.scanner.lexemes.Lexeme
import com.scanner.lexemes.LexemeType

class Scanner(private val src: String, private val errorWriter: IErrorWriter) {
    private val maxSourceLength = 10000
    private val maxLexemeLength = 20

    private var scanPos = ScannerPosition()

    private val char: Char
        inline get() = src[scanPos.position]
    private val nextChar: Char
        inline get() = src[scanPos.position + 1]

    init {
        if (src.length > maxSourceLength) {
            throw CommonException("source text is too long")
        }

        assert(src.substring(src.length - 2).all { it == SpecChars.fileEnd }) {
            "source must have '\\0\\0' at the end"
        }
    }

    @Suppress("unused")
    fun savePosition(): ScannerPosition {
        return scanPos.clone()
    }

    @Suppress("unused")
    fun restorePosition(scannerPosition: ScannerPosition) {
        scanPos = scannerPosition
    }

    fun findNextLexeme(): Lexeme {
        var lexeme = this.skipIgnoredChars()

        if (lexeme == null) {
            lexeme = this.getAlphabetLexeme()
        }

        if (lexeme == null) {
            lexeme = this.getNumericLexeme()
        }

        if (lexeme == null) {
            lexeme = this.getSymbolLexeme()
        }

        if (lexeme == null) {
            errorWriter.write("lexeme not found for char '$char'", scanPos.line, scanPos.column)
            lexeme = Lexeme(LexemeType.ERROR, "$char", scanPos.line, scanPos.column)
            scanPos.increaseColumn()
        }

        return lexeme
    }

    private inline fun skipIgnoredChars(): Lexeme? {
        while (true) {
            when (char) { // аналог switch
                ' ', '\t' -> scanPos.increaseColumn()
                '\n' -> scanPos.increaseLine()
                '/' -> {
                    if (nextChar == '/') { // обработка однострочного комментария
                        scanPos.increaseColumn(2)
                        while (char != SpecChars.fileEnd && char != '\n') {
                            scanPos.increaseColumn()
                        }
                    } else if (nextChar == '*') { // обработка многострочного комментария
                        // на случай незакрытого комментария в конце файла
                        val commentBegin = scanPos.clone()

                        scanPos.increaseColumn(2)
                        while (char != SpecChars.fileEnd && !(char == '*' && nextChar == '/')) {
                            if (char == '\n') {
                                scanPos.increaseLine()
                            } else {
                                scanPos.increaseColumn()
                            }
                        }

                        if (nextChar == SpecChars.fileEnd) {
                            errorWriter.write("unclosed multiline comment", commentBegin.line, commentBegin.column)
                            return Lexeme(LexemeType.ERROR, "/*", commentBegin.line, commentBegin.column)
                        } else {
                            scanPos.increaseColumn(2)
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

    private inline fun getAlphabetLexeme(): Lexeme? {
        if (!char.isLetter() && char != '_') {
            return null
        }

        val lexBegin = scanPos.position
        while ((char.isLetterOrDigit() || char == '_') && (scanPos.position - lexBegin <= maxLexemeLength)) {
            scanPos.increaseColumn()
        }

        val image = src.substring(lexBegin, scanPos.position)

        val type = if (image.length > maxLexemeLength) {
            errorWriter.write("too long lexeme", scanPos.line, scanPos.column - image.length)
            LexemeType.ERROR
        } else {
            when (image) {
                "char" -> LexemeType.CHAR
                "short" -> LexemeType.SHORT_INT
                "long" -> LexemeType.LONG_INT
                "int" -> LexemeType.INT
                "class" -> LexemeType.CLASS
                "main" -> LexemeType.MAIN_FUNC
                "while" -> LexemeType.WHILE
                else -> LexemeType.IDENTIFIER
            }
        }

        return Lexeme(type, image, scanPos.line, scanPos.column - image.length)
    }

    private inline fun getNumericLexeme(): Lexeme? {
        if (!isDecimal(char)) {
            return null
        }

        val numberBegin = scanPos.clone()

        var type = if (char == '0') {
            if(nextChar == 'x' || nextChar == 'X') {
                scanPos.increaseColumn(2)
                if (isHex(char)) {
                    LexemeType.CONST16
                } else {
                    errorWriter.write("invalid digit '$char' in hex constant",
                        numberBegin.line, numberBegin.column)
                    LexemeType.ERROR
                }
            } else {
                scanPos.increaseColumn()
                LexemeType.CONST8
            }
        } else {
            LexemeType.CONST10
        }

        val scanNumber = { isRightDigit: (Char) -> Boolean ->
            while (isRightDigit(char) && (scanPos.position - numberBegin.position <= maxLexemeLength)) {
                scanPos.increaseColumn()
            }
        }

        when (type) {
            LexemeType.CONST8 -> {
                scanNumber(::isOctal)
                if (char in '8'..'9') {
                    errorWriter.write("invalid digit '$char' in octal constant",
                        numberBegin.line, numberBegin.column)
                    type = LexemeType.ERROR
                }
            }
            LexemeType.CONST10 -> scanNumber(::isDecimal)
            LexemeType.CONST16 -> scanNumber(::isHex)
            else -> Unit // иначе ничего не происходит
        }

        val image = src.substring(numberBegin.position, scanPos.position)
        if (image.length > maxLexemeLength) {
            errorWriter.write("too long lexeme", numberBegin.line, numberBegin.column)
            type = LexemeType.ERROR
        }

        return Lexeme(type, image, numberBegin.line, numberBegin.column)
    }

    private inline fun getSymbolLexeme(): Lexeme? {
        var image = "$char"

        val type = when (char) {
            '=' -> {
                if (nextChar == '=') {
                    image += "$nextChar"
                    LexemeType.EQUAL
                }
                else LexemeType.ASSIGNMENT
            }
            '!' -> {
                if (nextChar == '=') {
                    image += "$nextChar"
                    LexemeType.NOT_EQUAL
                }
                else {
                    errorWriter.write("invalid lexeme '$image'", scanPos.line, scanPos.column)
                    LexemeType.ERROR
                }
            }
            '<' -> {
                when (nextChar) {
                    '=' -> {
                        image += "$nextChar"
                        LexemeType.LESS_OR_EQUAL
                    }
                    '<' -> {
                        image += "$nextChar"
                        LexemeType.SHIFT_LEFT
                    }
                    else -> LexemeType.LESS
                }
            }
            '>' -> {
                when (nextChar) {
                    '=' -> {
                        image += "$nextChar"
                        LexemeType.GREATER_OR_EQUAL
                    }
                    '>' -> {
                        image += "$nextChar"
                        LexemeType.SHIFT_RIGHT
                    }
                    else -> LexemeType.GREATER
                }
            }
            '+' -> {
                if (nextChar == '+') {
                    image += "$nextChar"
                    LexemeType.INCREMENT
                }
                else LexemeType.PLUS
            }
            '-' -> {
                if (nextChar == '-') {
                    image += "$nextChar"
                    LexemeType.DECREMENT
                }
                else LexemeType.MINUS
            }
            '*' -> LexemeType.MULTIPLICATION
            '/' -> LexemeType.DIVISION
            '%' -> LexemeType.REMAINDER
            ';' -> LexemeType.SEMICOLON
            ',' -> LexemeType.COMMA
            '{' -> LexemeType.BRACE_LEFT
            '}' -> LexemeType.BRACE_RIGHT
            '[' -> LexemeType.BRACKET_LEFT
            ']' -> LexemeType.BRACKET_RIGHT
            '(' -> LexemeType.PARENTHESIS_LEFT
            ')' -> LexemeType.PARENTHESIS_RIGHT
            SpecChars.fileEnd -> {
                image = ""
                LexemeType.END
            }

            else -> return null
        }

        val lexeme = Lexeme(type, image, scanPos.line, scanPos.column)
        scanPos.increaseColumn(image.length)
        return lexeme
    }
}


private inline fun isOctal(ch: Char): Boolean
    = ch in '0'..'7'

private inline fun isDecimal(ch: Char): Boolean
    = ch.isDigit()

private inline fun isHex(ch: Char): Boolean
    = isDecimal(ch) || ch in 'a'..'f' || ch in 'A'..'F'
