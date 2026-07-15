package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Emoji Shortcuts dictionary. Each row maps a trigger word (e.g. "lol") to a
 * space-separated list of emoji (e.g. "😂 😆") that the keyboard offers in the
 * suggestion strip when the user types the trigger anywhere in the buffer.
 *
 * [triggerMode] is "whole" for whole-word-only matches or "partial" for prefix matches.
 */
@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trigger: String,
    /** Space-separated emoji sequence to insert. */
    val emojis: String,
    val triggerMode: String = "whole",
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)
