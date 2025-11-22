package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.RagRepository
import javax.inject.Inject

class IngestRagUseCase @Inject constructor(
    private val repository: RagRepository
) {
    // UseCase chỉ chứa 1 hàm invoke (toán tử gọi)
    suspend operator fun invoke(text: String) = repository.ingestText(text)
}
