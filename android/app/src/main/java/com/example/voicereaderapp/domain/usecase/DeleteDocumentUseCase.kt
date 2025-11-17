package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for deleting a document.
 * Encapsulates the business logic for document deletion.
 */
class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Deletes a document by its ID.
     *
     * @param documentId ID of the document to delete
     */
    suspend operator fun invoke(documentId: String) {
        documentRepository.deleteDocument(documentId)
    }
}
