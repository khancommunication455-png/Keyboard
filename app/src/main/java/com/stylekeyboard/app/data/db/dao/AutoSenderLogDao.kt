package com.stylekeyboard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stylekeyboard.app.data.db.entity.AutoSenderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoSenderLogDao {
    @Query("SELECT * FROM auto_sender_log ORDER BY sentAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AutoSenderLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AutoSenderLogEntity): Long

    @Query("DELETE FROM auto_sender_log")
    suspend fun clearAll()
}
