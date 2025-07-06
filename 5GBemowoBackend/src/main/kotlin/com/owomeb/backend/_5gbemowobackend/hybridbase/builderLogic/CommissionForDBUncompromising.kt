package com.owomeb.backend._5gbemowobackend.hybridbase.builderLogic

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.EmbeddingMaster
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.HybridDbBuilder
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.ImageExtractorServer
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.*
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseStatus
import java.util.concurrent.TimeUnit

class CommissionForDBUncompromising(
    baseId: Long,
    private val appPathsConfig: AppPathsConfig,
    private val normManager: NormManager,
    private val imageExtractorServer: ImageExtractorServer,
    private val markdownManager: FinalMarkdown,
    private val embeddingManager: EmbeddingMaster,
    private val hybridDbCreator: HybridDbBuilder,
    private val sourceUrl: String
)  : Commission(baseId) {
    private var status: CommissionStatus = CommissionStatus.INITIAL
    private var currentStage = 0

    override fun proceed(baseService: BaseService) {
        try {
            when (currentStage) {
                0 -> {
                    download(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                1 -> {
                    extract(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                2 -> {
                    markdown(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                3 -> {
                    chunk(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                4 -> {
                embed(baseService) {
                    currentStage++
                    proceed(baseService)
                }
            }
                5 -> {
                    hybridBase(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                6 -> {
                    finalizeCommission(baseService)
                }
            }
        } catch (e: Exception) {
            updateStatus(baseService, BaseStatus.FAILED, "Error: ${e.message}")
            e.printStackTrace()
        }
    }
    private fun download(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Downloading the document")
        normManager.downloadAndExtractNorm(
            normUrl = sourceUrl,
            zipPath = appPathsConfig.getZipPath(baseId.toString()),
            docPath = appPathsConfig.getDocPath(baseId.toString())
        )
        status = CommissionStatus.DOWNLOADED
        onFinish()
    }

    private fun extract(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Extracting photos")

        imageExtractorServer.extractImages(
            inputDocxPath = appPathsConfig.getDocPath(baseId.toString()),
            outputDocxPath = appPathsConfig.getExtractedDocx(baseId.toString()),
            outputDirPath = appPathsConfig.getNormDirectory(baseId.toString())
        ) {
            status = CommissionStatus.EXTRACTED
            onFinish()
        }
    }



    private fun markdown(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Markdowning (wait)")
        markdownManager.doMarkdowning(
            inputPath = appPathsConfig.getExtractedDocx(baseId.toString()),
            outputPath = appPathsConfig.getMarkdownPath(baseId.toString())
        )
        status = CommissionStatus.MARKDOWNED
        onFinish()
    }
    private fun chunk(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Chunking Chunking (wait more)")
        val chunker = FinalChunker(
            pureMarkdownPath = appPathsConfig.getMarkdownPath(baseId.toString()),
            outputPath = appPathsConfig.getChunkedJsonPath(baseId.toString()),
            minChunkLen = 2000,
            maxChunkLen = 4000
        )
        chunker.process()
        status = CommissionStatus.CHUNKED
        onFinish()
    }
    private fun embed(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Embedding Embedding (wait very long)")
        embeddingManager.generateEmbeddings(
            inputFilePath = appPathsConfig.getChunkedJsonPath(baseId.toString()),
            outputFilePath = appPathsConfig.getEmbeddedJsonPath(baseId.toString()),
            onFinish = onFinish
        )
    }
    private fun hybridBase(baseService: BaseService, onFinish: () -> Unit) {
        status = CommissionStatus.EMBEDDED
        updateStatus(baseService, BaseStatus.PROCESSING, "Creating hybrid base")

        hybridDbCreator.buildHybridDatabase(
            inputPath = appPathsConfig.getEmbeddedJsonPath(baseId.toString()),
            outputPath = appPathsConfig.getHybridBaseDirectory(baseId.toString()),
            onFinish = onFinish
        )
    }

    private fun finalizeCommission(baseService: BaseService) {
        status = CommissionStatus.HYBRID_BASED
        updateStatus(baseService, BaseStatus.READY, "Base is READY -!!!-")
        status = CommissionStatus.DONE
    }
}