package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity

/**
 * Bigram (two consecutive words). Used to rank "next word" suggestions after
 * the user completes a word. Looked up by [firstWord] during typing.
 */
@Entity(tableName = "bigrams", primaryKeys = ["firstWord", "secondWord"])
data class BigramEntity(
    val firstWord: String,
    val secondWord: String,
    val frequency: Int,
    val lastUsed: Long
)
