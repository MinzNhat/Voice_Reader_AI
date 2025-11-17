package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

class UpdateReadPositionUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String, position: Int) {
        repository.updateReadPosition(id, position)
    }
}
