package com.stylekeyboard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stylekeyboard.app.data.db.entity.BigramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BigramDao {
    @Query("SELECT * FROM bigrams WHERE firstWord = :first ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun suggestNext(first: String, limit: Int = 5): List<BigramEntity>

    @Query("SELECT * FROM bigrams WHERE firstWord = :first AND secondWord = :second LIMIT 1")
    suspend fun get(first: String, second: String): BigramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BigramEntity)

    @Query("UPDATE bigrams SET frequency = frequency + 1, lastUsed = :now WHERE firstWord = :first AND secondWord = :second")
    suspend fun increment(first: String, second: String, now: Long): Int

    @Query("SELECT * FROM bigrams ORDER BY frequency DESC LIMIT :limit")
    fun observeTop(limit: Int = 50): Flow<List<BigramEntity>>

    @Query("DELETE FROM bigrams")
    suspend fun clearAll()
}
