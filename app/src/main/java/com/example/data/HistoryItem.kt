package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalText: String,
    val reshapedText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
