package com.example.voicereaderapp.domain.usecase.utp

import com.example.voicereaderapp.domain.model.utp.TextDetectionConfig
import com.example.voicereaderapp.domain.model.utp.TextDetectionResult
import com.example.voicereaderapp.domain.model.utp.UniversalText
import com.example.voicereaderapp.domain.repository.utp.DetectionContext
import com.example.voicereaderapp.domain.repository.utp.NormalizationConfig
import com.example.voicereaderapp.domain.repository.utp.TextDetectorRepository
import com.example.voicereaderapp.domain.repository.utp.TextNormalizerRepository
import android.util.Log
import com.example.voicereaderapp.utils.Result
import javax.inject.Inject

/**
 * Main use case for UTP pipeline
 * Combines Layer 1 (Detection) + Layer 2 (Normalization)
 */
class ProcessUniversalTextUseCase @Inject constructor(
    private val textDetectorRepository: TextDetectorRepository,
    private val textNormalizerRepository: TextNormalizerRepository
) {
    /**
     * Full pipeline: Detect -> Normalize -> Return UniversalText
     */
    suspend operator fun invoke(
        context: DetectionContext,
        detectionConfig: TextDetectionConfig = TextDetectionConfig(),
        normalizationConfig: NormalizationConfig = NormalizationConfig()
    ): Result<UniversalText> {
        return try {
            // Step 1: Detect text from all available sources
            val detectionResults = mutableListOf<UniversalText>()
            
            // Try accessibility first (fastest, most accurate for UI)
            if (detectionConfig.enableAccessibility && context.hasAccessibility) {
                when (val result = textDetectorRepository.extractFromAccessibility()) {
                    is TextDetectionResult.Success -> {
                        detectionResults.add(result.universalText)
                    }
                    is TextDetectionResult.Error -> {
                        // Log error but continue
                    }
                    is TextDetectionResult.Empty -> {
                        // No accessibility content
                    }
                }
            }
            
            // Try OCR if bitmap available
            if (detectionConfig.enableOCR && context.hasBitmap != null) {
                when (val result = textDetectorRepository.extractFromOCR(
                    com.example.voicereaderapp.domain.repository.utp.OcrSource.Image(context.hasBitmap),
                    detectionConfig
                )) {
                    is TextDetectionResult.Success -> {
                        detectionResults.add(result.universalText)
                    }
                    is TextDetectionResult.Error -> {
                        // Log error but continue
                    }
                    is TextDetectionResult.Empty -> {
                        // No OCR content
                    }
                }
            }
            
            // Try web fetch if URL available (URL could be content URI or HTTP/HTTPS)
            if (detectionConfig.enableWebFetch && context.hasUrl != null) {
                when (val result = textDetectorRepository.extractFromWeb(
                    context.hasUrl,
                    detectionConfig
                )) {
                    is TextDetectionResult.Success -> {
                        detectionResults.add(result.universalText)
                    }
                    is TextDetectionResult.Error -> {
                        // Log error but continue
                    }
                    is TextDetectionResult.Empty -> {
                        // No web content
                    }
                }
            }
            
            // If no results, return error
            Log.d("ProcessUniversalTextUseCase", "Detection results size=${detectionResults.size}")
            if (detectionResults.isEmpty()) {
                return Result.Error(Exception("No text detected from any source"))
            }
            
            // Step 2: Normalize and merge all sources
            val normalizedText = if (detectionResults.size == 1) {
                // Single source, just normalize
                textNormalizerRepository.normalize(detectionResults, normalizationConfig)
            } else {
                // Multiple sources, merge and normalize
                textNormalizerRepository.normalize(detectionResults, normalizationConfig)
            }
            
            Result.Success(normalizedText)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Auto-detect and process
     */
    suspend fun processAuto(
        context: DetectionContext
    ): Result<UniversalText> {
        return try {
            when (val result = textDetectorRepository.extractAuto(context)) {
                is TextDetectionResult.Success -> {
                    // Normalize single source
                    val normalized = textNormalizerRepository.normalize(
                        listOf(result.universalText)
                    )
                    Result.Success(normalized)
                }
                is TextDetectionResult.Error -> {
                    Result.Error(result.throwable ?: Exception(result.message))
                }
                is TextDetectionResult.Empty -> {
                    Result.Error(Exception("No text detected"))
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
