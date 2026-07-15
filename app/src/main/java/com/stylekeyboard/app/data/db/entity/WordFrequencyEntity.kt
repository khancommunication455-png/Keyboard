package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Unigram frequency table for the local predictive-text engine.
 * Seeded at first launch with a small common-words dictionary and grown
 * every time the user confirms a word. Looked up by prefix during typing.
 */
@Entity(tableName = "word_frequency")
data class WordFrequencyEntity(
    @PrimaryKey val word: String,
    val frequency: Int,
    val lastUsed: Long,
    val isUserAdded: Boolean = false
)
