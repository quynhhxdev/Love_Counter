package com.example.lovecounter.base.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewbinding.ViewBinding
import com.ads.admob.dialog.LoadingAdsDialog
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.InterAdsUtils
import com.example.lovecounter.utils.ads.RewardAdsUtils
import kotlinx.coroutines.runBlocking

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    val binding: VB get() = _binding!!

    private var loadingDialog: LoadingAdsDialog? = null

    abstract fun provideViewBinding(inflater: LayoutInflater): VB

    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}

    open fun onBack() {
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = try {
            runBlocking { CoupleProfileDataStore.readLanguageCode(newBase) }
        } catch (_: Exception) {
            "en"
        }
        val context = LocaleHelper.onAttach(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = provideViewBinding(layoutInflater)
        setContentView(binding.root)

        setupBackPressed()
        initViews()
        initData()
        initObserver()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })
    }

    // Fragment Helpers
    fun replaceFragment(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = fragment.javaClass.simpleName
    ) {
        supportFragmentManager.commit {
            replace(containerId, fragment, tag)
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }
    }

    fun addFragment(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = fragment.javaClass.simpleName
    ) {
        supportFragmentManager.commit {
            add(containerId, fragment, tag)
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }
    }

    fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            show(fragment)
        }
    }

    fun hideFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            hide(fragment)
        }
    }

    // Edge-to-Edge & Fullscreen Helpers
    fun setFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // UI & Ads Helpers
    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showLoading() {
        if (isFinishing || isDestroyed) return
        if (loadingDialog == null) {
            loadingDialog = LoadingAdsDialog(this)
        }
        loadingDialog?.show()
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
            loadingDialog = null
        } catch (_: Exception) {
        }
    }

    fun showInterAd(adId: String = AdPlacement.TEST_INTERSTITIAL_ID, onAdClosed: () -> Unit) {
        InterAdsUtils.showInterAd(this, onAdClosed)
    }

    fun showRewardAd(onRewardGranted: () -> Unit) {
        RewardAdsUtils.showRewardAd(this, onRewardGranted)
    }

    override fun onDestroy() {
        hideLoading()
        _binding = null
        super.onDestroy()
    }
}
