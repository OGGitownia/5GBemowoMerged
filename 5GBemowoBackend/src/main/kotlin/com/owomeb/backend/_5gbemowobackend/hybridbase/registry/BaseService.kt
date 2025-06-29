package com.owomeb.backend._5gbemowobackend.hybridbase.registry

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Service
class BaseService(
    private val baseRepository: BaseRepository,
) {
    val statusObservers = ConcurrentHashMap<Long, (BaseEntity) -> Unit>()

    fun listAllBases(): List<BaseEntity> = baseRepository.findAll()

    fun getBaseById(id: Long): BaseEntity? = baseRepository.findById(id).orElse(null)

    fun findBySourceUrl(url: String): BaseEntity? = baseRepository.findBySourceUrl(url)

    @Transactional
    fun createBase(
        sourceUrl: String,
        method: String,
        maxContextWindow: Int,
        multiSearchAllowed: Boolean,
        release: String,
        series: String,
        norm: String
    ): Long {
        val base = BaseEntity(
            sourceUrl = sourceUrl,
            createdWthMethod = BaseCreatingMethods.valueOf(method),
            maxContextWindow = maxContextWindow,
            multiSearchAllowed = multiSearchAllowed,
            release = release,
            series = series,
            norm = norm
        )
        return baseRepository.save(base).id
    }



    @Transactional
    fun updateStatus(baseId: Long, status: BaseStatus, message: String? = null) {
        val base = baseRepository.findById(baseId).orElseThrow()
        base.status = status
        base.statusMessage = message
        baseRepository.save(base)

        println("Updating status of base $baseId to $status with message: $message")
        println("Full entity after update: $base")

        statusObservers[baseId]?.invoke(base)
    }

    fun registerStatusObserver(baseId: Long, callback: (BaseEntity) -> Unit) {
        statusObservers[baseId] = callback
    }

    fun unregisterStatusObserver(baseId: Long) {
        statusObservers.remove(baseId)
    }

    @Transactional
    fun delete(base: BaseEntity) {
        println("Próba usuwania bazy ${base.sourceUrl}")
        baseRepository.delete(base)
    }
}
