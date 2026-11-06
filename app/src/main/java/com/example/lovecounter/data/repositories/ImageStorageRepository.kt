package com.example.lovecounter.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class StoredImage(
    val relativePath: String,
    val file: File
)

@Singleton
class ImageStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createCameraCaptureFile(): File {
        val directory = File(context.cacheDir, PENDING_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create camera cache directory")
        }
        return File(directory, "capture_${UUID.randomUUID()}.jpg")
    }

    suspend fun copyUriToPending(uri: Uri): File = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && !mimeType.startsWith("image/")) {
            throw IOException("The selected file is not an image")
        }

        val directory = File(context.cacheDir, PENDING_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create image cache directory")
        }
        val extension = imageExtension(uri, mimeType)
        val destination = File(directory, "selection_${UUID.randomUUID()}.$extension")

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open the selected image")
            input.use { source ->
                destination.outputStream().use { output -> source.copyTo(output) }
            }
            if (!isReadableImage(destination)) {
                throw IOException("The selected file is not a readable image")
            }
            destination
        } catch (exception: Exception) {
            destination.delete()
            throw exception
        }
    }

    suspend fun storePendingImage(
        pendingFile: File,
        directoryName: String,
        filePrefix: String,
        rootDirectory: String = "images"
    ): StoredImage = withContext(Dispatchers.IO) {
        if (!isReadableImage(pendingFile)) {
            throw IOException("The selected image is unavailable or invalid")
        }

        val relativeDirectory = listOf(rootDirectory, directoryName)
            .filter { it.isNotBlank() }
            .joinToString("/")
        val directory = File(context.filesDir, relativeDirectory)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create image storage directory")
        }
        val extension = pendingFile.extension
            .takeIf { it.matches(Regex("[a-zA-Z0-9]{1,5}")) }
            ?: "jpg"
        val fileName = "${filePrefix}_${UUID.randomUUID()}.$extension"
        val destination = File(directory, fileName)

        try {
            FileInputStream(pendingFile).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            if (!destination.isFile || !destination.canRead() || !isReadableImage(destination)) {
                throw IOException("Unable to verify the saved image")
            }
            StoredImage("$relativeDirectory/$fileName", destination)
        } catch (exception: Exception) {
            destination.delete()
            throw exception
        }
    }

    suspend fun loadPreview(file: File): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.isFile || !file.canRead()) return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext null
        val orientation = runCatching { ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        if (matrix.isIdentity) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        }
    }

    suspend fun deletePending(file: File?) = withContext(Dispatchers.IO) {
        file?.delete()
    }

    suspend fun deleteStoredImage(relativePath: String?) = withContext(Dispatchers.IO) {
        if (!relativePath.isNullOrBlank()) File(context.filesDir, relativePath).delete()
    }

    fun resolveStoredImage(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        return File(context.filesDir, relativePath)
    }

    private fun isReadableImage(file: File): Boolean {
        if (!file.isFile || !file.canRead() || file.length() == 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun imageExtension(uri: Uri, mimeType: String?): String {
        val mimeExtension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,5}")) }
        if (mimeExtension != null) return mimeExtension

        val displayName = context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
        return displayName
            ?.substringAfterLast('.', "")
            ?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,5}")) }
            ?: "jpg"
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_PREVIEW_DIMENSION ||
            height / sampleSize > MAX_PREVIEW_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        const val PENDING_DIRECTORY = "pending_images"
        const val MAX_PREVIEW_DIMENSION = 1200
    }
}
