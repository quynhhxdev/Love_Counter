package com.example.lovecounter.component.memory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.R
import com.example.lovecounter.component.common.LoveDatePickerDialogFragment
import com.example.lovecounter.component.common.PhotoSelectionHelper
import com.example.lovecounter.data.repositories.ImageStorageRepository
import com.example.lovecounter.data.repositories.MemoryRepository
import com.example.lovecounter.databinding.FragmentAddMemoryBinding
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.DateUtils
import com.example.lovecounter.utils.loadLocalPreview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AddMemoryFragment : Fragment() {

    @Inject
    lateinit var memoryRepository: MemoryRepository

    @Inject
    lateinit var imageStorageRepository: ImageStorageRepository

    private var _binding: FragmentAddMemoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoSelectionHelper: PhotoSelectionHelper
    private var selectedCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoSelectionHelper = PhotoSelectionHelper(
            fragment = this,
            imageStorageRepository = imageStorageRepository,
            onPhotoSelected = { file ->
                renderPhoto(file)
            },
            onError = {
                showPhotoError()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMemoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoSelectionHelper.restoreInstanceState(
            savedInstanceState,
            KEY_PENDING_PHOTO,
            KEY_CAMERA_OUTPUT
        )

        parentFragmentManager.setFragmentResultListener(DATE_PICKER_RESULT, viewLifecycleOwner) { _, result ->
            val dateMillis = result.getLong(LoveDatePickerDialogFragment.RESULT_DATE_MILLIS)
            selectedCalendar.timeInMillis = dateMillis
            binding.tvDateValue.text = DateUtils.formatDate(selectedCalendar.timeInMillis)
        }

        binding.btnBack.setOnClickListener {
            navigationManager.goBack()
        }

        binding.btnDateSelect.setOnClickListener {
            LoveDatePickerDialogFragment.newInstance(DATE_PICKER_RESULT, selectedCalendar.timeInMillis)
                .show(parentFragmentManager, "LoveDatePicker")
        }

        binding.cardAddPhoto.setOnClickListener {
            photoSelectionHelper.showPhotoSourceDialog(layoutInflater)
        }
        photoSelectionHelper.pendingPhoto?.let { renderPhoto(it) }

        binding.btnSaveMemory.setOnClickListener {
            saveMemory()
        }
    }

    private fun saveMemory() {
        val title = binding.etMemoryTitle.text.toString().trim()
        val date = binding.tvDateValue.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_enter_title, Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_select_date, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.btnSaveMemory.isEnabled = false
            try {
                memoryRepository.saveMemory(title, date, photoSelectionHelper.pendingPhoto)
                photoSelectionHelper.clearPendingPhoto()
                navigationManager.goBack()
            } catch (_: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), R.string.failed_to_save_memory, Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.btnSaveMemory?.isEnabled = true
            }
        }
    }

    private fun renderPhoto(file: File) {
        _binding?.let { currentBinding ->
            currentBinding.imgSelectedPhoto.loadLocalPreview(
                imageStorageRepository = imageStorageRepository,
                relativePath = file.absolutePath,
                scope = viewLifecycleOwner.lifecycleScope
            )
            currentBinding.imgSelectedPhoto.visibility = View.VISIBLE
            currentBinding.imgPlusAdd.visibility = View.GONE
        }
    }

    private fun showPhotoError() {
        _binding?.let {
            Toast.makeText(requireContext(), R.string.unable_to_load_photo, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        photoSelectionHelper.saveInstanceState(outState, KEY_PENDING_PHOTO, KEY_CAMERA_OUTPUT)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        photoSelectionHelper.cleanupPendingFiles(activity?.isChangingConfigurations == true)
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DATE_PICKER_RESULT = "add_memory_date_picker_result"
        private const val KEY_PENDING_PHOTO = "pending_memory_photo"
        private const val KEY_CAMERA_OUTPUT = "memory_camera_output"
    }
}
