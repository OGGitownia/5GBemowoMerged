package com.owomeb.backend._5gbemowobackend.hybridbase.retrieval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

@RestController
@RequestMapping("/hybrid_search_server")
class HybridSearchService: PythonServerModel<HybridSearchService.QueueItem>(
    scriptPath = "../resourcesShared/pythonScripts/pythonServers/serverSearch.py",
    serverName = "hybrid_search_server",
    autoClose = false
){

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun search(query: String, basePath: String, onFinish: (String, MutableList<Int>) -> Unit){
        val newSearchObject = QueueItem(
            query = query,
            basePath = basePath,
            onFinish = onFinish
        )
        this.addToQueue(newSearchObject)
    }


    override fun publishResult(result: String, item: QueueItem) {
        val sourceIndeces = mutableListOf<Int>()
        val cleanResult = try {
            val json = JSONObject(result)
            val resultsArray = json.optJSONArray("results")


            if (resultsArray != null) {
                val resultStrings = mutableListOf<String>()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.optJSONObject(i)
                    if (obj != null) {
                        val sentence = obj.optString("sentence")
                        val sourceIndex = obj.optInt("source_index", -1)
                        println("Results $i: source_index=$sourceIndex")
                        resultStrings.add("[$sourceIndex] $sentence")
                        sourceIndeces.add(sourceIndex)
                    }
                }

                resultStrings.joinToString("\n")
            } else {
                "The lack of results"
            }
        } catch (e: Exception) {
            "Error while parsing JSON: ${e.message}"
        }

        println("Error while search for: $item\n$cleanResult")
        item.onFinish(cleanResult, sourceIndeces)
    }


    override fun sendRequestToPython(item: QueueItem, callback: (String) -> Unit) {
        thread {
            try {
                val url = URL("http://localhost:$actualPort/$serverName/process")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val objectMapper = jacksonObjectMapper()
                val requestBody = objectMapper.writeValueAsString(
                    mapOf(
                        "query" to item.query,
                        "basePath" to item.basePath
                    )
                )

                connection.outputStream.use {
                    it.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (connection.responseCode == 200) {
                    callback(response)
                } else {
                    println("Error: $response")
                }
            } catch (e: Exception) {
                println("Comunication Error: ${e.message}")
            }
        }
    }


    data class QueueItem(
        val query: String,
        val basePath: String,
        val onFinish: (String, MutableList<Int>) -> Unit
    )

}
