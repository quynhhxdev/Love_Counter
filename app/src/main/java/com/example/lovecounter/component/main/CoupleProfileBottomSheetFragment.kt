package com.example.lovecounter.component.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovecounter.R
import com.example.lovecounter.component.common.GenderPopupMenu
import com.example.lovecounter.component.common.LoveDatePickerDialogFragment
import com.example.lovecounter.component.common.PhotoSelectionHelper
import com.example.lovecounter.data.repositories.ImageStorageRepository
import com.example.lovecounter.databinding.DialogCoupleProfileBinding
import com.example.lovecounter.utils.loadLocalPreview
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CoupleProfileBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: CoupleProfileViewModel by viewModels()

    @Inject
    lateinit var imageStorageRepository: ImageStorageRepository

    private var _binding: DialogCoupleProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoSelectionHelper: PhotoSelectionHelper
    private var currentTabIsMale = true
    private var lastRenderedAvatarPath: String? = null
    private var hasRenderedAvatarPath = false
    var onProfileSavedListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoSelectionHelper = PhotoSelectionHelper(
            fragment = this,
            imageStorageRepository = imageStorageRepository,
            onPhotoSelected = { file ->
                renderPendingAvatar(file)
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
        _binding = DialogCoupleProfileBinding.inflate(inflater, container, false)
        hasRenderedAvatarPath = false
        photoSelectionHelper.restoreInstanceState(
            savedInstanceState,
            KEY_PENDING_AVATAR,
            KEY_CAMERA_OUTPUT
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentTabIsMale = arguments?.getBoolean(KEY_IS_MALE, true) ?: true
        viewModel.init(currentTabIsMale)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        binding.imgAvatar.setImageResource(defaultAvatarResource())
        photoSelectionHelper.pendingPhoto?.let { renderPendingAvatar(it) }
    }

    private fun setupListeners() {
        binding.etName.addTextChangedListener { text ->
            val name = text?.toString() ?: ""
            viewModel.onNameChanged(name)
        }

        binding.btnGenderSelect.setOnClickListener {
            showGenderPopupMenu(it)
        }

        binding.btnBirthdaySelect.setOnClickListener {
            showLoveDatePicker()
        }

        binding.imgAvatar.setOnClickListener { photoSelectionHelper.showPhotoSourceDialog(layoutInflater) }
        binding.tvChangePhoto.setOnClickListener { photoSelectionHelper.showPhotoSourceDialog(layoutInflater) }

        binding.btnSave.setOnClickListener {
            viewModel.saveProfile(photoSelectionHelper.pendingPhoto)
        }

        childFragmentManager.setFragmentResultListener(DATE_PICKER_RESULT, viewLifecycleOwner) { _, result ->
            val dateMillis = result.getLong(LoveDatePickerDialogFragment.RESULT_DATE_MILLIS)
            viewModel.onBirthdaySelectedMillis(dateMillis)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        val active = state.activeProfile
                        if (binding.etName.text.toString() != active.name) {
                            binding.etName.setText(active.name)
                            binding.etName.setSelection(active.name.length)
                        }
                        binding.tvGenderValue.text = when (active.gender.uppercase()) {
                            "FEMALE" -> getString(R.string.female)
                            "OTHER" -> getString(R.string.other)
                            else -> getString(R.string.male)
                        }
                        binding.tvBirthdayValue.text = active.dateOfBirth

                        val avatarPath = state.activeAvatarPath
                        if (photoSelectionHelper.pendingPhoto == null &&
                            (!hasRenderedAvatarPath || lastRenderedAvatarPath != avatarPath)
                        ) {
                            hasRenderedAvatarPath = true
                            lastRenderedAvatarPath = avatarPath
                            displaySavedAvatar(avatarPath)
                        }

                        if (state.nameErrorRes != null) {
                            binding.tvErrorName.setText(state.nameErrorRes)
                            binding.tvErrorName.visibility = View.VISIBLE
                        } else {
                            binding.tvErrorName.visibility = View.GONE
                        }

                        if (state.birthdayError != null) {
                            binding.tvErrorBirthday.text = state.birthdayError
                            binding.tvErrorBirthday.visibility = View.VISIBLE
                        } else {
                            binding.tvErrorBirthday.visibility = View.GONE
                        }

                        binding.btnSave.isEnabled = !state.isSaving
                    }
                }

                launch {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is CoupleProfileEvent.SaveSuccess -> {
                                photoSelectionHelper.clearPendingPhoto()
                                onProfileSavedListener?.invoke()
                                dismiss()
                            }
                            is CoupleProfileEvent.ShowToast -> {
                                val message = event.formatArg?.let { getString(event.messageRes, it) }
                                    ?: getString(event.messageRes)
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showGenderPopupMenu(anchorView: View) {
        GenderPopupMenu.show(
            context = requireContext(),
            anchorView = anchorView,
            layoutInflater = layoutInflater,
            currentGender = viewModel.uiState.value.activeProfile.gender,
            onGenderSelected = { selected -> viewModel.onGenderChanged(selected) }
        )
    }

    private fun renderPendingAvatar(file: File) {
        _binding?.imgAvatar?.loadLocalPreview(
            imageStorageRepository = imageStorageRepository,
            relativePath = file.absolutePath,
            scope = viewLifecycleOwner.lifecycleScope,
            placeholderRes = defaultAvatarResource()
        )
    }

    private fun displaySavedAvatar(relativePath: String?) {
        if (photoSelectionHelper.pendingPhoto != null) return
        _binding?.imgAvatar?.loadLocalPreview(
            imageStorageRepository = imageStorageRepository,
            relativePath = relativePath,
            scope = viewLifecycleOwner.lifecycleScope,
            placeholderRes = defaultAvatarResource()
        )
    }

    private fun defaultAvatarResource(): Int =
        if (currentTabIsMale) R.drawable.ic_avt_boy else R.drawable.ic_avt_girl

    private fun showPhotoError() {
        _binding?.let {
            Toast.makeText(requireContext(), R.string.unable_to_load_photo, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        photoSelectionHelper.saveInstanceState(outState, KEY_PENDING_AVATAR, KEY_CAMERA_OUTPUT)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        photoSelectionHelper.cleanupPendingFiles(activity?.isChangingConfigurations == true)
        super.onDestroy()
    }

    private fun showLoveDatePicker() {
        LoveDatePickerDialogFragment
            .newInstance(DATE_PICKER_RESULT, viewModel.getInitialBirthdayMillis())
            .show(childFragmentManager, "LoveDatePicker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_IS_MALE = "KEY_IS_MALE"
        private const val DATE_PICKER_RESULT = "birthday_date_picker_result"
        private const val KEY_PENDING_AVATAR = "pending_profile_avatar"
        private const val KEY_CAMERA_OUTPUT = "profile_camera_output"

        fun newInstance(isMale: Boolean = true): CoupleProfileBottomSheetFragment {
            return CoupleProfileBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(KEY_IS_MALE, isMale)
                }
            }
        }
    }
}
