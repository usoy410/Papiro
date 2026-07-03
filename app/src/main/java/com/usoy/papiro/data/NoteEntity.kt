package com.usoy.papiro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val backgroundType: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
