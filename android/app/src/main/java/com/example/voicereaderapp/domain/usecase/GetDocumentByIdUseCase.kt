package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

class GetDocumentByIdUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String): ReadingDocument? {
        return repository.getDocumentById(id)
    }
}
