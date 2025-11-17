package com.example.voicereaderapp.data.local.dao.utp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voicereaderapp.data.local.entity.utp.SavedContentEntity

@Dao
interface SavedContentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: SavedContentEntity)
    
    @Query("SELECT * FROM saved_contents WHERE id = :id")
    suspend fun getById(id: String): SavedContentEntity?
    
    @Query("SELECT * FROM saved_contents ORDER BY savedAt DESC")
    suspend fun getAll(): List<SavedContentEntity>
    
    @Query("SELECT * FROM saved_contents WHERE title LIKE :query OR rawText LIKE :query")
    suspend fun search(query: String): List<SavedContentEntity>
    
    @Query("DELETE FROM saved_contents WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM saved_contents")
    suspend fun deleteAll()
}
