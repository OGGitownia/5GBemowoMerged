package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.owomeb.backend._5gbemowobackend.core.ProcessState
import com.owomeb.backend._5gbemowobackend.core.ServerBoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.springframework.web.bind.annotation.RequestBody
import java.net.HttpURLConnection
import java.net.ServerSocket
import kotlin.concurrent.thread


abstract class PythonServerModelV2<T>(
    private val scriptPath: String,
    private val serverBoot: ServerBoot,
    private val serverName: String
) {
    private var process: Process? = null

    private val _queue = MutableStateFlow<List<QueuedTask<T>>>(emptyList())
    protected val queue: StateFlow<List<QueuedTask<T>>> = _queue.asStateFlow()

    protected var actualPort: Int = findAvailablePort()

    init {
        startPythonMicroServer()
    }

    private fun startPythonMicroServer() {
        thread {
            try {
                val processBuilder = ProcessBuilder("python3", scriptPath, serverName, actualPort.toString())
                processBuilder.redirectErrorStream(true)
                process = processBuilder.start()

                serverBoot.registerProcess(serverName, process!!, actualPort)


                process!!.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { println("[$serverName]: $it") }
                }

                val code = process!!.waitFor()
                println("Server $serverName exited with code: $code")
                updateStatus(ProcessState.FAILED)

            } catch (e: Exception) {
                println("Startup error: ${e.message}")
                updateStatus(ProcessState.FAILED)
            }
        }
    }



    open fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        val port = body["port"] as? Int ?: return mapOf("status" to "ERROR", "message" to "Missing port in request")
        actualPort = port
        updateStatus(ProcessState.READY)
        processQueue()
        println("Server $serverName ready from markServerAsReady")
        return mapOf("status" to "OK", "message" to "$serverName is ready on port $actualPort")
    }

    private fun updateStatus(newState: ProcessState) {
        serverBoot.setProcessStatus(serverName, newState)
    }


    fun addToQueue(item: T) {
        _queue.value += QueuedTask(item, QueueStatus.QUEUED)
        processQueue()
    }

    private var isProcessing = false

    open fun processQueue() {
        if (isProcessing) return
        if (serverBoot.pythonServers.find { it.name == serverName }?.status != ProcessState.READY) return
        if (queue.value.none { it.status == QueueStatus.QUEUED }) return

        isProcessing = true

        val next = queue.value.firstOrNull { it.status == QueueStatus.QUEUED } ?: run {
            isProcessing = false
            return
        }

        updateTaskStatus(next, QueueStatus.SENT_TO_PYTHON)

        sendRequestToPython(next.item) { result ->
            result.onSuccess { response ->
                publishResult(response, next.item)
                _queue.value = _queue.value.filterNot { it == next }
                isProcessing = false
                processQueue()
            }.onFailure { exception ->
                onTaskFailure(exception, next.item)
                isProcessing = false
            }
        }
    }

    protected open fun sendRequestToPython(item: T, callback: (Result<String>) -> Unit) {
        thread {
            try {
                val url = java.net.URL("http://localhost:$actualPort/$serverName/process")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 60000
                connection.readTimeout = 120 * 60000

                val body = jacksonObjectMapper().writeValueAsString(item)
                connection.outputStream.write(body.toByteArray(Charsets.UTF_8))
                connection.outputStream.flush()

                val responseCode = connection.responseCode
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode == 200) {
                    callback(Result.success(responseText))
                } else {
                    val errorMessage = try {
                        jacksonObjectMapper().readTree(responseText).get("error")?.asText() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Unparsable error: $responseText"
                    }

                    callback(Result.failure(PythonServerException(errorMessage)))
                }

            } catch (e: Exception) {
                callback(Result.failure(PythonServerException("Communication failure with $serverName: ${e.message}")))
            }
        }
    }

    open fun onTaskFailure(error: Throwable, item: T) {
        println("Failed to process $item: ${error.message}")
        if (error is PythonServerException) {
            throw error
        }
    }

    protected open fun publishResult(result: String, item: T) {
        println("Result for $item: $result")
    }

    private fun updateTaskStatus(task: QueuedTask<T>, status: QueueStatus) {
        _queue.value = _queue.value.map {
            if (it == task) it.copy(status = status) else it
        }
    }

    data class QueuedTask<T>(
        val item: T,
        val status: QueueStatus
    )

    enum class QueueStatus {
        QUEUED,
        SENT_TO_PYTHON
    }

    private fun findAvailablePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }
}

class PythonServerException(message: String) : RuntimeException(message)
