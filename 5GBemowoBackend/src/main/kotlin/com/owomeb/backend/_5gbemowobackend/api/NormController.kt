package com.owomeb.backend._5gbemowobackend.api

import com.owomeb.backend._5gbemowobackend.frontendContentFetchers.Norm
import com.owomeb.backend._5gbemowobackend.frontendContentFetchers.NormDataRepository
import com.owomeb.backend._5gbemowobackend.frontendContentFetchers.Release
import com.owomeb.backend._5gbemowobackend.frontendContentFetchers.Series
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/norms")
class NormController(
    val repository: NormDataRepository
) {

    @GetMapping("/releases", produces = ["application/json"])
    fun getAllReleases(): List<Release> {
        val result = repository.getAllReleases()
        println("Fetched releases: $result")
        return result
    }

    @GetMapping("/series/{releaseId}", produces = ["application/json"])
    fun getSeriesForRelease(@PathVariable releaseId: String): List<Series> {
        val result = repository.getSeriesForRelease(releaseId)
        println("Fetched series for release $releaseId: $result")
        return result
    }

    @GetMapping("/norms/{releaseId}/{seriesId}", produces = ["application/json"])
    fun getNormsForReleaseAndSeries(
        @PathVariable releaseId: String,
        @PathVariable seriesId: String
    ): List<Norm> {
        val result = repository.getNormsForReleaseAndSeries(releaseId, seriesId)
        println("Fetched norms for release $releaseId and series $seriesId: $result")
        return result
    }
}
