package com.example.lovecounter.data.repositories

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.lovecounter.data.database.AppDatabase
import com.example.lovecounter.data.database.table.CustomBackgroundEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val imageStorageRepository: ImageStorageRepository
) {

    fun observeCustomBackgrounds(): Flow<List<CustomBackgroundEntity>> =
        appDatabase.customBackgroundDao().observeCustomBackgrounds()

    suspend fun getSelectedBackground(): CustomBackgroundEntity? =
        appDatabase.customBackgroundDao().getSelectedBackground()

    suspend fun importAndSaveBackground(uri: Uri): Result<CustomBackgroundEntity> = withContext(Dispatchers.IO) {
        var pendingFile: File? = null
        var storedImage: StoredImage? = null
        var savedToDatabase = false
        try {
            val pending = imageStorageRepository.copyUriToPending(uri)
            pendingFile = pending

            val pendingHash = calculateFileHash(pending)
            val existingBackgrounds = appDatabase.customBackgroundDao().getAllCustomBackgrounds()
            for (existing in existingBackgrounds) {
                val existingFile = File(context.filesDir, existing.localPath)
                if (existingFile.isFile && existingFile.canRead()) {
                    val existingHash = calculateFileHash(existingFile)
                    if (pendingHash == existingHash) {
                        imageStorageRepository.deletePending(pending)
                        appDatabase.withTransaction {
                            appDatabase.customBackgroundDao().clearSelection()
                            appDatabase.customBackgroundDao().selectBackground(existing.id)
                        }
                        return@withContext Result.success(existing)
                    }
                }
            }

            val stored = imageStorageRepository.storePendingImage(
                pendingFile = pending,
                directoryName = "backgrounds",
                filePrefix = "background",
                rootDirectory = ""
            )
            storedImage = stored
            val entity = CustomBackgroundEntity(
                id = UUID.randomUUID().toString(),
                name = "Custom Background",
                localPath = stored.relativePath,
                createdAt = System.currentTimeMillis(),
                isSelected = true
            )
            appDatabase.withTransaction {
                appDatabase.customBackgroundDao().clearSelection()
                appDatabase.customBackgroundDao().insertBackground(entity)
            }
            savedToDatabase = true
            imageStorageRepository.deletePending(pending)
            Result.success(entity)
        } catch (e: Exception) {
            if (!savedToDatabase) {
                storedImage?.let { imageStorageRepository.deleteStoredImage(it.relativePath) }
            }
            imageStorageRepository.deletePending(pendingFile)
            Result.failure(e)
        }
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun selectBackground(background: CustomBackgroundEntity): Boolean =
        withContext(Dispatchers.IO) {
            appDatabase.withTransaction {
                val dao = appDatabase.customBackgroundDao()
                val found = dao.selectBackground(background.id) > 0
                if (found) dao.clearSelectionExcept(background.id)
                found
            }
        }

    suspend fun selectDefaultBackground() = withContext(Dispatchers.IO) {
        appDatabase.withTransaction {
            appDatabase.customBackgroundDao().clearSelection()
        }
    }

    fun getBackgroundFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }
}
