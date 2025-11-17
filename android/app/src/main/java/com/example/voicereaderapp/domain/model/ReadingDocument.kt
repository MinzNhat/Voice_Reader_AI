package com.example.voicereaderapp.domain.model

/**
 * Domain model representing a document to be read.
 * Can represent PDF documents, scanned images, or live screen content.
 *
 * @property id Unique identifier for the document
 * @property title Document title
 * @property content Extracted text content from the document
 * @property type Type of document (PDF, IMAGE, LIVE_SCREEN)
 * @property createdAt Timestamp when the document was added
 * @property lastReadPosition Last reading position in the document
 */
data class ReadingDocument(
    val id: String,
    val title: String,
    val content: String,
    val type: DocumentType,
    val createdAt: Long,
    val lastReadPosition: Int = 0
)

/**
 * Enum representing different types of documents supported by the app.
 */
enum class DocumentType {
    PDF,
    IMAGE,
    LIVE_SCREEN
}
