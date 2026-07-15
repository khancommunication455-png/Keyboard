package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row app configuration. The Host App writes to this row; the keyboard
 * service reads it (and caches a snapshot) every time it starts a new input
 * connection so appearance/sound/glint behaviour is consistent system-wide.
 *
 * All compound settings are JSON-serialized for forward compatibility:
 *   - gifBackgroundUri: content:// URI string, or "" for none
 *   - glintConfig: {"enabled":true,"color":0xFF06E6FF,"speedMs":2200,"opacity":0.35}
 *   - keyShape: "square" | "circle" | "transparent_neon"
 *   - soundConfig: {"pack":"mechanical","volume":0.6,"muted":false,"customUri":""}
 *   - hapticsEnabled: true
 *   - activePresetId: 1L
 *   - emojiShortcutsEnabled: true
 *   - predictiveTextEnabled: true
 */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val gifBackgroundUri: String = "",
    val glintConfigJson: String = "",
    val keyShape: String = "square",
    val soundConfigJson: String = "",
    val hapticsEnabled: Boolean = true,
    val activePresetId: Long = 1L,
    val emojiShortcutsEnabled: Boolean = true,
    val predictiveTextEnabled: Boolean = true
)
