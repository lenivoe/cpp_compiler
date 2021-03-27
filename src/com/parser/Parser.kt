@file:Suppress("NOTHING_TO_INLINE")

package com.parser

import com.common.CommonException
import com.common.errors.ILogger
import com.lexer.Lexer
import com.lexer.token.Token
import com.lexer.token.TokenType
import com.lexer.token.isBasicDataType


class Parser(
    private val lexer: Lexer,
    private val logger: ILogger
) {
    /** ветвь: программа */
    fun parseProgram() {
        try {
            while (true) {
                val prevPos = lexer.savePosition()
                val token = lexer.skip(1).findNextToken()
                lexer.restorePosition(prevPos)

                when (token.type) {
                    TokenType.END -> break
                    TokenType.MAIN_FUNC -> parseMainFunction()
                    else -> parseDescription()
                }
            }
            logger.log("parser has no errors")
        } catch (e: CommonException) {
            logger.error("syntax error: ${e.message}")
        }
    }

    /** ветвь: описание */
    private fun parseDescription() {
        var prevPos = lexer.savePosition()
        var token = lexer.findNextToken()

        if (token.type.isBasicDataType || token.type === TokenType.IDENTIFIER) {
            if (token.type === TokenType.SHORT_INT || token.type === TokenType.LONG_INT) {
                prevPos = lexer.savePosition()
                token = lexer.findNextToken()
                if (token.type !== TokenType.INT) {
                    lexer.restorePosition(prevPos)
                }
            }

            do {
                throwExceptionIfNextTokenWrong(TokenType.IDENTIFIER, "identifier expected")

                prevPos = lexer.savePosition()
                token = lexer.findNextToken()

                when (token.type) {
                    TokenType.ASSIGNMENT -> parseExpression()
                    TokenType.BRACKET_LEFT -> {
                        parseConstant()
                        throwExceptionIfNextTokenWrong(TokenType.BRACKET_RIGHT, "']' expected")
                    }
                    else -> continue
                }

                // когда (token.type === TokenType.EQUAL || token.type === TokenType.BRACKET_RIGHT)
                prevPos = lexer.savePosition()
                token = lexer.findNextToken()

            } while (token.type === TokenType.COMMA)

            lexer.restorePosition(prevPos)
        }
        else if (token.type === TokenType.CLASS) {
            lexer.restorePosition(prevPos)
            parseClassDescription()
        }
        else {
            throw ParserException("data type or 'class' keyword expected", token)
        }

        throwExceptionIfNextTokenWrong(TokenType.SEMICOLON, "';' expected")
    }

    /** ветвь: описание класса */
    private fun parseClassDescription() {
        throwExceptionIfNextTokenWrong(TokenType.CLASS, "'class' keyword expected")
        throwExceptionIfNextTokenWrong(TokenType.IDENTIFIER, "identifier expected")
        throwExceptionIfNextTokenWrong(TokenType.BRACE_LEFT, "'{' expected")

        while (true) {
            val prevPos = lexer.savePosition()
            val token = lexer.findNextToken()

            when (token.type) {
                TokenType.BRACE_RIGHT -> break
                TokenType.END -> throw ParserException("'}' expected", token)
                else -> {}
            }
            lexer.restorePosition(prevPos)

            parseDescription()
        }
    }

    /** ветвь: составной оператор */
    private fun parseCompoundOperator() {
        throwExceptionIfNextTokenWrong(TokenType.BRACE_LEFT, "'{' expected")

        while (true) {
            val prevPos = lexer.savePosition()
            val token = lexer.findNextToken()

            when (token.type) {
                TokenType.BRACE_RIGHT -> break
                TokenType.END -> throw ParserException("'}' expected", token)
                else -> {}
            }

            val nextToken = lexer.findNextToken()
            lexer.restorePosition(prevPos)

            val isDescription = token.type === TokenType.CLASS
                    || token.type.isBasicDataType
                    || (token.type === TokenType.IDENTIFIER && nextToken.type === TokenType.IDENTIFIER)
            if (isDescription) {
                parseDescription()
            } else {
                parseOperator()
            }
        }
    }

    /** ветвь: main */
    private fun parseMainFunction() {
        throwExceptionIfNextTokenWrong(TokenType.INT, "'int' keyword expected before 'main'")
        throwExceptionIfNextTokenWrong(TokenType.MAIN_FUNC, "'main' keyword expected")
        throwExceptionIfNextTokenWrong(TokenType.PARENTHESIS_LEFT, "'(' expected")
        throwExceptionIfNextTokenWrong(TokenType.PARENTHESIS_RIGHT, "')' expected")

        parseCompoundOperator()
    }

    /** ветвь: оператор */
    private fun parseOperator() {
        val prevPos = lexer.savePosition()
        val token = lexer.findNextToken()
        lexer.restorePosition(prevPos)

        when (token.type) {
            TokenType.WHILE -> parseWhile()
            TokenType.BRACE_LEFT -> parseCompoundOperator()
            else -> {
                if (token.type === TokenType.IDENTIFIER) {
                    parseAssignment()
                } else if (token.type === TokenType.RETURN) {
                    lexer.skip(1)
                    parseExpression()
                }
                throwExceptionIfNextTokenWrong(TokenType.SEMICOLON, "';' expected")
            }
        }
    }

    /** ветвь: присваивание */
    private fun parseAssignment() {
        parseLeftSideValue()
        throwExceptionIfNextTokenWrong(TokenType.ASSIGNMENT, "'=' expected")
        parseExpression()
    }

    /** ветвь: левосторннее значение */
    private fun parseLeftSideValue() {
        throwExceptionIfNextTokenWrong(TokenType.IDENTIFIER, "identifier expected")

        var prevPos = lexer.savePosition()
        var token = lexer.findNextToken()

        if (token.type === TokenType.BRACKET_LEFT) {
            parseExpression()
            throwExceptionIfNextTokenWrong(TokenType.BRACKET_RIGHT, "']' expected")

            prevPos = lexer.savePosition()
            token = lexer.findNextToken()
        }

        while (token.type === TokenType.DOT) {
            throwExceptionIfNextTokenWrong(TokenType.IDENTIFIER, "identifier expected")

            prevPos = lexer.savePosition()
            token = lexer.findNextToken()

            if (token.type === TokenType.BRACKET_LEFT) {
                parseExpression()
                throwExceptionIfNextTokenWrong(TokenType.BRACKET_RIGHT, "']' expected")

                prevPos = lexer.savePosition()
                token = lexer.findNextToken()
            }
        }

        lexer.restorePosition(prevPos)
    }

    /** ветвь: while */
    private fun parseWhile() {
        throwExceptionIfNextTokenWrong(TokenType.WHILE, "'while' keyword expected")
        throwExceptionIfNextTokenWrong(TokenType.PARENTHESIS_LEFT, "'(' expected")
        parseExpression()
        throwExceptionIfNextTokenWrong(TokenType.PARENTHESIS_RIGHT, "')' expected")
        parseOperator()
    }

    /** ветвь: выражение */
    private fun parseExpression() = createBinaryOperation(::parseGreaterOrLesserOperation) { token ->
        token.type === TokenType.EQUAL || token.type === TokenType.NOT_EQUAL
    }

    /** ветвь: операция меньше либо равно или больше либо равно */
    private fun parseGreaterOrLesserOperation() = createBinaryOperation(::parseShiftOperation) { token ->
        token.type === TokenType.LESS_OR_EQUAL
                || token.type === TokenType.GREATER_OR_EQUAL
                || token.type === TokenType.LESS
                || token.type === TokenType.GREATER
    }

    /** ветвь: операция приоритета сдвига */
    private fun parseShiftOperation() = createBinaryOperation(::parseSumOperation) { token ->
        token.type === TokenType.SHIFT_LEFT || token.type === TokenType.SHIFT_RIGHT
    }

    /** ветвь: операция приоритета суммирования */
    private fun parseSumOperation() = createBinaryOperation(::parseMultiplicationOperation) { token ->
        token.type === TokenType.PLUS || token.type === TokenType.MINUS
    }

    /** ветвь: операция приоритета умножения */
    private fun parseMultiplicationOperation() = createBinaryOperation(::parsePrefixOperation) { token ->
        token.type === TokenType.MULTIPLICATION
                || token.type === TokenType.DIVISION
                || token.type === TokenType.REMAINDER
    }

    /** ветвь: префиксная операция */
    private fun parsePrefixOperation() {
        val prevPos = lexer.savePosition()
        val token = lexer.findNextToken()

        val isPrefixOperation = token.type === TokenType.PLUS
                || token.type === TokenType.MINUS
                || token.type === TokenType.INCREMENT
                || token.type === TokenType.DECREMENT
        if (!isPrefixOperation) {
            lexer.restorePosition(prevPos)
        }

        parsePostfixOperation()
    }

    /** ветвь: постфиксная операция */
    private fun parsePostfixOperation() {
        parseElementaryExpression()

        val prevPos = lexer.savePosition()
        val token = lexer.findNextToken()
        if (token.type !== TokenType.INCREMENT && token.type !== TokenType.DECREMENT) {
            lexer.restorePosition(prevPos)
        }
    }

    /** ветвь: элементарное выражение */
    private fun parseElementaryExpression() {
        val prevPos = lexer.savePosition()
        val token = lexer.findNextToken()
        when (token.type) {
            TokenType.IDENTIFIER -> {
                lexer.restorePosition(prevPos)
                parseLeftSideValue()
            }
            TokenType.PARENTHESIS_LEFT -> {
                parseExpression()
                throwExceptionIfNextTokenWrong(TokenType.PARENTHESIS_RIGHT, "')' expected")
            }
            else -> {
                lexer.restorePosition(prevPos)
                parseConstant()
            }
        }
    }

    /** ветвь: константа */
    private fun parseConstant() {
        val token = lexer.findNextToken()
        val isConst = token.type === TokenType.CONST8
                || token.type === TokenType.CONST10
                || token.type === TokenType.CONST16
        if (!isConst) {
            throw ParserException("octal, decimal or hex constant expected", token)
        }
    }


    //
    // вспомогательные методы
    //

    /**
     * Проверяет следующую лексему. Если ее тип не соответствует указанному, выбрасывает исключение.
     *
     * После выполнения, позиция в лексическом анализаторе смещается к следующей лексеме.
     */
    private inline fun throwExceptionIfNextTokenWrong(expectedType: TokenType, errMsg: String) {
        val token = lexer.findNextToken()
        if (token.type !== expectedType) {
            throw ParserException(errMsg, token)
        }
    }

    /** Шаблон бинарной операции. Будет выполнена полная подстановка кода, никаких лишних вызовов. */
    private inline fun createBinaryOperation(nextOperation: () -> Unit, isCorrectToken: (Token) -> Boolean) {
        nextOperation()

        while (true) {
            val prevPos = lexer.savePosition()
            val token = lexer.findNextToken()
            if (!isCorrectToken(token)) {
                lexer.restorePosition(prevPos)
                break
            }
            nextOperation()
        }
    }
}
