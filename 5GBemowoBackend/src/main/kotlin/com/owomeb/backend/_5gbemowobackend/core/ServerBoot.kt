package com.owomeb.backend._5gbemowobackend.core

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.net.HttpURLConnection

@Component
class ServerBoot {
    private var _serverState: ProcessState = ProcessState.LOADING
    val serverState: ProcessState get() = _serverState

    val pythonServers: MutableList<ServerProcess> = mutableListOf()

    fun updateState() {
        _serverState = when {
            pythonServers.any { it.status == ProcessState.FAILED } -> ProcessState.FAILED
            pythonServers.all { it.status == ProcessState.READY } -> {
                println("Server READY FROM SERVERBOOT big success")
                ProcessState.READY
            }
            else -> ProcessState.LOADING
        }
    }

    fun setProcessStatus(name: String, newStatus: ProcessState) {
        val process = pythonServers.find { it.name == name }
        if (process != null) {
            process.status = newStatus
            updateState()
        }
    }

    fun registerProcess(name: String, process: Process, port: Int) {
        pythonServers += ServerProcess(name = name, status = ProcessState.LOADING, process = process, port = port)
    }


    @PreDestroy
    fun shutdownPythonServers() {
        println("Shutting down Python servers gracefully...")

        for (server in pythonServers) {
            val port = server.port

            try {
                val url = java.net.URL("http://localhost:$port/${server.name}/shutdown")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    connectTimeout = 2000
                    readTimeout = 2000

                    val code = responseCode
                    println("Graceful shutdown for ${server.name}, HTTP $code")

                    if (code != 200) {
                        println("Non-200 response, fallback to destroy() for ${server.name}")
                        server.process?.destroy()
                    }
                }
                Thread.sleep(5000)
            } catch (e: Exception) {
                println("Graceful shutdown failed for ${server.name}: ${e.message}")
                server.process?.destroy()
            }
        }
    }



    data class ServerProcess(
        val name: String,
        var status: ProcessState,
        val process: Process? = null,
        val port: Int
    )

}
