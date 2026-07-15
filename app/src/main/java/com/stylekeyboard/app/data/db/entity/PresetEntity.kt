package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A font-conversion preset. Each preset stores a character map as JSON:
 *   { "a": "𝐚", "b": "𝐛", ... , "0": "𝟎", "!": "！", ... }
 * The Host App editor writes this map; the keyboard reads the active preset's map
 * on every keystroke via [com.stylekeyboard.app.util.TextConverter.convertText].
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** JSON-serialized Map<String,String> from source char (or emoji) -> replacement. */
    val mappingJson: String,
    /** A short human description shown under the preset name in the carousel. */
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)
