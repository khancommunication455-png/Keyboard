package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity

/**
 * Trigram (three consecutive words). Optional but improves accuracy for
 * common three-word phrases (e.g. "how are you").
 */
@Entity(tableName = "trigrams", primaryKeys = ["firstWord", "secondWord", "thirdWord"])
data class TrigramEntity(
    val firstWord: String,
    val secondWord: String,
    val thirdWord: String,
    val frequency: Int,
    val lastUsed: Long
)
