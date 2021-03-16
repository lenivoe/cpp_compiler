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
                        scanPos.increaseColumn(2)
                        while (char != SpecChars.fileEnd && !(char == '*' && nextChar == '/')) {
                            if (char == '\n') {
                                scanPos.increaseLine()
                            } else {
                                scanPos.increaseColumn()
                            }
                        }

                        assert(src.last() == SpecChars.fileEnd && src.last() == src[src.length - 2]) {
                            "source must have '\\0\\0' at the end"
                        }

                        if (nextChar == SpecChars.fileEnd) {
                            errorWriter.write("unclosed multiline comment", scanPos.line, scanPos.column)
                            return Lexeme(LexemeType.ERROR, "/*", scanPos.line, scanPos.column)
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
        if (!char.isDigit()) {
            return null
        }

        val lexBegin = scanPos.position

        var type = if (char == '0') {
            if(nextChar == 'x' || nextChar == 'X') {
                scanPos.increaseColumn(2)
                LexemeType.CONST16
            } else {
                scanPos.increaseColumn()
                LexemeType.CONST8
            }
        } else {
            LexemeType.CONST10
        }

        // способ проверки цифры в зависимости от предварительного типа лексемы
        val checkDigit: (Char) -> Boolean = when (type) {
            LexemeType.CONST8 -> { ch -> ch in '0'..'7' }
            LexemeType.CONST10 -> Char::isDigit
            LexemeType.CONST16 -> { ch -> ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F' }
            else -> throw Exception("unreachable code")
        }

        while(checkDigit(char) && (scanPos.position - lexBegin <= maxLexemeLength)) {
            scanPos.increaseColumn()
        }

        val image = src.substring(lexBegin, scanPos.position)
        if (image.length > maxLexemeLength) {
            errorWriter.write("too long lexeme", scanPos.line, scanPos.column - image.length)
            type = LexemeType.ERROR
        }

        return Lexeme(type, image, scanPos.line, scanPos.column - image.length)
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
                if (nextChar == '+') {
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
