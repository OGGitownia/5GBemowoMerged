package com.owomeb.backend._5gbemowobackend.hybridbase.builderLogic

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.EmbeddingMaster
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.HybridDbBuilder
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.ImageExtractorServer
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.*
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseCreatingMethods
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue

@Service
class CommissionManager(
    private val appPathsConfig: AppPathsConfig,
    private val normManager: NormManager,
    private val markdownManager: FinalMarkdown,
    private val embeddingManager: EmbeddingMaster,
    private val hybridDbCreator: HybridDbBuilder,
    private val imageExtractorServer: ImageExtractorServer
) {
    private lateinit var baseService: BaseService

    private val queue = LinkedBlockingQueue<Commission>()

    @PostConstruct
    fun startWorker() {
        Thread {
            while (true) {
                val commission = queue.take()
                try {
                    commission.proceed(baseService)
                } catch (e: Exception) {
                    println("[ERROR] Failed to execute commission: ${e.message}")
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun submitCommission(baseId: Long, sourceUrl: String, method: BaseCreatingMethods) {
        val commission = when (method) {
            BaseCreatingMethods.FITTED_OVERLAP -> CommissionForDBWithFittedOverlap(
                baseId = baseId,
                sourceUrl = sourceUrl,
                appPathsConfig = appPathsConfig,
                normManager = normManager,
                markdownManager = markdownManager,
                embeddingManager = embeddingManager,
                hybridDbCreator = hybridDbCreator,
                imageExtractorServer = imageExtractorServer
            )
            BaseCreatingMethods.UNCOMPROMISINGNESS -> CommissionForDBUncompromising(
                baseId = baseId,
                sourceUrl = sourceUrl,
                appPathsConfig = appPathsConfig,
                normManager = normManager,
                markdownManager = markdownManager,
                imageExtractorServer = imageExtractorServer,
                embeddingManager = embeddingManager,
                hybridDbCreator = hybridDbCreator,
            )
        }
        queue.put(commission)
    }
    @Autowired
    fun setBaseService(baseService: BaseService) {
        this.baseService = baseService
    }

}
