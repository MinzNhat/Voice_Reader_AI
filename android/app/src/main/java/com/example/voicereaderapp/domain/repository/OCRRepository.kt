package com.example.voicereaderapp.domain.repository

import com.example.voicereaderapp.data.remote.model.OCRResponse
import com.example.voicereaderapp.utils.Result
import java.io.File

/**
 * Repository interface for OCR operations
 */
interface OCRRepository {

    /**
     * Perform OCR on a file (PDF or image)
     * @param file The file to process
     * @return Result containing OCR response with text and bounding boxes
     */
    suspend fun performOCR(file: File): Result<OCRResponse>

    /**
     * Perform OCR on a cropped region of an image
     * @param file The image file
     * @param x X coordinate of crop region
     * @param y Y coordinate of crop region
     * @param width Width of crop region
     * @param height Height of crop region
     * @return Result containing OCR response
     */
    suspend fun performOCRWithCrop(
        file: File,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Result<OCRResponse>
}
