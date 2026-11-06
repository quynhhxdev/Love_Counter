package com.example.lovecounter.component.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.example.lovecounter.R
import com.example.lovecounter.databinding.DialogPhotoSourceBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.min

object PhotoSourceDialog {
    fun showPhotoPickerConfirmation(
        context: Context,
        onContinue: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.photo_picker_access_title)
            .setMessage(R.string.photo_picker_access_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_text) { _, _ -> onContinue() }
            .show()
    }

    fun show(
        context: Context,
        layoutInflater: LayoutInflater,
        onTakePhoto: () -> Unit,
        onUploadPhoto: () -> Unit
    ) {
        val dialog = Dialog(context)
        val binding = DialogPhotoSourceBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        binding.optionTakePhoto.setOnClickListener {
            dialog.dismiss()
            onTakePhoto()
        }
        binding.optionUploadPhoto.setOnClickListener {
            dialog.dismiss()
            onUploadPhoto()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        val metrics = context.resources.displayMetrics
        val horizontalInset = (32 * metrics.density).toInt()
        val maxWidth = (420 * metrics.density).toInt()
        val dialogWidth = min(metrics.widthPixels - horizontalInset, maxWidth)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
    }
}
