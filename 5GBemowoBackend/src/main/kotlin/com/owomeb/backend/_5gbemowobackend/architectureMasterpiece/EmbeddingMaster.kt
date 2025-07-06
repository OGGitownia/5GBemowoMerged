package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.owomeb.backend._5gbemowobackend.core.ServerBoot
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/embeddingMaster")
@Component
class EmbeddingMaster(serverBoot: ServerBoot) :
    PythonServerModelV2<EmbeddingMaster.QueueElement>(
        scriptPath = "../resourcesShared/pythonScripts/pythonServers/embeddingMaster.py",
        serverBoot = serverBoot,
        serverName = "embeddingMaster"
    ) {

    private val callbacks = ConcurrentHashMap<QueueElement, () -> Unit>()

    fun generateEmbeddings(inputFilePath: String, outputFilePath: String, onFinish: () -> Unit) {
        val element = QueueElement(inputFilePath, outputFilePath)
        callbacks[element] = onFinish
        addToQueue(element)
    }

    override fun publishResult(result: String, item: QueueElement) {
        println("Embedding completed for: ${item.inputFile}")
        callbacks.remove(item)?.invoke()
        println("Embedding result: $result")
    }

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        println("Obtained /embeddingMaster/server-ready z: $body")
        return super.markServerAsReady(body)
    }


    override fun onTaskFailure(error: Throwable, item: QueueElement) {
        super.onTaskFailure(error, item)
        println("Failed to generate embedding for ${item.inputFile}: ${error.message}")
        callbacks.remove(item)
    }

    data class QueueElement(val inputFile: String, val outputFile: String)
}