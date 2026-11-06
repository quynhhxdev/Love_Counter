package com.example.lovecounter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lovecounter.data.database.table.CustomBackgroundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBackgroundDao {
    @Query("SELECT * FROM custom_backgrounds ORDER BY createdAt DESC")
    fun observeCustomBackgrounds(): Flow<List<CustomBackgroundEntity>>

    @Query("SELECT * FROM custom_backgrounds ORDER BY createdAt DESC")
    suspend fun getAllCustomBackgrounds(): List<CustomBackgroundEntity>

    @Query("SELECT * FROM custom_backgrounds WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedBackground(): CustomBackgroundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackground(background: CustomBackgroundEntity)

    @Update
    suspend fun updateBackground(background: CustomBackgroundEntity)

    @Delete
    suspend fun deleteBackground(background: CustomBackgroundEntity)

    @Query("UPDATE custom_backgrounds SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE custom_backgrounds SET isSelected = 1 WHERE id = :id")
    suspend fun selectBackground(id: String): Int

    @Query("UPDATE custom_backgrounds SET isSelected = 0 WHERE id != :id")
    suspend fun clearSelectionExcept(id: String)
}