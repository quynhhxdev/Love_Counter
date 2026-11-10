package com.ads.admob.helper.interstitial

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.AdmobManager
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.interstitial.factory.admob.AdmobInterstitialAdFactoryImpl
import com.ads.admob.helper.interstitial.params.AdInterstitialState
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InterstitialAdsHelper private constructor(private val config: InterstitialAdConfig) :
    AdsHelper<InterstitialAdParam>() {
    private val tag = "InterstitialAdsHelper"
    private val factory = AdmobInterstitialAdFactoryImpl()
    private val mutableState = MutableStateFlow(AdInterstitialState.NONE)
    val state: StateFlow<AdInterstitialState> = mutableState

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var requestGeneration = 0L
    private var lastShownTimestamp = 0L
    private var loadingDialog: LoadingAdsDialog? = null
    private val loadCallbacks = mutableListOf<(Boolean) -> Unit>()

    fun loadAd(activity: Activity, onLoaded: ((Boolean) -> Unit)? = null) {
        canReloadAd = config.canReloadAds
        if (!canRequestAds(activity, config.canShowAds) ||
            !FirebaseConfigManager.isInterstitialAdsEnabled()
        ) {
            mutableState.value = AdInterstitialState.FAIL
            onLoaded?.invoke(false)
            return
        }
        if (interstitialAd != null) {
            mutableState.value = AdInterstitialState.LOADED
            onLoaded?.invoke(true)
            return
        }
        if (!canReloadAd && state.value == AdInterstitialState.CANCEL) {
            onLoaded?.invoke(false)
            return
        }
        onLoaded?.let(loadCallbacks::add)
        if (isLoading) return

        isLoading = true
        mutableState.value = AdInterstitialState.LOADING
        val generation = ++requestGeneration
        factory.requestInterstitialAd(activity, config.param.copy(adPlacement = config.adPlacement)) { ad ->
            if (generation != requestGeneration) {
                ad?.fullScreenContentCallback = null
                return@requestInterstitialAd
            }
            isLoading = false
            interstitialAd = ad
            mutableState.value = if (ad == null) AdInterstitialState.FAIL else AdInterstitialState.LOADED
            notifyLoadCallbacks(ad != null)
        }
    }

    fun showAd(activity: Activity, callback: InterstitialAdShowCallBack) {
        if (!adsAllowed(activity)) {
            callback.onAdDismissed()
            return
        }
        if (!isShowTimeValid()) {
            Log.d(tag, "Interstitial ad skipped by showByTime interval")
            callback.onAdDismissed()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            callback.onAdDismissed()
            return
        }
        showLoadedAd(activity, ad, callback)
    }

    fun forceShowInterstitial(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        callback: InterstitialAdShowCallBack,
        timeoutMs: Long = 1_500L
    ) {
        require(timeoutMs >= 0L) { "Interstitial readiness timeout must not be negative" }
        if (!adsAllowed(activity)) {
            callback.onAdFailedToShow("Interstitial ads are disabled")
            return
        }
        if (!isShowTimeValid()) {
            callback.onAdFailedToShow("Interstitial showByTime interval is active")
            return
        }
        if (state.value != AdInterstitialState.LOADING && interstitialAd == null) {
            callback.onAdFailedToShow("Interstitial ad is not loading or ready")
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            if (state.value == AdInterstitialState.LOADING) showLoadingDialog(activity)
            try {
                val ready = waitForAdReady(timeoutMs)
                dismissLoadingDialog()
                val ad = interstitialAd
                if (!ready || ad == null || activity.isFinishing || activity.isDestroyed) {
                    mutableState.value = AdInterstitialState.SHOW_FAIL
                    callback.onAdFailedToShow("Interstitial ad did not become ready")
                    return@launch
                }
                showLoadedAd(activity, ad, callback)
            } finally {
                dismissLoadingDialog()
            }
        }
    }

    suspend fun waitForAdReady(timeoutMs: Long = 1_500L): Boolean {
        require(timeoutMs >= 0L) { "Interstitial readiness timeout must not be negative" }
        val startedAt = System.currentTimeMillis()
        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            if (interstitialAd != null && state.value == AdInterstitialState.LOADED) return true
            if (!isLoading && state.value != AdInterstitialState.LOADED) return false
            delay(50L)
        }
        return interstitialAd != null && state.value == AdInterstitialState.LOADED
    }

    fun cancel() {
        requestGeneration++
        isLoading = false
        interstitialAd = null
        mutableState.value = AdInterstitialState.CANCEL
        notifyLoadCallbacks(false)
        dismissLoadingDialog()
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: InterstitialAd,
        callback: InterstitialAdShowCallBack
    ) {
        interstitialAd = null
        mutableState.value = AdInterstitialState.SHOW
        factory.showInterstitial(
            activity = activity,
            param = InterstitialAdParam.ShowAd(ad, config.adPlacement),
            onShown = {
                lastShownTimestamp = System.currentTimeMillis()
                mutableState.value = AdInterstitialState.SHOWED
                callback.onAdShowed()
            },
            onDismissed = {
                AdmobManager.onAdDismissed()
                mutableState.value = AdInterstitialState.NONE
                callback.onAdDismissed()
                if (canReloadAd) loadAd(activity)
            },
            onFailedToShow = { error ->
                AdmobManager.onAdDismissed()
                mutableState.value = AdInterstitialState.SHOW_FAIL
                callback.onAdFailedToShow(error.message)
                if (canReloadAd) loadAd(activity)
            }
        )
    }

    private fun adsAllowed(activity: Activity): Boolean =
        canRequestAds(activity, config.canShowAds) &&
            FirebaseConfigManager.isInterstitialAdsEnabled()

    private fun isShowTimeValid(): Boolean =
        config.showByTime == 0L || System.currentTimeMillis() - lastShownTimestamp >= config.showByTime

    private fun notifyLoadCallbacks(loaded: Boolean) {
        val callbacks = loadCallbacks.toList()
        loadCallbacks.clear()
        callbacks.forEach { it(loaded) }
    }

    private fun showLoadingDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            if (loadingDialog == null) loadingDialog = LoadingAdsDialog(activity)
            loadingDialog?.show()
        } catch (error: Exception) {
            Log.e(tag, "Unable to show interstitial loading dialog", error)
        }
    }

    private fun dismissLoadingDialog() {
        try {
            loadingDialog?.dismiss()
        } catch (error: Exception) {
            Log.e(tag, "Unable to dismiss interstitial loading dialog", error)
        } finally {
            loadingDialog = null
        }
    }

    companion object {
        private val instances = mutableMapOf<String, InterstitialAdsHelper>()

        @Synchronized
        fun getInstance(config: InterstitialAdConfig): InterstitialAdsHelper =
            instances.getOrPut(config.placementKey) { InterstitialAdsHelper(config) }
    }
}
