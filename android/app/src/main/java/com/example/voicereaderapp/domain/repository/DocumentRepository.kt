package com.example.voicereaderapp.domain.repository

import com.example.voicereaderapp.domain.model.ReadingDocument
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing reading documents.
 * Defines operations for CRUD functionality on documents.
 * Implementation will be in the data layer.
 */
interface DocumentRepository {
    /**
     * Retrieves all documents stored in the local database.
     *
     * @return Flow emitting list of ReadingDocument objects
     */
    fun getAllDocuments(): Flow<List<ReadingDocument>>

    /**
     * Retrieves a specific document by its ID.
     *
     * @param id Unique identifier of the document
     * @return The ReadingDocument if found, null otherwise
     */
    suspend fun getDocumentById(id: String): ReadingDocument?

    /**
     * Saves a new document or updates an existing one.
     *
     * @param document The ReadingDocument to be saved
     */
    suspend fun saveDocument(document: ReadingDocument)

    /**
     * Deletes a document from storage.
     *
     * @param documentId ID of the document to delete
     */
    suspend fun deleteDocument(documentId: String)

    /**
     * Updates the last read position for a document.
     *
     * @param documentId ID of the document
     * @param position Last reading position
     */
    suspend fun updateReadPosition(documentId: String, position: Int)
}
