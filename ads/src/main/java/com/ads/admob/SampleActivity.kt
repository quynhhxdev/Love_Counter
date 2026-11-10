package com.ads.admob

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.billing.factory.IapFactoryImpl
import kotlinx.coroutines.launch

class SampleActivity : AppCompatActivity() {

    private val iapFactory = IapFactoryImpl.INSTANCE
    private lateinit var purchaseStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        purchaseStatus = TextView(this).apply {
            text = getString(R.string.sample_iap_waiting)
            textSize = 16f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(
                TextView(this@SampleActivity).apply {
                    text = getString(R.string.sample_iap_title)
                    textSize = 22f
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                purchaseStatus,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = padding
                }
            )
            addView(
                Button(this@SampleActivity).apply {
                    text = getString(R.string.sample_iap_check_purchases)
                    setOnClickListener { iapFactory.checkPurchases() }
                }
            )
            addView(
                Button(this@SampleActivity).apply {
                    text = getString(R.string.sample_iap_restore_purchases)
                    setOnClickListener { iapFactory.restorePurchases() }
                }
            )
            addView(
                TextView(this@SampleActivity).apply {
                    text = getString(R.string.sample_iap_note)
                    textSize = 13f
                }
            )
        }
        setContentView(content)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                iapFactory.isNoAdsPurchased.collect { purchased ->
                    purchaseStatus.text = getString(
                        R.string.sample_iap_purchase_status,
                        getString(if (purchased) R.string.sample_iap_yes else R.string.sample_iap_no)
                    )
                }
            }
        }

        try {
            iapFactory.initBilling(applicationContext)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to initialize billing in SampleActivity", error)
            purchaseStatus.text = getString(R.string.sample_iap_initialization_failed, error.message.orEmpty())
        }
    }

    private companion object {
        const val TAG = "SampleActivity"
    }
}
