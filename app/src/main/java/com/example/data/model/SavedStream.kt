package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_streams")
data class SavedStream(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val url: String,
    val category: String = "Live",
    val addedTimestamp: Long = System.currentTimeMillis()
)
