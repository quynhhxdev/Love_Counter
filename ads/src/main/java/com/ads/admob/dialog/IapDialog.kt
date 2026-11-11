package com.ads.admob.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ads.admob.data.IapItem

class IapDialog(
    context: Context,
    private val iapItem: IapItem? = null,
    private val onBuyClicked: () -> Unit,
    private val onRestoreClicked: (() -> Unit)? = null
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    constructor(context: Context, onBuyClicked: () -> Unit) : this(context, null, onBuyClicked)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layoutRes = context.resources.getIdentifier("dialog_iap", "layout", context.packageName)
        if (layoutRes != 0) {
            setContentView(layoutRes)
        } else {
            val fallbackLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                setBackgroundColor(Color.WHITE)
            }
            setContentView(fallbackLayout)
        }

        window?.let { window ->
            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
            }
            window.attributes = lp
        }

        setCancelable(true)
        setCanceledOnTouchOutside(true)

        bindViews()
    }

    private fun bindViews() {
        val tvTitle = findViewById<TextView>(context.resources.getIdentifier("tvTitle", "id", context.packageName))
        val tvDescription = findViewById<TextView>(context.resources.getIdentifier("tvDescription", "id", context.packageName))
        val tvPrice = findViewById<TextView>(context.resources.getIdentifier("tvPrice", "id", context.packageName))
        val txtContinuePurchase = findViewById<View>(context.resources.getIdentifier("txtContinuePurchase", "id", context.packageName))
            ?: findViewById<Button>(context.resources.getIdentifier("btnBuy", "id", context.packageName))
        val txtRestorePurchase = findViewById<View>(
            context.resources.getIdentifier("txtRestorePurchase", "id", context.packageName)
        )

        iapItem?.let { item ->
            tvTitle?.text = item.title
            tvDescription?.text = item.description
            tvPrice?.text = item.price
        }

        txtContinuePurchase?.setOnClickListener {
            dismiss()
            onBuyClicked()
        }
        txtRestorePurchase?.visibility = if (onRestoreClicked == null) View.GONE else View.VISIBLE
        txtRestorePurchase?.setOnClickListener {
            dismiss()
            onRestoreClicked?.invoke()
        }
    }
}
