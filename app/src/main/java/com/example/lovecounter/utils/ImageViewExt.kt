package com.example.lovecounter.utils

import android.widget.ImageView
import com.example.lovecounter.data.repositories.ImageStorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

fun ImageView.loadLocalPreview(
    imageStorageRepository: ImageStorageRepository,
    relativePath: String?,
    scope: CoroutineScope,
    placeholderRes: Int? = null
) {
    tag = relativePath
    if (placeholderRes != null) {
        setImageResource(placeholderRes)
    }
    if (relativePath.isNullOrBlank()) {
        return
    }
    val file = if (File(relativePath).isAbsolute) {
        File(relativePath).takeIf { it.isFile && it.canRead() }
    } else {
        imageStorageRepository.resolveStoredImage(relativePath)?.takeIf { it.isFile && it.canRead() }
    }
    if (file == null) {
        if (placeholderRes != null) {
            setImageResource(placeholderRes)
        }
        return
    }
    scope.launch {
        val bitmap = imageStorageRepository.loadPreview(file)
        if (tag == relativePath) {
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else if (placeholderRes != null) {
                setImageResource(placeholderRes)
            }
        }
    }
}
