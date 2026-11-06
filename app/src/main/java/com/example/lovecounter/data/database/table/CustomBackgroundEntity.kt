package com.example.lovecounter.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_backgrounds")
data class CustomBackgroundEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val localPath: String,
    val createdAt: Long,
    val isSelected: Boolean = false
)