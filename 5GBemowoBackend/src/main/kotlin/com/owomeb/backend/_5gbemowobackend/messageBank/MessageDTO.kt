package com.owomeb.backend._5gbemowobackend.messageBank

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageDTO(
    val id: String,
    val question: String,
    val answer: String,
    val modelName: String,
    val tuners: MutableList<String>,
    val askedAt: Long,
    val answeredAt: Long?,
    val answered: Boolean,
    val userId: Long,
    val chatId: String,
    val baseId: String,
    val release: String,
    val series: String,
    val norm: String,
    val usedContextChunks: List<UsedChunk> = emptyList(),
    val highlighetedFragments: List<HighlightedFragment> = emptyList()
)




fun MessageEntity.toDTO(): MessageDTO = MessageDTO(
    id = id,
    question = question,
    answer = answer,
    modelName = modelName,
    tuners = tuners,
    askedAt = askedAt.toEpochMilli(),
    answeredAt = answeredAt?.toEpochMilli(),
    answered = answered,
    userId = userId,
    chatId = chatId,
    baseId = baseId,
    release = release,
    series = series,
    norm = norm,
    usedContextChunks = usedContextChunks,
    highlighetedFragments = highlighetedFragments
)



