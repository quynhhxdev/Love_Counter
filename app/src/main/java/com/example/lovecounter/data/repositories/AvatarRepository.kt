package com.example.lovecounter.data.repositories

import androidx.room.withTransaction
import com.example.lovecounter.data.database.AppDatabase
import com.example.lovecounter.data.database.table.ProfileAvatarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val imageStorageRepository: ImageStorageRepository
) {
    suspend fun getAvatarPaths(): ProfileAvatarEntity? = appDatabase.profileAvatarDao().getAvatarPaths()

    suspend fun saveAvatar(isMale: Boolean, pendingFile: File): String = withContext(Dispatchers.IO) {
        val storedImage = imageStorageRepository.storePendingImage(
            pendingFile = pendingFile,
            directoryName = "avatars",
            filePrefix = if (isMale) "male" else "female"
        )

        val (oldPath, newPaths) = try {
            appDatabase.withTransaction {
                val dao = appDatabase.profileAvatarDao()
                dao.ensureRow(ProfileAvatarEntity())
                val previous = dao.getAvatarPaths() ?: ProfileAvatarEntity()
                if (isMale) {
                    dao.updateMaleAvatar(storedImage.relativePath)
                } else {
                    dao.updateFemaleAvatar(storedImage.relativePath)
                }
                val updated = dao.getAvatarPaths() ?: ProfileAvatarEntity()
                val oldPath = if (isMale) previous.maleAvatarPath else previous.femaleAvatarPath
                oldPath to listOfNotNull(updated.maleAvatarPath, updated.femaleAvatarPath)
            }
        } catch (exception: Exception) {
            imageStorageRepository.deleteStoredImage(storedImage.relativePath)
            throw exception
        }

        if (oldPath != null && oldPath !in newPaths) {
            imageStorageRepository.deleteStoredImage(oldPath)
        }
        imageStorageRepository.deletePending(pendingFile)
        storedImage.relativePath
    }
}
