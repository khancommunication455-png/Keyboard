package com.stylekeyboard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stylekeyboard.app.data.db.entity.UserDictionaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDictionaryDao {
    @Query("SELECT * FROM user_dictionary ORDER BY addedDate DESC")
    fun observeAll(): Flow<List<UserDictionaryEntity>>

    @Query("SELECT * FROM user_dictionary WHERE word LIKE :prefix || '%' ORDER BY word LIMIT 10")
    suspend fun suggestByPrefix(prefix: String): List<UserDictionaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserDictionaryEntity)

    @Delete
    suspend fun delete(entity: UserDictionaryEntity)

    @Query("DELETE FROM user_dictionary WHERE word = :word")
    suspend fun deleteByWord(word: String)
}
