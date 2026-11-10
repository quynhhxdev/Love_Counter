package com.ads.admob.helper.interstitial

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdShowCallBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class InterstitialAdSplashHelper(private val config: InterstitialAdSplashConfig) :
    AdsHelper<InterstitialAdParam>() {
    constructor(config: InterstitialAdConfig) : this(
        InterstitialAdSplashConfig(
            param = config.param,
            showReady = true,
            holdShow = true,
            adPlacement = config.adPlacement
        )
    )

    private val helper = InterstitialAdsHelper.getInstance(
        InterstitialAdConfig(
            placementKey = config.adPlacement,
            param = config.param.copy(adPlacement = config.adPlacement),
            showByTime = 0L,
            canReloadAds = false,
            adPlacement = config.adPlacement
        )
    )

    fun loadAndShowSplashAd(activity: Activity, onFinished: () -> Unit) =
        loadAndShowSplashAd(activity, config.timeOut, onFinished)

    fun loadAndShowSplashAd(
        activity: Activity,
        timeoutMs: Long,
        onFinished: () -> Unit
    ) {
        require(timeoutMs >= 0L) { "Splash interstitial timeout must not be negative" }
        if (!canRequestAds(activity) || !FirebaseConfigManager.isInterstitialAdsEnabled()) {
            onFinished()
            return
        }

        val finished = AtomicBoolean(false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var timeoutJob: kotlinx.coroutines.Job? = null

        fun finish() {
            if (finished.compareAndSet(false, true)) {
                timeoutJob?.cancel()
                onFinished()
                scope.cancel()
            }
        }

        timeoutJob = scope.launch(start = CoroutineStart.LAZY) {
            delay(timeoutMs)
            finish()
        }

        helper.loadAd(activity) { loaded ->
            if (finished.get()) return@loadAd
            if (!config.holdShow) {
                finish()
                return@loadAd
            }
            if (!loaded || !config.showReady) return@loadAd

            scope.launch {
                delay(config.timeDelay)
                if (finished.get()) return@launch
                timeoutJob?.cancel()
                val callback = object : InterstitialAdShowCallBack {
                    override fun onAdShowed() = Unit
                    override fun onAdDismissed() = finish()
                    override fun onAdFailedToShow(error: String) = finish()
                }
                val lifecycleOwner = activity as? LifecycleOwner
                if (lifecycleOwner != null) {
                    helper.forceShowInterstitial(activity, lifecycleOwner, callback)
                } else {
                    helper.showAd(activity, callback)
                }
            }
        }

        if (!config.holdShow) {
            finish()
        } else if (!finished.get()) {
            timeoutJob?.start()
        }
    }
}
