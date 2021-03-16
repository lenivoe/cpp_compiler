import com.common.CommonException
import com.common.errors.FileErrorWriter
import com.common.SourceLoader
import com.scanner.Scanner
import com.scanner.lexemes.Lexeme
import com.scanner.lexemes.LexemeType
import java.io.FileWriter
import java.lang.Exception

fun testScanner(testFileName: String, outputFileName: String, errorsFileName: String) {
    val lexemeWriter = FileWriter(outputFileName)
    val errorWriter = FileErrorWriter(errorsFileName)
    val sourceLoader = SourceLoader()

    try {
        sourceLoader.loadFile(testFileName)
        val scanner = Scanner(sourceLoader.source, errorWriter)

        val lexemeList = mutableListOf<Lexeme>()
        do {
            val lexeme = scanner.findNextLexeme()
            lexemeList.add(lexeme)
        } while (lexeme.type != LexemeType.END)

        val srcLines = sourceLoader.source.split('\n')
        for (lexeme in lexemeList) {
            lexemeWriter.appendLine(lexeme.toString())

            // проверка соответвия лексем и их позиций в тексте
            val (_, image, line, column) = lexeme
            val sub = srcLines[line-1].substring(column-1, column - 1 + image.length)
            if(lexeme.image != sub) {
                println("wrong lexeme position $lexeme")
            }
        }

    } catch (ex: CommonException) {
        errorWriter.write("common error: $ex")
    } catch (ex: Exception) {
        errorWriter.write("unidentified error: $ex")
    } finally {
        lexemeWriter.close()
        errorWriter.close()
    }
}

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    testScanner("scanner_test.cpp", "lexemes.log","errors.log")
}
