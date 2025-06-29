package com.owomeb.backend._5gbemowobackend.messageBank

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.owomeb.backend._5gbemowobackend.api.MessageSocketHandler
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.jpa.repository.JpaContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.io.File
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit






@Service
class MessageService(
    @Value("\${google.api.key}") private val googleApiKey: String,
    @Value("\${google.api.model}") private val googleApiModel: String,
    @Value("\${google.api.baseurl}") private val googleApiBaseUrl: String,
    @Value("\${openai.api.key}") private val openAiApiKey: String,

    private val messageRepository: MessageRepository,
    private val hybridSearchService: HybridSearchService,
    private val appPathsConfig: AppPathsConfig,
    private val messageSocketHandler: MessageSocketHandler,
    private val jpaContext: JpaContext
) {
    private val googleAiClient = WebClient.create(googleApiBaseUrl)
    private lateinit var openAiClient: WebClient

    private val ollamaClient = WebClient.create("http://localhost:11434")


    private val queue = LinkedBlockingQueue<MessageEntity>()
    private val scope = CoroutineScope(Dispatchers.Default)


    private val SYSTEM_PROMPT = """
You are a technical assistant specialized in 3GPP standards, including specifications such as LTE, NR, and 5G Core. You must provide highly detailed, precise, and technically accurate answers to questions based only on the provided context.

If the context contains references to images in the following format:
'photo_X.extension : Figure identificator: photo_name'
(e.g., 'photo_24.emf : Figure 5.3.3.1-1: RRC connection establishment, successful'),
treat these as embedded figure references.

Whenever a figure reference appears in the context and the photo_name is directly relevant to the question (e.g., the procedure described in the figure matches the one being asked about), include the entire figure reference, consisting of:

the filename (e.g., photo_24.emf), and

the figure identifier and title (e.g., Figure 5.3.3.1-1: RRC connection establishment, successful)

Insert this complete figure reference in a logically appropriate place outside of the sentence, ideally at the end of the sentence that most directly relates to the figure. Example:
"This procedure is illustrated in photo_24.emf : Figure 5.3.3.1-1: RRC connection establishment, successful."

If the user’s question refers to a term or phrase that appears in a figure title (photo_name), assume the figure is relevant. Always include the full figure reference, including the filename and figure identifier, at a relevant place in the answer, even if the user does not explicitly mention a figure.

Do not invent or modify figure references, and do not include figures that are unrelated to the answer.

If the context includes an HTML table that contains information relevant to the answer, always append the entire HTML table at the end of the response, exactly as it appears in the context — without any modification, reconstruction, or formatting changes. The HTML code of the table must be preserved exactly.

You must not answer questions that cannot be answered using the context. Do not hallucinate facts, behaviors, or diagram names that are not explicitly given.
        """.trimIndent()

    private val PROMPT_TEMPLATE = """
Answer the following question using only the provided context. Include exact figure references if the figure supports your answer.
Context:
{context}

    ---

    Answer the question: {question}
    """.trimIndent()


    @PostConstruct
    fun initProcessor() {
        openAiClient = createOpenAiClient()

        scope.launch {
            try {
                testOpenAiConnection()
                println("OpenAI is reachable at startup")
            } catch (e: Exception) {
                println("OpenAI unreachable at startup: ${e.message}")
            }
        }
        scope.launch {
            while (true) {
                val message = queue.take()
                println("Processing message: ${message.id} using model ${message.modelName}")
                println(message.question)
                processMessage(message)
            }
        }
    }


    fun addQuestionToQueue(message: MessageEntity) {
        queue.put(message)
    }

    private suspend fun ensureOllamaRunning() {
        try {
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder("ollama", "run", "llama3")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
            }
            println("Ollama process started.")
            delay(3000)
        } catch (e: Exception) {
            println("Failed to start Ollama: ${e.message}")
        }
    }

    private suspend fun processMessage(message: MessageEntity) {
        when (message.modelName) {
            "LLaMA_3_8B_Q4_0" -> queryLlama3(message)
            "CHAT_GPT4_1" -> queryChatGpt(message)
            "GEMINI_2_5_PRO" -> queryGemini(message)
            else -> simulateDefault(message)
        }
    }

    private suspend fun queryLlama3(message: MessageEntity) {
        try {
            hybridSearchService.search(
                query = message.question,
                basePath = appPathsConfig.getHybridBaseDirectory(message.baseId),
                onFinish = { contextForQuery, sourceIndices ->

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val prompt = PROMPT_TEMPLATE
                                .replace("{context}", contextForQuery)
                                .replace("{question}", message.question)

                            val request = mapOf(
                                "model" to "llama3",
                                "system" to SYSTEM_PROMPT,
                                "prompt" to prompt,
                                "stream" to false
                            )
                            println("Piastow")
                            println(prompt)

                            val response = ollamaClient.post()
                                .uri("/api/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(OllamaResponse::class.java)
                                .awaitSingle()

                            val updatedMessage = saveAnswer(message, response.response)
                            println("response.response")
                            messageSocketHandler.sendMessageToUser(message.userId, updatedMessage)

                        } catch (e: Exception) {
                            println("Ollama failed: ${e.message}")
                            saveAnswer(message, "Error generating answer: ${e.message}")
                        }
                    }
                }

            )
        } catch (e: Exception) {
            println("Hybrid search failed: ${e.message}")
            saveAnswer(message, "Error in hybrid search: ${e.message}")
        }
    }

    private fun createOpenAiClient(): WebClient {
        val timeoutMillis = 30_000L

        val httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(timeoutMillis))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis.toInt())
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS))
            }

        val connector = ReactorClientHttpConnector(httpClient)

        return WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .clientConnector(connector)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $openAiApiKey")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    private suspend fun testOpenAiConnection() {
        val response = openAiClient.get()
            .uri("/models")
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
        println("OpenAI models response: ${response.take(1000)}...")
    }


    private suspend fun queryChatGpt(message: MessageEntity) {
        println("Processing message ${message.id} via 'queryChatGpt' (OpenAI GPT)")

        try {
            hybridSearchService.search(
                query = message.question,
                basePath = appPathsConfig.getHybridBaseDirectory(message.baseId),
                onFinish = { contextForQuery, sourceIndices ->
                    val contextChunks = getContextChunks(appPathsConfig.getEmbeddedJsonPath(message.baseId), sourceIndices)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            println("Index sourceIndices: $sourceIndices")

                            val prompt = PROMPT_TEMPLATE
                                .replace("{context}", contextForQuery)
                                .replace("{question}", message.question)

                            val request = mapOf(
                                "model" to "gpt-4.1-2025-04-14",
                                "messages" to listOf(
                                    mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                                    mapOf("role" to "user", "content" to prompt)
                                )
                            )

                            safeExecuteWithRetry {
                                val response = openAiClient.post()
                                    .uri("/chat/completions")
                                    .bodyValue(request)
                                    .retrieve()
                                    .bodyToMono(OpenAiResponse::class.java)
                                    .awaitSingle()

                                val answerText = response.choices.firstOrNull()?.message?.content
                                    ?: "No response from OpenAI."

                                val updatedMessage = saveAnswer(message, answerText)

                                val highlighted = highlightUsedContextFragments(
                                    question = message.question,
                                    contextChunks = contextChunks,
                                    modelAnswer = message.answer,
                                )
                                println("highlighted: $highlighted")

                                message.usedContextChunks.addAll(contextChunks.map { (text, index) -> UsedChunk(text, index) })
                                message.highlighetedFragments.addAll(highlighted)

                                val updatedMessageWithHighlightedContext = saveAnswer(message, answerText)
                                messageSocketHandler.sendMessageToUser(message.userId, updatedMessageWithHighlightedContext)
                            }

                        } catch (e: Exception) {
                            val errorMsg = "OpenAI API call failed for message ${message.id}: ${e.message}"
                            println(errorMsg)
                            saveAnswer(message, "Error with OpenAI GPT (ChatGpt): ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            val errorMsg = "Hybrid search failed (chatGPT) for message ${message.id}: ${e.message}"
            println(errorMsg)
            saveAnswer(message, "Error in search (chatGPT): ${e.message}")
        }
    }


    private suspend fun queryGemini(message: MessageEntity) {
        println("Processing message ${message.id} via 'queryGemini' (Google AI). Model in message: ${message.modelName}")
        try {
            hybridSearchService.search(
                query = message.question,
                basePath = appPathsConfig.getHybridBaseDirectory(message.baseId),
                onFinish = { contextForQuery, sourceIndices ->
                    println("Index sourceIndices: $sourceIndices")
                    val contextChunks = getContextChunks(appPathsConfig.getEmbeddedJsonPath(message.baseId), sourceIndices)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val fullPrompt = """
                            $SYSTEM_PROMPT

                            ${
                                PROMPT_TEMPLATE
                                    .replace("{context}", contextForQuery)
                                    .replace("{question}", message.question)
                            }
                        """.trimIndent()

                            val effectiveGoogleModel = googleApiModel

                            val requestBody = GeminiRequest(
                                contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
                            )

                            println("Sending request to Google AI model '$effectiveGoogleModel' (for Gemini, message ${message.id})")

                            val response = callGeminiWithRetryOnce(requestBody, effectiveGoogleModel)

                            val answerText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: run {
                                    val finishReason = response.candidates?.firstOrNull()?.finishReason
                                    val safetyRatings =
                                        response.promptFeedback?.safetyRatings?.joinToString { "${it.category}: ${it.probability}" }
                                    var errorMessage = "Error: No content from Gemini (Gemini)."
                                    if (finishReason != null) errorMessage += " Reason: $finishReason."
                                    if (safetyRatings != null && safetyRatings.isNotEmpty()) errorMessage += " Safety: $safetyRatings."
                                    errorMessage
                                }

                            val updatedMessage = saveAnswer(message, answerText)

                            val highlighted = highlightUsedContextFragments(
                                question = message.question,
                                contextChunks = contextChunks,
                                modelAnswer = message.answer,
                            )
                            println("highlighted: $highlighted")

                            message.usedContextChunks.addAll(contextChunks.map { (text, index) -> UsedChunk(text, index) })

                            message.highlighetedFragments.addAll(highlighted)

                            val updatedMessageWithHighlightedContext = saveAnswer(message, answerText)
                            messageSocketHandler.sendMessageToUser(message.userId, updatedMessageWithHighlightedContext)
                        } catch (e: Exception) {
                            val errorMsg = "Google AI API call failed (Gemini) for message ${message.id}: ${e.message}"
                            println(errorMsg)
                            e.printStackTrace()
                            saveAnswer(message, "Error with Google AI (Gemini): ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            val errorMsg = "Hybrid search failed (Gemini) for message ${message.id}: ${e.message}"
            println(errorMsg)
            saveAnswer(message, "Error in search (Gemini): ${e.message}")
        }
    }


    private suspend fun callGeminiWithRetryOnce(
        requestBody: GeminiRequest,
        modelName: String,
        retryLeft: Boolean = true
    ): GeminiResponse {
        return try {
            googleAiClient.post()
                .uri("/v1beta/models/$modelName:generateContent?key=$googleApiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<GeminiResponse>() {})
                .awaitSingle()
        } catch (e: Exception) {
            if (e is SocketException && retryLeft) {
                println("Connection reset detected, retrying once for model: $modelName")
                callGeminiWithRetryOnce(requestBody, modelName, retryLeft = false)
            } else {
                throw e
            }
        }
    }

    private suspend fun safeExecuteWithRetry(
        maxRetries: Int = 3,
        delayMillis: Long = 50,
        block: suspend () -> Unit
    ) {
        repeat(maxRetries) { attempt ->
            try {
                block()
                return
            } catch (e: Exception) {
                println("Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == maxRetries - 1) throw e
                delay(delayMillis)
            }
        }
    }


    fun String.normalizeToAscii(): String {
        return this.replace('\u00A0', ' ')
            .replace(Regex("[–—−]"), "-")
            .replace("\r", "")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun highlightUsedContextFragments(
        question: String,
        contextChunks: List<Pair<String, Int>>,
        modelAnswer: String
    ): List<HighlightedFragment> {
        val promptPrefix = """
You are an AI assistant. The user has asked a question, and another LLM has answered it based on the provided context fragment.

Your task is to identify which parts of the context fragment most likely contributed to forming the answer.

You must return the **exact same context fragment**, with the exception that you may insert the special tags:
- BBB_RED_OPEN (to mark the beginning of a relevant phrase)
- BBB_RED_CLOSE (to mark the end of a relevant phrase)

IMPORTANT:
- You are encouraged to mark all phrases, sentences, or smaller expressions that are semantically or conceptually related to the answer.
- You may mark multiple fragments in one chunk.
- **You are not allowed to change, remove, or reword any part of the original context.**
- The only permitted change is inserting the BBB_RED_OPEN and BBB_RED_CLOSE tags into the original text.
- If you find nothing relevant, return the fragment unchanged (no tags added).

Question: $question
Answer: $modelAnswer

""".trimIndent()

        return coroutineScope {
            contextChunks.map { (chunk, index) ->
                async {
                    var attempt = 0
                    var result = ""

                    while (attempt < 5) {
                        try {
                            val request = GeminiRequest(
                                contents = listOf(
                                    Content(parts = listOf(Part(text = "$promptPrefix\nContext Fragment:\n$chunk")))
                                )
                            )

                            result = callGeminiWithRetryOnce(request, googleApiModel).candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text ?: ""

                            println("Result of gemini Highlighting\n\n\n $result \n\n\n")

                            val openCount = Regex("BBB_RED_OPEN").findAll(result).count()
                            val closeCount = Regex("BBB_RED_CLOSE").findAll(result).count()
                            if (openCount != closeCount) {
                                printRed("Highlight Mismatch in BBB_RED_OPEN and BBB_RED_CLOSE: attempt=$attempt")
                                attempt++
                                delay(2000L * attempt)
                                continue
                            }

                            val quotedFragments = Regex("BBB_RED_OPEN(.*?)BBB_RED_CLOSE")
                                .findAll(result)
                                .map { it.groupValues[1] }
                                .toList()

                            val normalizedChunk = chunk.normalizeToAscii()

                            val matches = quotedFragments.mapNotNull { quote ->
                                val normalizedQuote = quote.normalizeToAscii()

                                val positions = Regex(Regex.escape(normalizedQuote))
                                    .findAll(normalizedChunk)
                                    .mapIndexed { i, match -> match.range.first to i + 1 }
                                    .toList()

                                if (positions.isEmpty()) {
                                    printRed("Highlight Could not find quote: '$quote' in chunk at index $index")
                                    return@mapNotNull null
                                }

                                val (position, occurrence) = positions.first()
                                HighlightedFragment(quote, occurrence, index)
                            }

                            if (matches.size != quotedFragments.size) {
                                printRed("Highlight Some quotes were not uniquely matched in chunk at index $index. Retrying...")
                                attempt++
                                delay(2000L * attempt)
                                continue
                            }

                            return@async matches
                        } catch (e: Exception) {
                            printRed("Highlight Exception on attempt $attempt: ${e.message}")
                            attempt++
                            delay(200L * attempt)
                        }
                    }
                    emptyList()
                }
            }.awaitAll().flatten()
        }
    }



    private suspend fun simulateDefault(message: MessageEntity) {
        saveAnswer(message, "This is a the best answer for the best question")
    }

    private fun saveAnswer(message: MessageEntity, answer: String): MessageEntity {
        val updated = message.copy(
            answer = answer,
            answered = true,
            answeredAt = Instant.now()
        )
        messageRepository.save(updated)
        println("Saved answer for message ${updated.id}")
        return updated
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OllamaResponse(
        @JsonProperty("response") val response: String,
        @JsonProperty("done") val done: Boolean
    )


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EmbeddedChunk(
        val index: Int,
        val content: String
    )

    data class EmbeddedJson(
        val embeddedChunks: List<EmbeddedChunk>
    )

    fun getContextChunks(jsonPath: String, sourceIndices: List<Int>): List<Pair<String, Int>> {
        val mapper = jacksonObjectMapper()
        val file = File(jsonPath)
        val parsed: EmbeddedJson = mapper.readValue(file)

        return parsed.embeddedChunks
            .filter { it.index in sourceIndices }
            .sortedBy { it.index }
            .map { it.content to it.index }
    }


}


fun printRed(text: String) {
    println("\u001B[31m$text\u001B[0m")
}

