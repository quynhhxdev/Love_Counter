package com.example.lovecounter.component.splash

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.example.lovecounter.R
import com.example.lovecounter.App
import com.example.lovecounter.component.bootstrap.BootstrapFragment
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.FirebaseConfigManager
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.InterAdsUtils
import com.example.lovecounter.utils.ads.NativeAdsUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var hasNavigated = false
    private var splashProgressAnimator: ValueAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startSplashProgressAnimation(view)

        // Vô hiệu hóa và clear Ads Resume trên màn hình Splash để tránh xung đột với Interstitial Ad
        App.appResumeAdHelper?.clearAd()

        ConsentManager.gatherConsent(
            activity = requireActivity(),
            isTestDevice = true,
            callback = object : OnConsentResponse {
                override fun onConsentGathered(canRequestAds: Boolean) {
                    if (!isAdded) return
                    val adUnitId = AdPlacement.INTER_SPLASH_ID

                    viewLifecycleOwner.lifecycleScope.launch {
                        if (canRequestAds) {
                            InterAdsUtils.loadInterAd(requireActivity(), adUnitId)
                            if (FirebaseConfigManager.isNativeAdsEnabled()) {
                                NativeAdsUtils.preloadNative(
                                    requireContext().applicationContext,
                                    AdPlacement.LANGUAGE_NATIVE,
                                    AdPlacement.NATIVE_LANGUAGE_ID
                                )
                            }
                        }

                        delay(3500L)
                        showAdOrProceed()
                    }
                }
            }
        )
    }

    private fun startSplashProgressAnimation(view: View) {
        val progress = view.findViewById<LinearLayout>(R.id.splashProgress)
        if (progress.childCount == 0) return

        splashProgressAnimator = ValueAnimator.ofInt(0, progress.childCount).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val activeIndex = (animator.animatedValue as Int).coerceAtMost(progress.childCount - 1)
                for (index in 0 until progress.childCount) {
                    progress.getChildAt(index).setBackgroundResource(
                        if (index == activeIndex) R.drawable.bg_indicator_active
                        else R.drawable.bg_indicator_inactive
                    )
                }
            }
            start()
        }
    }

    override fun onDestroyView() {
        splashProgressAnimator?.cancel()
        splashProgressAnimator = null
        super.onDestroyView()
    }

    private fun showAdOrProceed() {
        if (hasNavigated) return
        hasNavigated = true

        InterAdsUtils.showInterAd(requireActivity()) {
            navigateToNext()
        }
    }

    private fun navigateToNext() {
        if (isAdded) {
            navigationManager.replaceRoot(BootstrapFragment())
        }
    }
}
