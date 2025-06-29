package com.owomeb.backend._5gbemowobackend.messageBank

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class HighlightedFragment @JsonCreator constructor(
    @JsonProperty("quote") val quote: String,
    @JsonProperty("occurrence") val occurrence: Int,
    @JsonProperty("chunkIndex") val chunkIndex: Int
)
