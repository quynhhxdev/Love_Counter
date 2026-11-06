package com.example.lovecounter.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lovecounter.data.database.dao.CustomBackgroundDao
import com.example.lovecounter.data.database.dao.MemoryDao
import com.example.lovecounter.data.database.dao.ProfileAvatarDao
import com.example.lovecounter.data.database.dao.TestDao
import com.example.lovecounter.data.database.table.CustomBackgroundEntity
import com.example.lovecounter.data.database.table.ProfileAvatarEntity
import com.example.lovecounter.data.database.table.TableMemoryEntity
import com.example.lovecounter.data.database.table.TableTestEntity

@Database(
    entities = [
        TableTestEntity::class,
        TableMemoryEntity::class,
        CustomBackgroundEntity::class,
        ProfileAvatarEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testDao(): TestDao
    abstract fun memoryDao(): MemoryDao
    abstract fun customBackgroundDao(): CustomBackgroundDao
    abstract fun profileAvatarDao(): ProfileAvatarDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profile_avatars` (
                        `id` INTEGER NOT NULL,
                        `maleAvatarPath` TEXT,
                        `femaleAvatarPath` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}