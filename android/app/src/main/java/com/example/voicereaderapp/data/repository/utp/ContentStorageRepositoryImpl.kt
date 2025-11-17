package com.example.voicereaderapp.data.repository.utp

import com.example.voicereaderapp.domain.model.utp.UniversalText
import com.example.voicereaderapp.domain.repository.utp.SavedContent
import com.example.voicereaderapp.domain.repository.utp.ContentStorageRepository
import com.example.voicereaderapp.data.local.dao.utp.SavedContentDao
import com.example.voicereaderapp.data.local.entity.utp.SavedContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Layer 3B: Content Storage Implementation
 */
class ContentStorageRepositoryImpl @Inject constructor(
    private val savedContentDao: SavedContentDao
) : ContentStorageRepository {
    
    override suspend fun saveContent(
        text: UniversalText,
        title: String?,
        tags: List<String>
    ): SavedContent {
        return withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val savedContent = SavedContent(
                id = id,
                title = title ?: text.metadata.title ?: "Untitled",
                universalText = text,
                tags = tags,
                savedAt = System.currentTimeMillis()
            )
            
            // Save to database
            savedContentDao.insert(savedContent.toEntity())
            
            savedContent
        }
    }
    
    override suspend fun getContent(id: String): SavedContent? {
        return withContext(Dispatchers.IO) {
            savedContentDao.getById(id)?.toDomain()
        }
    }
    
    override suspend fun getAllContents(): List<SavedContent> {
        return withContext(Dispatchers.IO) {
            savedContentDao.getAll().map { it.toDomain() }
        }
    }
    
    override suspend fun deleteContent(id: String) {
        withContext(Dispatchers.IO) {
            savedContentDao.deleteById(id)
        }
    }
    
    override suspend fun searchContents(query: String): List<SavedContent> {
        return withContext(Dispatchers.IO) {
            savedContentDao.search("%$query%").map { it.toDomain() }
        }
    }
}

// Extension functions for mapping
private fun SavedContent.toEntity(): SavedContentEntity {
    return SavedContentEntity(
        id = id,
        title = title,
        rawText = universalText.rawText,
        tags = tags.joinToString(","),
        savedAt = savedAt,
        readCount = readCount,
        lastReadAt = lastReadAt
    )
}

private fun SavedContentEntity.toDomain(): SavedContent {
    // TODO: Reconstruct UniversalText from database
    // For now, create a simple version
    val universalText = UniversalText(
        rawText = rawText,
        tokens = emptyList(),
        positions = emptyList(),
        sourceType = com.example.voicereaderapp.domain.model.utp.TextSourceType.HYBRID
    )
    
    return SavedContent(
        id = id,
        title = title,
        universalText = universalText,
        tags = tags.split(",").filter { it.isNotEmpty() },
        savedAt = savedAt,
        readCount = readCount,
        lastReadAt = lastReadAt
    )
}
