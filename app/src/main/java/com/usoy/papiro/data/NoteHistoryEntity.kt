package com.usoy.papiro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_history")
data class NoteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val noteId: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
