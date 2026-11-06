package com.example.lovecounter.data.repositories

import androidx.room.withTransaction
import com.example.lovecounter.data.database.AppDatabase
import com.example.lovecounter.data.database.table.TableMemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val imageStorageRepository: ImageStorageRepository
) {
    fun getAllMemoriesFlow(): Flow<List<TableMemoryEntity>> =
        appDatabase.memoryDao().getAllMemoriesFlow()

    suspend fun deleteMemory(memory: TableMemoryEntity) = withContext(Dispatchers.IO) {
        appDatabase.withTransaction {
            appDatabase.memoryDao().deleteMemory(memory)
        }
        if (memory.imagePath.isNotBlank() &&
            !appDatabase.memoryDao().isImagePathReferenced(memory.imagePath)
        ) {
            imageStorageRepository.deleteStoredImage(memory.imagePath)
        }
    }

    suspend fun saveMemory(title: String, date: String, pendingPhoto: File?): Long =
        withContext(Dispatchers.IO) {
            val storedImage = pendingPhoto?.let {
                imageStorageRepository.storePendingImage(it, "memories", "memory")
            }
            val id = try {
                appDatabase.withTransaction {
                    appDatabase.memoryDao().insertMemory(
                        TableMemoryEntity(
                            title = title,
                            date = date,
                            imagePath = storedImage?.relativePath.orEmpty()
                        )
                    )
                }
            } catch (exception: Exception) {
                storedImage?.let { imageStorageRepository.deleteStoredImage(it.relativePath) }
                throw exception
            }
            if (pendingPhoto != null) imageStorageRepository.deletePending(pendingPhoto)
            id
        }
}
