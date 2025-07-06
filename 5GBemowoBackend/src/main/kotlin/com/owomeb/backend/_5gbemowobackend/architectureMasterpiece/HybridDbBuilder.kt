package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.owomeb.backend._5gbemowobackend.core.ServerBoot
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/hybridDbBuilder")
@Component
class HybridDbBuilder(serverBoot: ServerBoot) :
    PythonServerModelV2<HybridDbBuilder.QueueElement>(
        scriptPath = "../resourcesShared/pythonScripts/pythonServers/hybridDbBuilder.py",
        serverBoot = serverBoot,
        serverName = "hybridDbBuilder"
    ) {

    private val callbacks = ConcurrentHashMap<QueueElement, () -> Unit>()

    fun buildHybridDatabase(inputPath: String, outputPath: String, onFinish: () -> Unit) {
        val element = QueueElement(inputPath, outputPath)
        callbacks[element] = onFinish
        addToQueue(element)
    }

    override fun publishResult(result: String, item: QueueElement) {
        println("Hybrid database created for: ${item.inputPath}")
        callbacks.remove(item)?.invoke()
        println("Hybrid DB result: $result")
    }

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        println("Obtained /hybridDbBuilder/server-ready z: $body")
        return super.markServerAsReady(body)
    }

    override fun onTaskFailure(error: Throwable, item: QueueElement) {
        super.onTaskFailure(error, item)
        println("Failed to build hybrid DB for ${item.inputPath}: ${error.message}")
        callbacks.remove(item)
    }

    data class QueueElement(val inputPath: String, val outputPath: String)
}
