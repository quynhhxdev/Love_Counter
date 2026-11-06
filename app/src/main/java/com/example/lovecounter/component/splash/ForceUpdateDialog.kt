package com.example.lovecounter.component.splash

import android.content.Context
import com.example.lovecounter.base.dialog.BaseDialog
import com.example.lovecounter.databinding.DialogForceUpdateBinding

class ForceUpdateDialog(
    context: Context,
    private val onUpdateClick: () -> Unit
) : BaseDialog<DialogForceUpdateBinding>(context) {

    override fun provideViewBinding() = DialogForceUpdateBinding.inflate(layoutInflater)

    override fun initViews() {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        binding.btnUpdate.setOnClickListener {
            onUpdateClick()
            dismiss()
        }
    }
}
