package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.data.local.entity.NoteRepository
import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for deleting a document.
 * Encapsulates the business logic for document deletion.
 * Also handles cascade deletion of associated notes.
 */
class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val noteRepository: NoteRepository
) {
    /**
     * Deletes a document by its ID and all associated notes.
     *
     * @param documentId ID of the document to delete
     */
    suspend operator fun invoke(documentId: String) {
        // Delete all notes associated with this document first
        noteRepository.deleteNotesByDocumentId(documentId)

        // Then delete the document
        documentRepository.deleteDocument(documentId)
    }
}
