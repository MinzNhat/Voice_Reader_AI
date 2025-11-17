package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all documents.
 * Encapsulates the business logic for fetching documents from the repository.
 *
 * @property documentRepository Repository providing document data
 */
class GetAllDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Executes the use case to retrieve all documents.
     *
     * @return Flow emitting list of all ReadingDocument objects
     */
    operator fun invoke(): Flow<List<ReadingDocument>> {
        return documentRepository.getAllDocuments()
    }
}
