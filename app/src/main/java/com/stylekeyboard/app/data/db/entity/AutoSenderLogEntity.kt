package com.stylekeyboard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row in the Auto Sender's run log. The Host App displays these so the user
 * can see exactly what was sent, when, and to which target.
 */
@Entity(tableName = "auto_sender_log")
data class AutoSenderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sentAt: Long,
    val targetPackage: String,
    val message: String,
    val status: String // "sent" | "skipped" | "failed"
)
