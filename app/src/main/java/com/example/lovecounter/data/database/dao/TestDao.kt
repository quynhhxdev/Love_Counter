package com.example.lovecounter.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lovecounter.data.database.table.TableTestEntity

@Dao
interface TestDao {
    @Query("SELECT * FROM table_test")
    suspend fun getAll(): List<TableTestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TableTestEntity>)

    @Query("DELETE FROM table_test")
    suspend fun deleteAll()
}
