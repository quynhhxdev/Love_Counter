package com.example.lovecounter.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_memory")
data class TableMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val date: String,
    val imagePath: String = ""
)