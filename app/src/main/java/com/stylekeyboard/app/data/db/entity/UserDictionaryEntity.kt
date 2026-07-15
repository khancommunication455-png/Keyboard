package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Words the user has explicitly added (names, slang, etc.). These always
 * surface in suggestions even with a frequency of 1.
 */
@Entity(tableName = "user_dictionary")
data class UserDictionaryEntity(
    @PrimaryKey val word: String,
    val addedDate: Long
)
