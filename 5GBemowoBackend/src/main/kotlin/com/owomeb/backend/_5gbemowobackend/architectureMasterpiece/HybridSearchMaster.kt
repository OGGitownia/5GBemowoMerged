package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.owomeb.backend._5gbemowobackend.core.ServerBoot
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/hybridSearchMaster")
@Component
class HybridSearchMaster(serverBoot: ServerBoot) :
    PythonServerModelV2<HybridSearchMaster.QueryElement>(
        scriptPath = "../resourcesShared/pythonScripts/pythonServers/hybridSearchMaster.py",
        serverBoot = serverBoot,
        serverName = "hybridSearchMaster"
    ) {

    private val callbacks = mutableMapOf<QueryElement, (String, List<Int>) -> Unit>()

    fun search(query: String, basePath: String, onResult: (String, List<Int>) -> Unit) {
        val element = QueryElement(query, basePath)
        callbacks[element] = onResult
        addToQueue(element)
    }

    override fun publishResult(result: String, item: QueryElement) {
        println("Search completed for: ${item.query}")
        try {
            val parsed = parseResult(result)

            val contextForQuery = parsed.results.joinToString("\n") { it.sentence }
            val sourceIndices = parsed.results.map { it.sourceIndex }

            callbacks.remove(item)?.invoke(contextForQuery, sourceIndices)
        } catch (e: Exception) {
            println("Failed to parse result: ${e.message}")
        }
    }

    override fun onTaskFailure(error: Throwable, item: QueryElement) {
        super.onTaskFailure(error, item)
        println("Search failed for: ${item.query} - ${error.message}")
        callbacks.remove(item)
    }

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        println("Obtained /hybridSearchMaster/server-ready z: $body")
        return super.markServerAsReady(body)
    }

    data class QueryElement(
        val query: String,
        val basePath: String
    )

    data class SearchResult(
        val results: List<Match>
    )

    data class Match(
        val sentence: String,
        val sourceIndex: Int
    )

    private fun parseResult(json: String): SearchResult {
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        return mapper.readValue(json, SearchResult::class.java)
    }
}
