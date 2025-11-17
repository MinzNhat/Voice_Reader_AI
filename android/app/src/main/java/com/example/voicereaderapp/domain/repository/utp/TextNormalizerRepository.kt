package com.example.voicereaderapp.domain.repository.utp

import com.example.voicereaderapp.domain.model.utp.UniversalText

/**
 * Layer 2: Text Normalizer Layer
 * Normalizes and merges text from multiple sources
 */
interface TextNormalizerRepository {
    
    /**
     * Merge multiple UniversalText sources into one
     * Handles duplicates, ordering, and bounding box mapping
     */
    suspend fun merge(
        sources: List<UniversalText>
    ): UniversalText
    
    /**
     * Remove duplicated text
     */
    suspend fun removeDuplicates(
        text: UniversalText
    ): UniversalText
    
    /**
     * Normalize line breaks and formatting
     */
    suspend fun normalizeFormatting(
        text: UniversalText
    ): UniversalText
    
    /**
     * Order tokens by reading order (top-to-bottom, left-to-right)
     */
    suspend fun orderByReadingSequence(
        text: UniversalText
    ): UniversalText
    
    /**
     * Filter out less important text (ads, navigation, etc)
     */
    suspend fun filterNoise(
        text: UniversalText,
        keepHeaders: Boolean = true,
        keepNavigation: Boolean = false
    ): UniversalText
    
    /**
     * Tokenize text into words/sentences
     */
    suspend fun tokenize(
        text: UniversalText,
        tokenizeBy: TokenizeMode = TokenizeMode.WORD
    ): UniversalText
    
    /**
     * Map each character/word to its bounding box
     */
    suspend fun mapPositions(
        text: UniversalText
    ): UniversalText
    
    /**
     * Full normalization pipeline
     */
    suspend fun normalize(
        sources: List<UniversalText>,
        config: NormalizationConfig = NormalizationConfig()
    ): UniversalText
}

/**
 * Tokenization mode
 */
enum class TokenizeMode {
    CHARACTER,
    WORD,
    SENTENCE,
    PARAGRAPH
}

/**
 * Normalization configuration
 */
data class NormalizationConfig(
    val removeDuplicates: Boolean = true,
    val normalizeFormatting: Boolean = true,
    val orderByReading: Boolean = true,
    val filterNoise: Boolean = true,
    val tokenizeBy: TokenizeMode = TokenizeMode.WORD,
    val mapPositions: Boolean = true,
    val minConfidence: Float = 0.5f
)
