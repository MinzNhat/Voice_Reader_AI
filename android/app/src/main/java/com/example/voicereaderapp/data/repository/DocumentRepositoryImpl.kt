package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.local.dao.DocumentDao
import com.example.voicereaderapp.data.local.entity.toDomain
import com.example.voicereaderapp.data.local.entity.toEntity
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.DocumentRepository
import com.google.firebase.appdistribution.gradle.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DocumentRepository.
 * Handles data operations for reading documents using local database.
 *
 * @property documentDao DAO for database operations
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val apiService: ApiService
) : DocumentRepository {
    /**
     * Retrieves all documents from local database.
     * Converts database entities to domain models.
     *
     * @return Flow emitting list of ReadingDocument domain models
     */
    override fun getAllDocuments(): Flow<List<ReadingDocument>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Retrieves a specific document by ID.
     *
     * @param id Unique identifier of the document
     * @return ReadingDocument if found, null otherwise
     */
    override suspend fun getDocumentById(id: String): ReadingDocument? {
        return documentDao.getDocumentById(id)?.toDomain()
    }

    /**
     * Saves a document to the local database.
     * Converts domain model to database entity.
     *
     * @param document ReadingDocument to be saved
     */
    override suspend fun saveDocument(document: ReadingDocument) {
        documentDao.insertDocument(document.toEntity())
    }

    /**
     * Deletes a document from the database.
     *
     * @param documentId ID of the document to delete
     */
    override suspend fun deleteDocument(documentId: String) {
        documentDao.deleteDocumentById(documentId)
    }

    /**
     * Updates the last read position for a document.
     *
     * @param documentId ID of the document
     * @param position New read position
     */
    override suspend fun updateReadPosition(documentId: String, position: Int) {
        documentDao.updateReadPosition(documentId, position)
    }
}
