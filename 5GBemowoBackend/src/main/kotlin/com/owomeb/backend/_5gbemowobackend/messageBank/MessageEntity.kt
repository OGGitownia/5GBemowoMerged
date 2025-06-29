package com.owomeb.backend._5gbemowobackend.messageBank

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.Instant


@Entity
@Table(name = "chat_messages")
data class MessageEntity(

    @Id
    @Column(name = "id", nullable = false, unique = true)
    val id: String = "",

    @Column(nullable = false, length = 10000)
    val question: String = "",

    @Column(length = 10000)
    var answer: String = "",

    @Column(nullable = false)
    val modelName: String = "",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message_tuners", joinColumns = [JoinColumn(name = "message_id")])
    @Column(name = "tuner")
    val tuners: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    val askedAt: Instant = Instant.now(),

    @Column
    var answeredAt: Instant? = null,

    @Column(nullable = false)
    var answered: Boolean = false,

    @Column(nullable = false)
    val userId: Long = 0L,

    @Column(nullable = false)
    val chatId: String = "",

    @Column(nullable = false)
    val baseId: String = "",

    @Column(nullable = false)
    val release: String = "",

    @Column(nullable = false)
    val series: String = "",

    @Column(nullable = true)
    val norm: String = "",

    @Lob
    @Column(nullable = false)
    @Convert(converter = UsedContextChunksConverter::class)
    val usedContextChunks: MutableList<UsedChunk> = mutableListOf(),


    @Lob
    @Column(nullable = false)
    @Convert(converter = HighlightedFragmentListConverter::class)
    val highlighetedFragments: MutableList<HighlightedFragment> = mutableListOf()
)


@Converter
class UsedContextChunksConverter : AttributeConverter<List<UsedChunk>, String> {
    override fun convertToDatabaseColumn(attribute: List<UsedChunk>): String {
        return ObjectMapper().writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): List<UsedChunk> {
        val typeRef = object : TypeReference<List<UsedChunk>>() {}
        return ObjectMapper().readValue(dbData, typeRef)
    }
}


@Converter
class HighlightedFragmentListConverter : AttributeConverter<List<HighlightedFragment>, String> {
    override fun convertToDatabaseColumn(attribute: List<HighlightedFragment>): String {
        return ObjectMapper().writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): List<HighlightedFragment> {
        val typeRef = object : TypeReference<List<HighlightedFragment>>() {}
        return ObjectMapper().readValue(dbData, typeRef)
    }
}
