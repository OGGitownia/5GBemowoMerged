package com.owomeb.backend._5gbemowobackend.messageBank


import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class UsedChunk @JsonCreator constructor(
    @JsonProperty("text") val text: String,
    @JsonProperty("index") val index: Int
)
