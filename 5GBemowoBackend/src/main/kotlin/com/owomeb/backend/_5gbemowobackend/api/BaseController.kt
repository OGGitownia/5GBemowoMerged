package com.owomeb.backend._5gbemowobackend.api

import com.owomeb.backend._5gbemowobackend.hybridbase.builderLogic.CommissionManager
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseCreatingMethods
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseEntity
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.CreateBaseRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bases")
class BaseController(
    private val baseService: BaseService,
    private val commissionManager: CommissionManager
) {

    @GetMapping("/methods/get-all", produces = ["application/json"])
    fun getBaseCreatingMethods(): List<String> {
        return BaseCreatingMethods.entries.map { it.name }
    }

    @PostMapping("/create")
    fun createBase(@RequestBody request: CreateBaseRequest): ResponseEntity<Any> {
        return try {
            val baseId = baseService.createBase(
                sourceUrl = request.sourceUrl,
                method = request.selectedMethod,
                multiSearchAllowed = request.multiSearchAllowed,
                release = request.release,
                series = request.series,
                norm = request.norm,
                maxContextWindow = request.maxContextWindow
            )

            commissionManager.submitCommission(
                baseId = baseId,
                sourceUrl = request.sourceUrl,
                method = BaseCreatingMethods.valueOf(request.selectedMethod)
            )
            ResponseEntity.ok(mapOf("message" to "Base successfully created", "baseId" to baseId))
        } catch (ex: Exception) {
            ResponseEntity.badRequest().body(mapOf("message" to "Error during base creation", "error" to ex.message))
        }
    }



    @GetMapping
    fun getAllBases(): List<BaseEntity> {
        val allBases = baseService.listAllBases()
        println("getAllBases")
        println(allBases)

        return allBases
    }
    fun deleteBaseBySourceUrl(url: String) {
        println("Próba usuwaniaDELETEBASEBYSOURCEURL")
        val base = baseService.findBySourceUrl(url)
        if (base != null) {
            baseService.delete(base)
        }
    }
    fun deleteAllBases(): ResponseEntity<String> {
        val bases = baseService.listAllBases()
        bases.forEach { baseService.delete(it) }
        return ResponseEntity.ok("All bases have been deleted")
    }



    @GetMapping("/{id}")
    fun getBase(@PathVariable id: Long): ResponseEntity<BaseEntity> {
        println("getBase")
        val base = baseService.getBaseById(id)
        return if (base != null) ResponseEntity.ok(base)
        else ResponseEntity.notFound().build()
    }

}
