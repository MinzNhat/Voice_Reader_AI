package com.example.voicereaderapp.data.remote.dto

data class PdfExtractRequest(
    val page: Int? = null
)

data class PdfResponse(
    val text: String,
    val totalPages: Int? = null,
    val pageNumber: Int? = null
)

data class PdfMetadataResponse(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val totalPages: Int
)
