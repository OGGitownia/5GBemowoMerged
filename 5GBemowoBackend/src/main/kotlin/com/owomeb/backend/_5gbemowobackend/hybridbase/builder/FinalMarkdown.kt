package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream


@Component
class FinalMarkdown {

    fun doMarkdowning(inputPath: String, outputPath: String) {
        val inputFile = File(inputPath)
        if (inputFile.extension != "docx")
            println("Final markdown received format: ${inputFile.extension}, expected docx")
        else
            println("Found appropriate format to markdowning")



        val docx = XWPFDocument(FileInputStream(inputFile))
        val allElements = docx.bodyElements
        val forewordIndices = mutableListOf<Int>()
        for ((index, element) in allElements.withIndex()) {
            if (element is XWPFParagraph) {
                for (run in element.runs) {
                    val runText = run.text()?.trim()
                    if (runText != null && runText.equals("foreword", ignoreCase = true)) {
                        forewordIndices.add(index)
                        break
                    }
                }
            }
        }

        if (forewordIndices.size != 2) {
            println("Found ${forewordIndices.size} 'foreword'.")
            for ((index, element) in allElements.withIndex()) {
                if (element is XWPFParagraph) {
                    for (run in element.runs) {
                        val runText = run.text()?.trim()
                        if (runText?.contains("foreword", ignoreCase = true) == true) {
                            //println("• [index=$index] RUN='$runText', paragraphStyle='${element.style ?: "brak"}'")
                        }
                    }
                }
            }
            throw IllegalStateException("Expected exactly 2 occurrences of the word 'foreword'")
        }

        val secondIndex = forewordIndices[1]
        println("Found two occurrences of 'foreword'. Removing everything up to and including index $secondIndex.")
        for (i in secondIndex downTo 0) {
            docx.removeBodyElement(i)
        }

        val output = StringBuilder()
        for (element in docx.bodyElements) {
            when (element) {
                is XWPFParagraph -> {
                    val style = element.style ?: ""
                    val headingLevel = when {
                        style.startsWith("Heading", ignoreCase = true) ->
                            style.removePrefix("Heading").toIntOrNull()?.coerceIn(1, 6) ?: 0
                        else -> 0
                    }

                    val rawText = element.runs.joinToString("") { it.text() ?: "" }
                    val cleanText = sanitize(rawText)

                    if (cleanText.isNotEmpty()) {
                        if (headingLevel > 0) {
                            output.appendLine("#".repeat(headingLevel) + " " + cleanText)
                        } else {
                            output.appendLine(cleanText)
                        }
                    }
                }
                is XWPFTable -> {
                    val rows = element.rows
                    if (rows.isNotEmpty()) {
                        output.appendLine("<table>")
                        for ((rowIndex, row) in rows.withIndex()) {
                            output.appendLine("  <tr>")
                            for (cell in row.tableCells) {
                                val cellText = sanitize(cell.text.replace("\\n", " "))
                                val tag = if (rowIndex == 0) "th" else "td"
                                output.appendLine("    <$tag>$cellText</$tag>")
                            }
                            output.appendLine("  </tr>")
                        }
                        output.appendLine("</table>")
                    }
                }
            }
        }
        File(outputPath).writeText(output.toString().trim(), Charsets.UTF_8)
        println("Saved cleaned markdown with UTF-8 and HTML tables to: $outputPath")
    }
    fun sanitize(text: String): String {
        var cleaned = text
        val log = mutableListOf<String>()

        if (cleaned.contains('\u00A0')) {
            cleaned = cleaned.replace('\u00A0', ' ')
            log.add("NBSP (U+00A0) → space")
        }
        if (cleaned.contains('\u200B')) {
            cleaned = cleaned.replace("\u200B", "")
            log.add("zero-width space (U+200B) → removed")
        }
        if (cleaned.contains('\t')) {
            cleaned = cleaned.replace("\t", " ")
            log.add("tab (U+0009) → space")
        }
        if (cleaned.contains('\u201C')) {
            cleaned = cleaned.replace("\u201C", "\"")
            log.add("smart quote (U+201C) → \"")
        }
        if (cleaned.contains('\u201D')) {
            cleaned = cleaned.replace("\u201D", "\"")
            log.add("smart quote (U+201D) → \"")
        }
        if (Regex("\\s{2,}").containsMatchIn(cleaned)) {
            cleaned = cleaned.replace("\\s+".toRegex(), " ")
            log.add("multiple spaces → single")
        }

        cleaned = cleaned.trim()

        if (log.isNotEmpty()) {
            // println("Sanitizing text: \"$original\"")
            log.forEach { println(" - $it") }
            // println("➡ After sanitization: \"$cleaned\"")
        }
        return cleaned
    }
}