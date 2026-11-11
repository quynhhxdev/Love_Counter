package com.example.lovecounter.component.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.billing.factory.IapFactoryImpl
import com.ads.admob.data.IapItem
import com.ads.admob.dialog.IapDialog
import com.ads.admob.listener.PurchaseListener
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.language.LanguageFragment
import com.example.lovecounter.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    private val iapFactory = IapFactoryImpl.INSTANCE
    private val purchaseListener = object : PurchaseListener {
        override fun onPurchaseSuccess(productId: String) {
            if (productId == IapFactoryImpl.SKU_NO_ADS) {
                showPurchaseMessage(R.string.iap_purchase_success)
            }
        }

        override fun onPurchaseFailed(errorCode: Int, message: String) {
            showPurchaseMessage(R.string.iap_purchase_failed)
        }
    }

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun initViews() {
        // Banner is owned and managed by MainActivity at the bottom of the screen
        binding.bannerContainer.visibility = View.GONE

        binding.btnBack.setOnClickListener { navigationManager.goBack() }
        binding.btnLanguage.setOnClickListener { navigationManager.navigate(LanguageFragment()) }
        binding.btnRate.setOnClickListener { showToast(getString(R.string.rate_app_clicked)) }
        binding.btnShare.setOnClickListener { showToast(getString(R.string.share_app_clicked)) }
        binding.btnFeedback.setOnClickListener { showToast(getString(R.string.feedback_clicked)) }
        binding.btnPrivacy.setOnClickListener { showToast(getString(R.string.privacy_policy_clicked)) }

    }

    override fun onStart() {
        super.onStart()
        iapFactory.registerPurchaseServiceListener(purchaseListener)
    }

    override fun onStop() {
        iapFactory.unregisterPurchaseServiceListener(purchaseListener)
        super.onStop()
    }


    private fun showIapDialog(item: IapItem) {
        IapDialog(
            context = requireContext(),
            iapItem = item,
            onBuyClicked = {
                activity?.let { host ->
                    iapFactory.buyIap(host, item.productId, item.productType)
                }
            },
            onRestoreClicked = iapFactory::restorePurchases
        ).show()
    }

    private fun showPurchaseMessage(messageRes: Int) {
        activity?.runOnUiThread {
            if (isAdded && view != null) showToast(getString(messageRes))
        }
    }
}
