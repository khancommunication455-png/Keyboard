package com.stylekeyboard.app.util

import com.stylekeyboard.app.data.db.entity.ShortcutEntity

/**
 * Built-in emoji shortcuts seeded into Room on first launch.
 * ~18 sensible defaults covering common chat reactions.
 */
object DefaultShortcuts {

    fun all(): List<ShortcutEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ShortcutEntity(trigger = "lol", emojis = "😂 😆", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "love", emojis = "💕 😘", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "omg", emojis = "🤯 😱", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "fire", emojis = "🔥", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "sad", emojis = "😢 💔", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "happy", emojis = "😀 😊", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "angry", emojis = "😡 😤", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "cool", emojis = "😎 🕶", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "ok", emojis = "👍 ✅", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "yes", emojis = "✅ 💯", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "no", emojis = "❌ 🚫", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "thanks", emojis = "🙏 😊", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "bye", emojis = "👋 😘", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "hi", emojis = "👋 😀", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "wow", emojis = "😮 🤩", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "sleep", emojis = "😴 💤", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "coffee", emojis = "☕ 😌", triggerMode = "whole", isBuiltIn = true, createdAt = now),
            ShortcutEntity(trigger = "party", emojis = "🎉 🥳", triggerMode = "whole", isBuiltIn = true, createdAt = now)
        )
    }
}
