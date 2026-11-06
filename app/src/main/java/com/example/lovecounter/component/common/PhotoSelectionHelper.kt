package com.example.lovecounter.component.common

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.BuildConfig
import com.example.lovecounter.data.repositories.ImageStorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PhotoSelectionHelper(
    private val fragment: Fragment,
    private val imageStorageRepository: ImageStorageRepository,
    private val onPhotoSelected: (File) -> Unit,
    private val onError: (() -> Unit)? = null
) {
    var pendingPhoto: File? = null
        private set
    var cameraOutputFile: File? = null
        private set

    private val pickPhotoLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null && fragment.isAdded) {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val pending = imageStorageRepository.copyUriToPending(uri)
                        setPendingPhoto(pending)
                        onPhotoSelected(pending)
                    } catch (_: Exception) {
                        onError?.invoke()
                    }
                }
            }
        }

    private val takePhotoLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val captureFile = cameraOutputFile
            cameraOutputFile = null
            if (success && captureFile != null && fragment.isAdded) {
                setPendingPhoto(captureFile)
                onPhotoSelected(captureFile)
            } else if (captureFile != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    imageStorageRepository.deletePending(captureFile)
                }
            }
        }

    fun showPhotoSourceDialog(layoutInflater: LayoutInflater) {
        val context = fragment.requireContext()
        PhotoSourceDialog.show(
            context = context,
            layoutInflater = layoutInflater,
            onTakePhoto = {
                var file: File? = null
                try {
                    val capture = imageStorageRepository.createCameraCaptureFile()
                    file = capture
                    cameraOutputFile = capture
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        capture
                    )
                    takePhotoLauncher.launch(uri)
                } catch (_: Exception) {
                    cameraOutputFile = null
                    file?.let { capture ->
                        CoroutineScope(Dispatchers.IO).launch {
                            imageStorageRepository.deletePending(capture)
                        }
                    }
                    onError?.invoke()
                }
            },
            onUploadPhoto = {
                PhotoSourceDialog.showPhotoPickerConfirmation(context) {
                    if (fragment.isAdded) {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
        )
    }

    fun setPendingPhoto(file: File?) {
        val previous = pendingPhoto
        pendingPhoto = file
        if (previous != null && previous != file) {
            CoroutineScope(Dispatchers.IO).launch {
                imageStorageRepository.deletePending(previous)
            }
        }
    }

    fun clearPendingPhoto() {
        setPendingPhoto(null)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?, keyPending: String, keyCamera: String) {
        pendingPhoto = savedInstanceState
            ?.getString(keyPending)
            ?.let(::File)
            ?.takeIf { it.isFile }
        cameraOutputFile = savedInstanceState
            ?.getString(keyCamera)
            ?.let(::File)
    }

    fun saveInstanceState(outState: Bundle, keyPending: String, keyCamera: String) {
        outState.putString(keyPending, pendingPhoto?.absolutePath)
        outState.putString(keyCamera, cameraOutputFile?.absolutePath)
    }

    fun cleanupPendingFiles(isChangingConfigurations: Boolean) {
        if (!isChangingConfigurations) {
            val pending = listOfNotNull(pendingPhoto, cameraOutputFile)
            CoroutineScope(Dispatchers.IO).launch {
                pending.forEach { imageStorageRepository.deletePending(it) }
            }
        }
    }
}
