package com.example.voicereaderapp.data.repository.utp

import android.graphics.Rect
import com.example.voicereaderapp.domain.model.utp.TextSourceType
import com.example.voicereaderapp.domain.model.utp.Token
import com.example.voicereaderapp.domain.model.utp.UniversalText
import com.example.voicereaderapp.domain.repository.utp.NormalizationConfig
import com.example.voicereaderapp.domain.repository.utp.TextNormalizerRepository
import com.example.voicereaderapp.domain.repository.utp.TokenizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Layer 2 Implementation: Text Normalizer Layer
 */
class TextNormalizerRepositoryImpl @Inject constructor() : TextNormalizerRepository {
    
    /**
     * Full normalization pipeline
     */
    override suspend fun normalize(
        sources: List<UniversalText>,
        config: NormalizationConfig
    ): UniversalText {
        return withContext(Dispatchers.Default) {
            var result = merge(sources)
            
            if (config.removeDuplicates) {
                result = removeDuplicates(result)
            }
            
            if (config.normalizeFormatting) {
                result = normalizeFormatting(result)
            }
            
            if (config.orderByReading) {
                result = orderByReadingSequence(result)
            }
            
            if (config.filterNoise) {
                result = filterNoise(result)
            }
            
            if (config.tokenizeBy != TokenizeMode.WORD) {
                result = tokenize(result, config.tokenizeBy)
            }
            
            if (config.mapPositions) {
                result = mapPositions(result)
            }
            
            result
        }
    }
    
    /**
     * Merge multiple sources
     */
    override suspend fun merge(sources: List<UniversalText>): UniversalText {
        return withContext(Dispatchers.Default) {
            if (sources.isEmpty()) {
                return@withContext UniversalText(
                    rawText = "",
                    tokens = emptyList(),
                    positions = emptyList(),
                    sourceType = TextSourceType.HYBRID
                )
            }
            
            if (sources.size == 1) {
                return@withContext sources.first()
            }
            
            // Merge strategies
            val allTokens = sources.flatMap { it.tokens }
            val allPositions = sources.flatMap { it.positions }
            
            // Combine raw text
            val rawText = sources.joinToString("\n") { it.rawText }
            
            // Determine source type
            val sourceType = if (sources.map { it.sourceType }.distinct().size > 1) {
                TextSourceType.HYBRID
            } else {
                sources.first().sourceType
            }
            
            UniversalText(
                rawText = rawText,
                tokens = allTokens,
                positions = allPositions,
                sourceType = sourceType,
                metadata = sources.first().metadata
            )
        }
    }
    
    /**
     * Remove duplicates
     */
    override suspend fun removeDuplicates(text: UniversalText): UniversalText {
        return withContext(Dispatchers.Default) {
            val uniqueTokens = text.tokens.distinctBy { 
                "${it.text}_${it.position.centerX()}_${it.position.centerY()}" 
            }
            
            text.copy(
                tokens = uniqueTokens,
                positions = uniqueTokens.map { it.position }
            )
        }
    }
    
    /**
     * Normalize formatting
     */
    override suspend fun normalizeFormatting(text: UniversalText): UniversalText {
        return withContext(Dispatchers.Default) {
            val normalized = text.rawText
                .replace(Regex("\\s+"), " ") // Multiple spaces to single
                .replace(Regex("\\n{3,}"), "\n\n") // Multiple newlines to double
                .trim()
            
            text.copy(rawText = normalized)
        }
    }
    
    /**
     * Order by reading sequence (top-to-bottom, left-to-right)
     */
    override suspend fun orderByReadingSequence(text: UniversalText): UniversalText {
        return withContext(Dispatchers.Default) {
            val orderedTokens = text.tokens.sortedWith(
                compareBy(
                    { it.position.top },  // Top to bottom
                    { it.position.left }  // Left to right
                )
            ).mapIndexed { index, token ->
                token.copy(index = index)
            }
            
            text.copy(
                tokens = orderedTokens,
                positions = orderedTokens.map { it.position }
            )
        }
    }
    
    /**
     * Filter noise (ads, navigation, etc)
     */
    override suspend fun filterNoise(
        text: UniversalText,
        keepHeaders: Boolean,
        keepNavigation: Boolean
    ): UniversalText {
        return withContext(Dispatchers.Default) {
            // Simple noise filtering based on text patterns
            val noisePatterns = listOf(
                "advertisement", "sponsored", "ad", "menu", "navigation",
                "cookie", "subscribe", "newsletter"
            )
            
            val filteredTokens = text.tokens.filter { token ->
                val lowerText = token.text.lowercase()
                !noisePatterns.any { lowerText.contains(it) }
            }
            
            text.copy(
                tokens = filteredTokens,
                positions = filteredTokens.map { it.position },
                rawText = filteredTokens.joinToString(" ") { it.text }
            )
        }
    }
    
    /**
     * Tokenize text
     */
    override suspend fun tokenize(
        text: UniversalText,
        tokenizeBy: TokenizeMode
    ): UniversalText {
        return withContext(Dispatchers.Default) {
            when (tokenizeBy) {
                TokenizeMode.CHARACTER -> {
                    // Split into characters
                    val charTokens = mutableListOf<Token>()
                    text.rawText.forEachIndexed { index, char ->
                        if (!char.isWhitespace()) {
                            charTokens.add(
                                Token(
                                    text = char.toString(),
                                    position = Rect(), // Will be mapped later
                                    index = index,
                                    sourceType = text.sourceType,
                                    startIndex = index,
                                    endIndex = index + 1
                                )
                            )
                        }
                    }
                    text.copy(tokens = charTokens)
                }
                TokenizeMode.WORD -> {
                    // Already tokenized by words (default)
                    text
                }
                TokenizeMode.SENTENCE -> {
                    // Split into sentences
                    val sentences = text.rawText.split(Regex("[.!?]+"))
                    var cursor = 0
                    val sentenceTokens = sentences.mapIndexed { idx, sentence ->
                        val trimmed = sentence.trim()
                        val start = text.rawText.indexOf(trimmed, cursor).takeIf { it >= 0 } ?: cursor
                        val end = start + trimmed.length
                        cursor = end
                        Token(
                            text = trimmed,
                            position = Rect(),
                            index = idx,
                            sourceType = text.sourceType,
                            startIndex = start,
                            endIndex = end
                        )
                    }
                    text.copy(tokens = sentenceTokens)
                }
                TokenizeMode.PARAGRAPH -> {
                    // Split into paragraphs
                    val paragraphs = text.rawText.split(Regex("\\n\\n+"))
                    var cursor2 = 0
                    val paragraphTokens = paragraphs.mapIndexed { idx, paragraph ->
                        val trimmed = paragraph.trim()
                        val start = text.rawText.indexOf(trimmed, cursor2).takeIf { it >= 0 } ?: cursor2
                        val end = start + trimmed.length
                        cursor2 = end
                        Token(
                            text = trimmed,
                            position = Rect(),
                            index = idx,
                            sourceType = text.sourceType,
                            startIndex = start,
                            endIndex = end
                        )
                    }
                    text.copy(tokens = paragraphTokens)
                }
            }
        }
    }
    
    /**
     * Map character/word positions to bounding boxes
     */
    override suspend fun mapPositions(text: UniversalText): UniversalText {
        return withContext(Dispatchers.Default) {
            // Ensure each token has correct position mapping
            // Already done during OCR/Accessibility extraction
            text
        }
    }
}
