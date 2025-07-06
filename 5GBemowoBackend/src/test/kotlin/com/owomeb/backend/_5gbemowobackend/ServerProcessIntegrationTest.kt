import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import java.io.*
import java.time.Duration
import java.time.Instant

class ServerProcessIntegrationTest {

    private val serverName = "newEmbedding"
    private val port = 5111
    private val restTemplate = RestTemplate()
    private val pythonScript = "C:\\gitRepositories\\5GBemowoMerged\\resourcesShared\\pythonScripts\\pythonServers\\newEmbedding.py"
    private val inputPath = "C:\\gitRepositories\\5GBemowoMerged\\resourcesShared\\data\\3\\chunky.json"

    @Test
    fun `test embedding server end-to-end`() {
        val timestamp = System.currentTimeMillis().toString()
        val outputDir = File("C:\\gitRepositories\\5GBemowoMerged\\resourcesShared\\tests\\$timestamp")
        outputDir.mkdirs()
        val outputPath = File(outputDir, "embeddedchunky.json").absolutePath

        require(File(pythonScript).exists()) { "Python script not found at $pythonScript" }
        require(File(inputPath).exists()) { "Input JSON not found at $inputPath" }

        val process = ProcessBuilder("python3", pythonScript, serverName, port.toString())
            .redirectErrorStream(true)
            .start()

        val logThread = Thread {
            val reader = process.inputStream.bufferedReader()
            reader.useLines { lines ->
                lines.forEach {
                    println("[PYTHON] $it")
                }
            }
        }
        logThread.start()


        println("⏳ Czekam na uruchomienie serwera...")
        var serverReady = false
        repeat(60) {
            try {
                val response = restTemplate.getForEntity("http://localhost:$port/status", String::class.java)
                if (response.statusCode == HttpStatus.OK) {
                    serverReady = true
                    println("✅ Serwer gotowy.")
                    return@repeat
                }
            } catch (_: Exception) {}
            Thread.sleep(1000)
        }

        assertTrue(serverReady, "❌ Serwer Python nie uruchomił się w 60 sekund.")

        // Przygotuj zapytanie
        val requestBody = mapOf(
            "inputFile" to inputPath,
            "outputFile" to outputPath
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val httpEntity = HttpEntity(requestBody, headers)

        println("📤 Wysyłam plik do embeddingu...")
        val start = Instant.now()
        val response = restTemplate.postForEntity(
            "http://localhost:$port/$serverName/process",
            httpEntity,
            String::class.java
        )
        val duration = Duration.between(start, Instant.now()).toMillis()

        println("⏱️ Embedding zakończony w ${duration / 1000.0} sekund")

        assertEquals(HttpStatus.OK, response.statusCode, "❌ Niepoprawny status HTTP")

        val outputFile = File(outputPath)
        assertTrue(outputFile.exists(), "❌ Plik wynikowy nie istnieje: $outputPath")

        val jsonContent = outputFile.readText(Charsets.UTF_8)
        assertTrue(jsonContent.contains("embeddedChunks"), "❌ Brakuje embeddedChunks w pliku wynikowym")

        println("✅ Test zakończony sukcesem – przetworzono plik.")

        // Zamknięcie serwera
        try {
            restTemplate.postForEntity("http://localhost:$port/$serverName/shutdown", null, String::class.java)
        } catch (_: Exception) {
            println("⚠️ Serwer mógł już się zakończyć.")
        }

        logThread.join(3000)

        process.destroy()
    }
}
