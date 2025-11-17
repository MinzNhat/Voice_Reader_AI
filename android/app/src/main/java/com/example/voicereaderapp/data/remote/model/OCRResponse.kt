package com.example.voicereaderapp.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * OCR API Response from backend
 * Maps to the normalized format from /ocr endpoint
 * CRITICAL: Includes imageWidth/imageHeight to scale coordinates properly
 */
data class OCRResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("words")
    val words: List<OCRWord>,

    @SerializedName("imageWidth")
    val imageWidth: Int = 0,

    @SerializedName("imageHeight")
    val imageHeight: Int = 0
)

data class OCRWord(
    @SerializedName("text")
    val text: String,

    @SerializedName("bbox")
    val bbox: BoundingBox,

    @SerializedName("index")
    val index: Int
)

data class BoundingBox(
    @SerializedName("x1")
    val x1: Float,

    @SerializedName("y1")
    val y1: Float,

    @SerializedName("x2")
    val x2: Float,

    @SerializedName("y2")
    val y2: Float,

    @SerializedName("x3")
    val x3: Float,

    @SerializedName("y3")
    val y3: Float,

    @SerializedName("x4")
    val x4: Float,

    @SerializedName("y4")
    val y4: Float
) {
    /**
     * Convert bounding box vertices to RectF for Canvas drawing
     * Uses min/max to handle rotated text
     */
    fun toRectF(): android.graphics.RectF {
        val left = minOf(x1, x2, x3, x4)
        val top = minOf(y1, y2, y3, y4)
        val right = maxOf(x1, x2, x3, x4)
        val bottom = maxOf(y1, y2, y3, y4)
        return android.graphics.RectF(left, top, right, bottom)
    }
}
