package com.example.voicereaderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument

/**
 * Room entity representing a document in the local database.
 * Maps to the "documents" table.
 *
 * @property id Primary key - unique identifier for the document
 * @property title Document title
 * @property content Extracted text content
 * @property type Type of document as string (PDF, IMAGE, LIVE_SCREEN)
 * @property createdAt Timestamp of creation
 * @property lastReadPosition Last reading position
 * @property voiceId Selected voice ID for this document (nullable for backward compatibility)
 * @property language Selected language for this document (nullable for backward compatibility)
 * @property speed Playback speed for this document (nullable for backward compatibility)
 * @property audioCacheJson Multiple cached audio versions as JSON (one per voice+language)
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val type: String,
    val createdAt: Long,
    val lastReadPosition: Int = 0,
    val voiceId: String? = null,
    val language: String? = null,
    val speed: Float? = null,
    val audioCacheJson: String? = null
)

/**
 * Extension function to convert DocumentEntity to domain model.
 *
 * @return ReadingDocument domain model
 */
fun DocumentEntity.toDomain(): ReadingDocument {
    return ReadingDocument(
        id = id,
        title = title,
        content = content,
        type = DocumentType.valueOf(type),
        createdAt = createdAt,
        lastReadPosition = lastReadPosition,
        voiceId = voiceId,
        language = language,
        speed = speed,
        audioCacheJson = audioCacheJson
    )
}

/**
 * Extension function to convert domain model to DocumentEntity.
 *
 * @return DocumentEntity for database storage
 */
fun ReadingDocument.toEntity(): DocumentEntity {
    return DocumentEntity(
        id = id,
        title = title,
        content = content,
        type = type.name,
        createdAt = createdAt,
        lastReadPosition = lastReadPosition,
        voiceId = voiceId,
        language = language,
        speed = speed,
        audioCacheJson = audioCacheJson
    )
}
