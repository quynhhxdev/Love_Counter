package com.example.lovecounter.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lovecounter.data.database.table.ProfileAvatarEntity

@Dao
interface ProfileAvatarDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRow(entity: ProfileAvatarEntity)

    @Query("SELECT * FROM profile_avatars WHERE id = :id")
    suspend fun getAvatarPaths(id: Int = ProfileAvatarEntity.SINGLETON_ID): ProfileAvatarEntity?

    @Query("UPDATE profile_avatars SET maleAvatarPath = :path WHERE id = :id")
    suspend fun updateMaleAvatar(path: String, id: Int = ProfileAvatarEntity.SINGLETON_ID)

    @Query("UPDATE profile_avatars SET femaleAvatarPath = :path WHERE id = :id")
    suspend fun updateFemaleAvatar(path: String, id: Int = ProfileAvatarEntity.SINGLETON_ID)
}
