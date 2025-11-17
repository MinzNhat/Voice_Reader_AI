package com.example.voicereaderapp.domain.repository.utp

import com.example.voicereaderapp.domain.model.utp.Token
import com.example.voicereaderapp.domain.model.utp.UniversalText
import kotlinx.coroutines.flow.Flow

/**
 * Layer 3: Application Layer
 * All features run on top of normalized UniversalText
 */

/**
 * 1. TTS + Highlight Service
 */
interface TtsHighlightRepository {
    
    /**
     * Speak text with synchronized highlighting
     */
    fun speakWithHighlight(
        text: UniversalText,
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): Flow<HighlightEvent>
    
    /**
     * Stop speaking
     */
    fun stop()
    
    /**
     * Pause/Resume
     */
    fun pause()
    fun resume()
}

/**
 * Highlight event for UI synchronization
 */
sealed class HighlightEvent {
    data class TokenHighlight(val token: Token, val progress: Float) : HighlightEvent()
    data class SentenceHighlight(val tokens: List<Token>) : HighlightEvent()
    object Started : HighlightEvent()
    object Paused : HighlightEvent()
    object Resumed : HighlightEvent()
    object Completed : HighlightEvent()
    data class Error(val message: String) : HighlightEvent()
}

/**
 * 2. Content Summarization Service
 */
interface ContentSummarizerRepository {
    
    /**
     * Summarize text using LLM
     */
    suspend fun summarize(
        text: UniversalText,
        maxLength: Int = 500,
        language: String = "vi"
    ): SummaryResult
    
    /**
     * Extract key points
     */
    suspend fun extractKeyPoints(
        text: UniversalText,
        count: Int = 5
    ): List<String>
    
    /**
     * Generate title/headline
     */
    suspend fun generateTitle(
        text: UniversalText
    ): String
}

/**
 * Summary result
 */
data class SummaryResult(
    val summary: String,
    val keyPoints: List<String>,
    val readingTime: Int, // in minutes
    val language: String
)

/**
 * 3. Translation Service
 */
interface TranslationRepository {
    
    /**
     * Translate text
     */
    suspend fun translate(
        text: UniversalText,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): UniversalText
    
    /**
     * Detect language
     */
    suspend fun detectLanguage(
        text: UniversalText
    ): String
    
    /**
     * Get available languages
     */
    suspend fun getAvailableLanguages(): List<Language>
}

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

/**
 * 4. Chat with Content Service (RAG)
 */
interface ContentChatRepository {
    
    /**
     * Initialize chat session with content
     */
    suspend fun initializeSession(
        text: UniversalText,
        sessionId: String = java.util.UUID.randomUUID().toString()
    ): ChatSession
    
    /**
     * Ask question about content
     */
    suspend fun ask(
        sessionId: String,
        question: String
    ): ChatResponse
    
    /**
     * Get chat history
     */
    suspend fun getHistory(
        sessionId: String
    ): List<ChatMessage>
    
    /**
     * End session
     */
    suspend fun endSession(sessionId: String)
}

data class ChatSession(
    val sessionId: String,
    val contentSummary: String,
    val createdAt: Long
)

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class ChatResponse(
    val answer: String,
    val relevantTokens: List<Token>,
    val confidence: Float
)

/**
 * 5. Context Search Service
 */
interface ContextSearchRepository {
    
    /**
     * Index text for search
     */
    suspend fun indexText(
        text: UniversalText,
        documentId: String
    )
    
    /**
     * Search within indexed documents
     */
    suspend fun search(
        query: String,
        documentIds: List<String>? = null
    ): List<SearchResult>
    
    /**
     * Semantic search using embeddings
     */
    suspend fun semanticSearch(
        query: String,
        topK: Int = 10
    ): List<SearchResult>
    
    /**
     * Delete index
     */
    suspend fun deleteIndex(documentId: String)
}

data class SearchResult(
    val documentId: String,
    val snippet: String,
    val tokens: List<Token>,
    val score: Float
)

/**
 * 6. Content Storage Service
 */
interface ContentStorageRepository {
    
    /**
     * Save content for offline reading
     */
    suspend fun saveContent(
        text: UniversalText,
        title: String? = null,
        tags: List<String> = emptyList()
    ): SavedContent
    
    /**
     * Get saved content
     */
    suspend fun getContent(id: String): SavedContent?
    
    /**
     * Get all saved contents
     */
    suspend fun getAllContents(): List<SavedContent>
    
    /**
     * Delete content
     */
    suspend fun deleteContent(id: String)
    
    /**
     * Search saved contents
     */
    suspend fun searchContents(query: String): List<SavedContent>
}

data class SavedContent(
    val id: String,
    val title: String,
    val universalText: UniversalText,
    val tags: List<String>,
    val savedAt: Long,
    val readCount: Int = 0,
    val lastReadAt: Long? = null
)

/**
 * 7. PDF Export Service
 */
interface PdfExportRepository {
    
    /**
     * Export content as PDF
     */
    suspend fun exportToPdf(
        text: UniversalText,
        title: String,
        outputPath: String? = null
    ): PdfExportResult
    
    /**
     * Export with custom styling
     */
    suspend fun exportWithStyle(
        text: UniversalText,
        style: PdfStyle
    ): PdfExportResult
}

data class PdfExportResult(
    val filePath: String,
    val fileSize: Long,
    val pageCount: Int
)

data class PdfStyle(
    val fontSize: Float = 12f,
    val fontFamily: String = "sans-serif",
    val lineSpacing: Float = 1.5f,
    val margin: Float = 20f,
    val includeHighlights: Boolean = false
)
