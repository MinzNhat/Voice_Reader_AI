package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.RagRepository
import javax.inject.Inject

class AskRagUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(question: String) = repository.askQuestion(question)
}
