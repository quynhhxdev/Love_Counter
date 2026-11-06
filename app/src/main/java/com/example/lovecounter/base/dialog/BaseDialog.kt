package com.example.lovecounter.base.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.viewbinding.ViewBinding

abstract class BaseDialog<VB : ViewBinding>(
    context: Context,
    themeResId: Int = 0
) : Dialog(context, themeResId) {

    private var _binding: VB? = null
    val binding: VB get() = _binding!!

    abstract fun provideViewBinding(): VB

    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        _binding = provideViewBinding()
        setContentView(binding.root)

        window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        initViews()
        initData()
        initObserver()
    }
}
