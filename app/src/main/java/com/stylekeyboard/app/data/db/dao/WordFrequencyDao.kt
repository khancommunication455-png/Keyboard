package com.stylekeyboard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stylekeyboard.app.data.db.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordFrequencyDao {
    @Query("SELECT * FROM word_frequency WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun suggestByPrefix(prefix: String, limit: Int = 8): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency WHERE word = :word LIMIT 1")
    suspend fun get(word: String): WordFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WordFrequencyEntity)

    @Query("UPDATE word_frequency SET frequency = frequency + 1, lastUsed = :now WHERE word = :word")
    suspend fun increment(word: String, now: Long): Int

    @Query("SELECT * FROM word_frequency ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    fun observeTop(limit: Int = 100): Flow<List<WordFrequencyEntity>>

    @Query("UPDATE word_frequency SET frequency = MAX(1, frequency / 2), lastUsed = lastUsed WHERE lastUsed < :cutoff")
    suspend fun decayOldEntries(cutoff: Long): Int

    @Query("DELETE FROM word_frequency WHERE isUserAdded = 0 AND frequency <= 1 AND lastUsed < :cutoff")
    suspend fun pruneStale(cutoff: Long): Int

    @Query("DELETE FROM word_frequency WHERE isUserAdded = 0")
    suspend fun clearLearned()
}
