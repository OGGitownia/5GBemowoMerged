package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/hybridDbCreator")
class HybridDbCreator : PythonServerModel<HybridDbCreator.QueueItem>(
    serverName = "hybridDbCreator",
    scriptPath = "../resourcesShared/pythonScripts/pythonServers/dbCreator.py",
    autoClose = true
) {

    private val callbacks = ConcurrentHashMap<QueueItem, () -> Unit>()

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun createDb(inputFilePath: String, outputFilePath: String, onFinish: () -> Unit) {
        val queueItem = QueueItem(inputPath = inputFilePath, outputPath = outputFilePath)
        callbacks[queueItem] = onFinish
        this.addToQueue(queueItem)
    }

    override fun publishResult(result: String, item: QueueItem) {
        println("Finished creating base for: ${item.inputPath}")
        callbacks.remove(item)?.invoke()
        println("Processing result from server $serverName: $result")
    }


    data class QueueItem(val inputPath: String, val outputPath: String)
}
