package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for updating a document's title.
 * Encapsulates the business logic for renaming documents.
 */
class UpdateDocumentTitleUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Updates the title of a document.
     *
     * @param documentId ID of the document to update
     * @param newTitle New title for the document
     */
    suspend operator fun invoke(documentId: String, newTitle: String) {
        val document = documentRepository.getDocumentById(documentId)
        if (document != null) {
            val updatedDocument = document.copy(title = newTitle)
            documentRepository.saveDocument(updatedDocument)
        }
    }
}
