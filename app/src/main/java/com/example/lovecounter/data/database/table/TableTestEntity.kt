package com.example.lovecounter.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_test")
data class TableTestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val value: String
)
