package com.owomeb.backend._5gbemowobackend.api


import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import org.springframework.core.io.FileSystemResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api/chat")
class ChatController(private val appPathsConfig: AppPathsConfig) {

    @GetMapping("/available-models/{baseId}")
    fun getAvailableModels(@PathVariable baseId: Long): ResponseEntity<List<String>> {
        val result = ChatModel.entries.map { it.name }
        println("Zwracam modele: $result")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/available-tuners/{baseId}")
    fun getAvailableTuners(
        @PathVariable baseId: Long, @RequestParam model: String
    ): ResponseEntity<List<String>> {
        val result = AnswerTuner.entries.map { it.name }
        println("Zwracam tunery: $result")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/chunks/{baseId}")
    fun getChunks(@PathVariable baseId: String): ResponseEntity<FileSystemResource> {
        println("Zwracam plik Michael'a Jacksona")
        val path = appPathsConfig.getChunkedJsonPath(baseId)
        val file = File(path)

        return if (file.exists()) {
            ResponseEntity.ok(FileSystemResource(file))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

enum class ChatModel {
    LLaMA_3_8B_Q4_0,
    CHAT_GPT4_1,
    GEMINI_2_5_PRO
}


enum class AnswerTuner {

}
