package com.stylekeyboard.app.data.repository

import com.stylekeyboard.app.data.db.dao.ShortcutDao
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.data.model.ShortcutExport
import kotlinx.coroutines.flow.Flow

class ShortcutRepository(private val dao: ShortcutDao) {

    fun observeAll(): Flow<List<ShortcutEntity>> = dao.observeAll()

    suspend fun getAll(): List<ShortcutEntity> = dao.getAll()

    suspend fun insert(trigger: String, emojis: String, mode: String = "whole"): Long =
        dao.insert(
            ShortcutEntity(
                trigger = trigger.lowercase(),
                emojis = emojis,
                triggerMode = mode
            )
        )

    suspend fun update(entity: ShortcutEntity, trigger: String, emojis: String, mode: String) =
        dao.update(
            entity.copy(
                trigger = trigger.lowercase(),
                emojis = emojis,
                triggerMode = mode
            )
        )

    suspend fun delete(entity: ShortcutEntity) = dao.delete(entity)

    suspend fun exportJson(): String {
        val all = dao.getAll()
        val exports = all.map {
            ShortcutExport(it.trigger, it.emojis, it.triggerMode)
        }
        return JsonCodec.encodeShortcutList(exports)
    }

    suspend fun importJson(json: String, replaceAll: Boolean = false) {
        val list = JsonCodec.decodeShortcutList(json)
        val current = dao.getAll().associateBy { it.trigger }
        list.forEach { item ->
            val existing = current[item.trigger]
            if (existing != null) {
                dao.update(
                    existing.copy(
                        emojis = item.emojis,
                        triggerMode = item.triggerMode
                    )
                )
            } else {
                dao.insert(
                    ShortcutEntity(
                        trigger = item.trigger.lowercase(),
                        emojis = item.emojis,
                        triggerMode = item.triggerMode
                    )
                )
            }
        }
    }
}
