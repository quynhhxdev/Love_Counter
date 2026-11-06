package com.example.lovecounter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lovecounter.data.database.table.TableMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM table_memory ORDER BY id DESC")
    fun getAllMemoriesFlow(): Flow<List<TableMemoryEntity>>

    @Query("SELECT * FROM table_memory ORDER BY id DESC")
    suspend fun getAllMemories(): List<TableMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: TableMemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: TableMemoryEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM table_memory WHERE imagePath = :path)")
    suspend fun isImagePathReferenced(path: String): Boolean
}