package com.ads.admob.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager

class LoadingAdsDialog(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (isShowing) {
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layoutRes = context.resources.getIdentifier("dialog_love_test_loading", "layout", context.packageName)
        if (layoutRes != 0) {
            setContentView(layoutRes)
        }

        window?.let { window ->
            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
            window.attributes = lp
        }

        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    override fun show() {
        super.show()
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, 6000L)
    }

    override fun dismiss() {
        handler.removeCallbacks(timeoutRunnable)
        try {
            if (isShowing) {
                super.dismiss()
            }
        } catch (_: Exception) {
        }
    }
}
