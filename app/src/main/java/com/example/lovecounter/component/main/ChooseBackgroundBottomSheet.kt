package com.example.lovecounter.component.main

import android.net.Uri
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lovecounter.R
import com.example.lovecounter.base.adapter.BaseAdapter
import com.example.lovecounter.component.common.PhotoSourceDialog
import com.example.lovecounter.data.database.table.CustomBackgroundEntity
import com.example.lovecounter.data.repositories.BackgroundRepository
import com.example.lovecounter.databinding.DialogChooseBackgroundBinding
import com.example.lovecounter.databinding.ItemBackgroundOptionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

fun List<CustomBackgroundEntity>.toBackgroundOptions(): List<ChooseBackgroundBottomSheet.BackgroundOption> {
    val hasSelectedCustom = any { it.isSelected }
    val list = mutableListOf<ChooseBackgroundBottomSheet.BackgroundOption>()
    list.add(
        ChooseBackgroundBottomSheet.BackgroundOption(
            isAddOption = true,
            isCustom = false,
            drawableRes = 0,
            filePath = null
        )
    )
    list.add(
        ChooseBackgroundBottomSheet.BackgroundOption(
            isAddOption = false,
            isCustom = false,
            drawableRes = R.drawable.bg_top_home,
            filePath = null,
            isSelected = !hasSelectedCustom
        )
    )
    forEach { entity ->
        list.add(
            ChooseBackgroundBottomSheet.BackgroundOption(
                isAddOption = false,
                isCustom = true,
                drawableRes = 0,
                filePath = entity.localPath,
                id = entity.id,
                isSelected = entity.isSelected
            )
        )
    }
    return list
}

@AndroidEntryPoint
class ChooseBackgroundBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var backgroundRepository: BackgroundRepository

    private var _binding: DialogChooseBackgroundBinding? = null
    private val binding get() = _binding!!

    var onBackgroundSelectedListener: ((Any) -> Unit)? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            importBackground(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChooseBackgroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var currentEntities = emptyList<CustomBackgroundEntity>()
        val adapter = BackgroundAdapter(
            onAddClick = {
                showPhotoAccessConfirmation()
            },
            onSelectClick = { option ->
                lifecycleScope.launch {
                    try {
                        val selected = if (option.isCustom && option.id != null) {
                            backgroundRepository.selectBackground(
                                currentEntities.firstOrNull { it.id == option.id }
                                    ?: return@launch
                            )
                        } else {
                            backgroundRepository.selectDefaultBackground()
                            true
                        }
                        if (selected) {
                            onBackgroundSelectedListener?.invoke(
                                if (option.isCustom) option.filePath!! else option.drawableRes
                            )
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), R.string.background_unavailable, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), R.string.failed_to_select_background, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvBackgrounds.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBackgrounds.adapter = adapter

        lifecycleScope.launch {
            backgroundRepository.observeCustomBackgrounds().collectLatest { customEntities ->
                currentEntities = customEntities
                adapter.submitList(customEntities.toBackgroundOptions())
            }
        }
    }

    private fun showPhotoAccessConfirmation() {
        PhotoSourceDialog.showPhotoPickerConfirmation(requireContext()) {
            if (isAdded) {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }

    private fun importBackground(uri: Uri) {
        lifecycleScope.launch {
            backgroundRepository.importAndSaveBackground(uri)
                .onSuccess { entity ->
                    Toast.makeText(requireContext(), R.string.background_added, Toast.LENGTH_SHORT).show()
                    onBackgroundSelectedListener?.invoke(entity.localPath)
                    dismiss()
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.failed_to_import_image, Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class BackgroundOption(
        val isAddOption: Boolean,
        val isCustom: Boolean,
        val drawableRes: Int,
        val filePath: String?,
        val id: String? = null,
        val isSelected: Boolean = false
    )

    class BackgroundAdapter(
        private val onAddClick: () -> Unit,
        private val onSelectClick: (BackgroundOption) -> Unit
    ) : BaseAdapter<BackgroundOption, ItemBackgroundOptionBinding>() {

        override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemBackgroundOptionBinding {
            return ItemBackgroundOptionBinding.inflate(inflater, parent, false)
        }

        override fun bind(binding: ItemBackgroundOptionBinding, item: BackgroundOption, position: Int) {
            val density = binding.root.resources.displayMetrics.density
            if (item.isAddOption) {
                binding.imgBgOption.setImageDrawable(null)
                binding.imgAddIcon.visibility = View.VISIBLE
                binding.imgAddIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                binding.cardBackground.setCardBackgroundColor(Color.parseColor("#777780"))
                binding.cardBackground.strokeWidth = 0
                binding.cardBackground.foreground = null
                binding.root.setOnClickListener { onAddClick() }
            } else {
                binding.imgAddIcon.visibility = View.GONE
                binding.cardBackground.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.white))
                binding.cardBackground.strokeWidth = (1 * density).toInt()
                binding.cardBackground.setStrokeColor(Color.parseColor("#777780"))
                if (item.isCustom && item.filePath != null) {
                    val file = File(binding.root.context.filesDir, item.filePath)
                    if (file.isFile && file.canRead()) {
                        binding.imgBgOption.setImageURI(Uri.fromFile(file))
                    } else {
                        binding.imgBgOption.setImageResource(R.drawable.bg_top_home)
                    }
                } else {
                    binding.imgBgOption.setImageResource(item.drawableRes)
                }
                binding.cardBackground.foreground = if (item.isSelected) {
                    GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = 14 * density
                        setStroke(
                            (3 * density).toInt(),
                            ContextCompat.getColor(binding.root.context, R.color.primary_pink)
                        )
                    }
                } else {
                    null
                }
                binding.root.setOnClickListener { onSelectClick(item) }
            }
        }
    }
}
