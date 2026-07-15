package com.stylekeyboard.app.data.repository

import com.stylekeyboard.app.data.db.dao.AutoSenderLogDao
import com.stylekeyboard.app.data.db.entity.AutoSenderLogEntity
import kotlinx.coroutines.flow.Flow

class AutoSenderLogRepository(private val dao: AutoSenderLogDao) {
    fun observeRecent(limit: Int = 200): Flow<List<AutoSenderLogEntity>> = dao.observeRecent(limit)
    suspend fun insert(entity: AutoSenderLogEntity) = dao.insert(entity)
    suspend fun clearAll() = dao.clearAll()
}
