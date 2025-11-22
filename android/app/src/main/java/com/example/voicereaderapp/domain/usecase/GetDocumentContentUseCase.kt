package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.repository.DocumentRepository
import javax.inject.Inject

class GetDocumentContentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Lấy nội dung text của tài liệu dựa vào ID.
     * Trả về chuỗi rỗng nếu không tìm thấy.
     */
    suspend operator fun invoke(documentId: String): String {
        val document = documentRepository.getDocumentById(documentId)

        return document?.content ?: ""
    }
}
