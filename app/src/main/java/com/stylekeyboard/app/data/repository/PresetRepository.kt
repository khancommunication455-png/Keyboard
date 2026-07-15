package com.stylekeyboard.app.data.repository

import com.stylekeyboard.app.data.db.dao.PresetDao
import com.stylekeyboard.app.data.db.entity.PresetEntity
import com.stylekeyboard.app.data.model.JsonCodec
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val dao: PresetDao) {

    fun observeAll(): Flow<List<PresetEntity>> = dao.observeAll()

    suspend fun getAll(): List<PresetEntity> = dao.getAll()

    suspend fun getById(id: Long): PresetEntity? = dao.getById(id)

    suspend fun insert(name: String, mapping: Map<String, String>, description: String = ""): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            PresetEntity(
                name = name,
                mappingJson = JsonCodec.encodeMap(mapping),
                description = description,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(preset: PresetEntity, mapping: Map<String, String>, name: String, description: String) {
        dao.update(
            preset.copy(
                name = name,
                description = description,
                mappingJson = JsonCodec.encodeMap(mapping),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicate(preset: PresetEntity): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            preset.copy(
                id = 0,
                name = preset.name + " (copy)",
                createdAt = now,
                updatedAt = now,
                isBuiltIn = false
            )
        )
    }

    suspend fun delete(preset: PresetEntity) = dao.delete(preset)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun count(): Int = dao.count()
}
