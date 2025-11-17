package com.example.voicereaderapp.data.local.dao

import androidx.room.*
import com.example.voicereaderapp.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the DocumentEntity.
 * Provides methods to perform CRUD operations on the documents table in the database.
 */
@Dao
interface DocumentDao {
    /**
     * Retrieves all documents from the database, ordered by creation time in descending order.
     *
     * @return A Flow emitting a list of DocumentEntity objects
     */
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    /**
     * Retrieves a document by its unique ID.
     *
     * @param id The ID of the document to be retrieved
     * @return The DocumentEntity object if found, or null if not found
     */
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    /**
     * Inserts a new document into the database.
     * If a document with the same ID already exists, it will be replaced.
     *
     * @param document The DocumentEntity object to be inserted
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    /**
     * Updates an existing document in the database.
     *
     * @param document The DocumentEntity object with updated values
     */
    @Update
    suspend fun updateDocument(document: DocumentEntity)

    /**
     * Deletes a document from the database by its unique ID.
     *
     * @param id The ID of the document to be deleted
     */
    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    /**
     * Updates the last read position for a specific document.
     *
     * @param id The ID of the document
     * @param position The new read position
     */
    @Query("UPDATE documents SET lastReadPosition = :position WHERE id = :id")
    suspend fun updateReadPosition(id: String, position: Int)
}
