import com.common.CommonException
import com.common.errors.FileLogger
import com.common.SourceLoader
import com.lexer.Lexer
import com.lexer.token.Token
import com.lexer.token.TokenType
import java.io.FileWriter
import java.lang.Exception

fun testScanner(testFileName: String, outputFileName: String, errorsFileName: String) {
    val tokenWriter = FileWriter(outputFileName)
    val logger = FileLogger(errorsFileName)
    val sourceLoader = SourceLoader()

    try {
        sourceLoader.loadFile(testFileName)
        val lexer = Lexer(sourceLoader.source, logger)

        val lexemeList = mutableListOf<Token>()
        do {
            val lexeme = lexer.findNextToken()
            lexemeList.add(lexeme)
        } while (lexeme.type != TokenType.END)

        val srcLines = sourceLoader.source.split('\n')
        for (lexeme in lexemeList) {
            tokenWriter.appendLine(lexeme.toString())

            // проверка соответвия лексем и их позиций в тексте
            val (_, image, line, column) = lexeme
            val sub = srcLines[line-1].substring(column-1, column - 1 + image.length)
            if(lexeme.image != sub) {
                println("wrong lexeme position $lexeme")
            }
        }

    } catch (e: CommonException) {
        logger.error("common exception: $e")
    } catch (e: Exception) {
        logger.error("unidentified error: $e")
    } finally {
        tokenWriter.close()
        logger.close()
    }
}

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    testScanner("test_data/lexer_test_no_errors.cpp", "tokens_no_errors.log", "lexer_no_errors.log")
    testScanner("test_data/lexer_test_with_errors.cpp", "tokens_with_errors.log", "lexer_with_errors.log")
}
