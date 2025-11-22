package com.example.voicereaderapp.domain.repository

interface RagRepository {
    suspend fun ingestText(text: String): Result<Boolean>
    suspend fun askQuestion(question: String): Result<String>
}
