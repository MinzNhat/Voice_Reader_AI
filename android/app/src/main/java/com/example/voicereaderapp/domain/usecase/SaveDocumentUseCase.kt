package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for saving a document.
 * Handles validation and business logic before persisting the document.
 *
 * @property documentRepository Repository for document persistence
 */
class SaveDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Executes the use case to save a document.
     * Validates the document before saving.
     *
     * @param document The ReadingDocument to be saved
     * @throws IllegalArgumentException if document validation fails
     */
    suspend operator fun invoke(document: ReadingDocument) {
        // Validate document before saving
        require(document.title.isNotBlank()) { "Document title cannot be blank" }
        require(document.content.isNotBlank()) { "Document content cannot be blank" }
        
        documentRepository.saveDocument(document)
    }
}
