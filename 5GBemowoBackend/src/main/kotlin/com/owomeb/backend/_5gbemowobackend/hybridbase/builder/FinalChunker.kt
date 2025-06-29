package com.owomeb.backend._5gbemowobackend.hybridbase.builder


import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


class FinalChunker(
    private val pureMarkdownPath: String,
    private val outputPath: String,
    private val minChunkLen: Int = 200,
    private val maxChunkLen: Int = 400
) {
    fun splitTextByAllHeaders(input: Pair<String, Int>): List<Pair<String, Int>> {
        val (text, level) = input
        val nextLevel = level + 1
        val pattern = "^${"#".repeat(nextLevel)}\\s+".toRegex(RegexOption.MULTILINE)

        val splitChunks = text.split(pattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (splitChunks.size == 1) {
            return listOf(Pair(text, level))
        }

        val result = mutableListOf<Pair<String, Int>>()
        for (chunk in splitChunks) {
            result.addAll(splitTextByAllHeaders(Pair(chunk, nextLevel)))
        }
        return result
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    fun processChunks(chunks: List<Pair<String, Int>>): List<Quadruple<String, String, Int, String>> {

        val result = mutableListOf<Quadruple<String, String, Int, String>>()
        val hierarchy = mutableMapOf<Int, String>()

        chunks.forEach { (chunk, level) ->
            val lines = chunk.lines()
            val title = lines.firstOrNull()?.take(100)?.trim() ?: "Untitled"
            val content = lines.drop(1).joinToString("\n").trim()

            hierarchy[level] = title

            val mergedTitle = (0..level).mapNotNull { hierarchy[it] }.joinToString(", ")

            if (content.isNotBlank()) {
                result.add(Quadruple(title, content, level, mergedTitle))
            } else {
                println("Empty chunk deleted: $title")
            }
        }
        return result
    }





    private fun printChunkInfo(chunks: List<Pair<String, Int>>) {
        println("\n**Chunks analyse:**")

        val wordCounts = chunks.map { (chunk, _) ->
            chunk.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        }

        val averageLength = wordCounts.average()
        val aboveMaxLen = wordCounts.count { it > maxChunkLen }
        val belowMinLen = wordCounts.count { it < minChunkLen }

        val longestChunks = wordCounts.sortedDescending().take(10)
        val shortestChunks = wordCounts.sorted().take(10)

        println("Average chunk length (in words) is: %.2f".format(averageLength))
        println("The number of chunks above $maxChunkLen words: $aboveMaxLen")
        println("Number of chunks below $minChunkLen words: $belowMinLen")


        println("\nTop ten longest chunks")
        longestChunks.forEachIndexed { index, len ->
            println("#${index + 1}: $len words")
        }

        println("\nTop ten shortest chunks")
        shortestChunks.forEachIndexed { index, len ->
            println("#${index + 1}: $len words")
        }

        println("\nChunk headers")
        chunks.forEachIndexed { index, (chunk, level) ->
            val firstLine = chunk.lines().firstOrNull()?.take(100) ?: "No data"
            //println("#$index [Level $level]: $firstLine")
        }

        val oneLiners = chunks.count { (chunk, lvl) ->
            chunk.lines().size == 1
        }

        println("One liners $oneLiners")

    }

    private fun printQuadrupleInfo(chunks: List<Quadruple<String, String, Int, String>>) {
        println("\nChunks analysis")

        println("Number of chunks: ${chunks.size}")

        val wordCounts = chunks.map { (title, content, _, _) ->
            content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        }

        val averageLength = wordCounts.average()
        val aboveMaxLen = wordCounts.count { it > maxChunkLen }
        val belowMinLen = wordCounts.count { it < minChunkLen }

        val longestChunks = chunks.zip(wordCounts)
            .sortedByDescending { it.second }
            .take(10)

        val shortestChunks = chunks.zip(wordCounts)
            .sortedBy { it.second }
            .take(10)

        println("Average chunk length (in words): %.2f".format(averageLength))
        println("Number of chunks above $maxChunkLen words: $aboveMaxLen")
        println("Number of chunks below $minChunkLen words: $belowMinLen")

        println("\nTop 10 longest chunks (word count):")
        longestChunks.forEachIndexed { index, (chunk, len) ->
            println("#${index + 1}: $len words - ${chunk.first}")
        }

        println("\nAll chunks below $minChunkLen words:")

        val shortChunks = chunks.filter { (_, content, _, _) ->
            content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size < 20
        }

        shortChunks.forEachIndexed { index, (title, content, level, mergedTitle) ->
            val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            println("\nChunk #$index [Level $level]")
            println("Title: $title")
            println("Merged Title: $mergedTitle")
            println("Word count: $wordCount")
            println("Content:\n$content")
            println("----")
        }

        //println("\nChunk headers with levels:")
        chunks.forEachIndexed { index, (title, _, level, mergedTitle) ->
            // println("#$index [Level $level] |||  Merged Title: $mergedTitle")
        }

        val oneLiners = chunks.count { (_, content, _, _) ->
            content.lines().size == 1
        }
        println("\nChunks that consist of a single line: $oneLiners")
    }

    private fun mergeShortChunks(chunks: List<Quadruple<String, String, Int, String>>): List<Quadruple<String, String, Int, String>> {
        val result = mutableListOf<Quadruple<String, String, Int, String>>()
        var tempContent = ""
        var tempMergedTitle = ""
        var tempLevel: Int? = null

        for ((title, content, level, mergedTitle) in chunks) {
            val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

            if (wordCount < minChunkLen) {
                tempContent += if (tempContent.isEmpty()) "$title\n$content" else "\n\n$title\n$content"
                tempMergedTitle = mergedTitle
                if (tempLevel == null) tempLevel = level
            } else {
                if (tempContent.isNotEmpty()) {
                    val newContent = tempContent + "\n\n" + content
                    result.add(Quadruple(title, newContent.trim(), level, tempMergedTitle))

                    tempContent = ""
                    tempMergedTitle = ""
                    tempLevel = null
                } else {
                    result.add(Quadruple(title, content, level, mergedTitle))
                }
            }
        }
        if (tempContent.isNotEmpty() && tempLevel != null) {
            result.add(Quadruple("Merged Chunk", tempContent.trim(), tempLevel, tempMergedTitle))
        }
        return result
    }

    fun splitLongChunk(
        chunks: List<Quadruple<String, String, Int, String>>,
        desirableLen: Int
    ): List<Quadruple<String, String, Int, String>> {

        val minOverlap = desirableLen / 10
        val result = mutableListOf<Quadruple<String, String, Int, String>>()

        for ((title, content, level, mergedTitle) in chunks) {
            val words = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            if (words.size <= (desirableLen * 1.35).toInt()) {
                result.add(Quadruple(title, content, level, mergedTitle))
                continue
            }


            val additionalWords = mergedTitle.split("\\s+".toRegex()).size + 10 + 7
            val availableWords = desirableLen - additionalWords

            if (availableWords <= 0) {
                println("ERROR: mergedTitle + additional info exceeds desirable length. Skipping chunk: $title")
                continue
            }


            val fragmentCount = Math.ceil(words.size / availableWords.toDouble()).toInt()


            val overlap = Math.max(minOverlap, (fragmentCount * availableWords - words.size) / (fragmentCount - 1))

            if (overlap < minOverlap) {
                println("ERROR: Calculated overlap is smaller than minimum allowed. Skipping chunk: $title")
                continue
            }

            println("Splitting chunk: $title into $fragmentCount parts, overlap: $overlap")

            for (i in 0 until fragmentCount) {
                val start = i * (availableWords - overlap)
                val end = minOf(start + availableWords, words.size)

                if (start >= end) {
                    println("Invalid range detected. start: $start, end: $end, chunk skipped.")
                    break
                }

                val fragmentWords = words.subList(start, end)
                val fragmentText = fragmentWords.joinToString(" ")

                val formattedText = """
            This part of the chunk exists mainly for context: $mergedTitle
            (This fragment is part of the section, it is fragment ${i + 1}/$fragmentCount)
            
            $fragmentText
            
            ${if (i < fragmentCount - 1) "(Below is part of fragment ${i + 2}/$fragmentCount)" else ""}
            """.trimIndent()

                val newTitle = "$title [Fragment ${i + 1}/$fragmentCount]"
                result.add(Quadruple(newTitle, formattedText, level, mergedTitle))
            }
        }

        return result
    }

    fun process() {
        val inputText = File(pureMarkdownPath).readText()
        val chunks2 = splitTextByAllHeaders(Pair(inputText, 0))

        printChunkInfo(chunks2)
        val processedChunks = processChunks(chunks2)
        printQuadrupleInfo(processedChunks)
        val merged = mergeShortChunks(processedChunks)
        printQuadrupleInfo(merged)
        val trimmedTittles = trimLongMergedTitles(
            merged,
            maxWords = (maxChunkLen + minChunkLen)/4
        )

        val split = splitLongChunk(trimmedTittles, (maxChunkLen + minChunkLen)/2)

        printQuadrupleInfo(split)
        saveChunksToJsonFile(
            split,
            outputPath = outputPath
        )
    }

    private fun trimLongMergedTitles(
        chunks: List<Quadruple<String, String, Int, String>>,
        maxWords: Int
    ): List<Quadruple<String, String, Int, String>> {

        println("Trimming titles")
        val result = mutableListOf<Quadruple<String, String, Int, String>>()

        for ((title, content, level, mergedTitle) in chunks) {
            val mergedTitleWords = mergedTitle.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            var newMergedTitle = mergedTitle

            if (mergedTitleWords.size > maxWords) {
                val cutLength = mergedTitleWords.size - maxWords
                newMergedTitle = mergedTitleWords.takeLast(maxWords).joinToString(" ")
                println("MergedTitle trimmed by $cutLength words: [$mergedTitle] → [$newMergedTitle]")
            }
            result.add(Quadruple(title, content, level, newMergedTitle))
        }

        return result
    }


    fun cleanChunkText(chunk: String): String {
        return chunk
            .replace(Regex("(?m)^This part of the chunk exists.*\\n?"), "")
            .replace(Regex("(?m)^\\(This fragment is part.*\\)\\n?"), "")
            .replace(Regex("(?m)^\\(Below is part.*\\)\\n?"), "")
            .trim()
    }

    private fun saveChunksToJsonFile(chunks: List<Quadruple<String, String, Int, String>>, outputPath: String) {
        val outputFile = File(outputPath)
        outputFile.parentFile.mkdirs()

        val jsonChunks = chunks.mapIndexed { idx, (title, content, level, mergedTitle) ->
            val cleanContent = cleanChunkText(content)
            ChunkJson(idx, title, content, cleanContent, level, mergedTitle)
        }



        val json = Json { prettyPrint = true }.encodeToString(ChunkJsonWrapper(jsonChunks))

        outputFile.writeText(json)
        println("Zapisano ${chunks.size} chunków do ${outputFile.absolutePath}")
    }

    @Serializable
    data class ChunkJsonWrapper(val chunks: List<ChunkJson>)

    @Serializable
    data class ChunkJson(
        val index: Int,
        val title: String,
        val content: String,
        val cleanContent: String,
        val level: Int,
        val mergedTitle: String
    )



}
