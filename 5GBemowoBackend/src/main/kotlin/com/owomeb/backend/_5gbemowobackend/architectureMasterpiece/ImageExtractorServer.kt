package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.owomeb.backend._5gbemowobackend.core.ServerBoot
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/imageExtractor")
@Component
class ImageExtractorServer(serverBoot: ServerBoot) :
    PythonServerModelV2<ImageExtractorServer.QueueElement>(
        scriptPath = "../resourcesShared/pythonScripts/pythonServers/imageExtractor.py",
        serverBoot = serverBoot,
        serverName = "imageExtractor"
    ) {

    private val callbacks = ConcurrentHashMap<QueueElement, () -> Unit>()

    fun extractImages(
        inputDocxPath: String,
        outputDocxPath: String,
        outputDirPath: String,
        onFinish: () -> Unit
    ) {
        val element = QueueElement(
            input = inputDocxPath,
            outputDocx = outputDocxPath,
            outputDir = outputDirPath
        )
        callbacks[element] = onFinish
        addToQueue(element)
    }

    override fun publishResult(result: String, item: QueueElement) {
        println("Image extraction completed for: ${item.input}")
        callbacks.remove(item)?.invoke()
        println("Result: $result")
    }

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        println("Received /imageExtractor/server-ready: $body")
        return super.markServerAsReady(body)
    }

    override fun onTaskFailure(error: Throwable, item: QueueElement) {
        super.onTaskFailure(error, item)
        println("Failed to extract images from ${item.input}: ${error.message}")
        callbacks.remove(item)
    }

    data class QueueElement(
        val input: String,
        val outputDocx: String,
        val outputDir: String
    )
}
