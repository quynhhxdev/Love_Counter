package com.example.lovecounter.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_avatars")
data class ProfileAvatarEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val maleAvatarPath: String? = null,
    val femaleAvatarPath: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
