package com.stylekeyboard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stylekeyboard.app.data.db.entity.TigramEntity
import com.stylekeyboard.app.data.db.entity.TrigramEntity

@Dao
interface TrigramDao {
    @Query("SELECT * FROM trigrams WHERE firstWord = :first AND secondWord = :second ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun suggestNext(first: String, second: String, limit: Int = 3): List<TrigramEntity>

    @Query("SELECT * FROM trigrams WHERE firstWord = :first AND secondWord = :second AND thirdWord = :third LIMIT 1")
    suspend fun get(first: String, second: String, third: String): TrigramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrigramEntity)

    @Query("UPDATE trigrams SET frequency = frequency + 1, lastUsed = :now WHERE firstWord = :first AND secondWord = :second AND thirdWord = :third")
    suspend fun increment(first: String, second: String, third: String, now: Long): Int

    @Query("DELETE FROM trigrams")
    suspend fun clearAll()
}
