package com.ads.admob.helper.reward

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.AdmobManager
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.reward.factory.admob.AdmobRewardAdFactoryImpl
import com.ads.admob.listener.RewardAdCallBack
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RewardAdHelper private constructor(private val config: RewardAdConfig) :
    AdsHelper<RewardAdParam>() {
    private val tag = "RewardAdHelper"
    private val factory = AdmobRewardAdFactoryImpl()
    private val mutableState = MutableStateFlow(AdRewardState.NONE)
    val state: StateFlow<AdRewardState> = mutableState

    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoadingReward = false
    private var isLoadingInterstitial = false
    private var rewardGeneration = 0L
    private var interstitialGeneration = 0L
    private var lastShownTimestamp = 0L
    private var loadingDialog: LoadingAdsDialog? = null
    private val rewardLoadCallbacks = mutableListOf<RewardAdCallBack>()
    private val interstitialLoadCallbacks = mutableListOf<RewardAdCallBack>()

    fun loadAd(activity: Activity, callback: RewardAdCallBack) {
        canReloadAd = config.canReloadAds
        if (!adsAllowed(activity)) {
            mutableState.value = AdRewardState.FAIL
            callback.onAdFailedToLoad("Rewarded ads are disabled or consent is unavailable")
            return
        }
        rewardedAd?.let {
            mutableState.value = AdRewardState.LOADED
            callback.onAdLoaded()
            return
        }
        if (!canReloadAd && mutableState.value == AdRewardState.CANCEL) {
            callback.onAdFailedToLoad("Rewarded ad loading is disabled")
            return
        }
        rewardLoadCallbacks.add(callback)
        if (isLoadingReward) return

        isLoadingReward = true
        mutableState.value = AdRewardState.LOADING
        val generation = ++rewardGeneration
        factory.requestRewardAd(
            activity,
            config.param.copy(adPlacement = config.adPlacement)
        ) { ad, error ->
            if (generation != rewardGeneration) return@requestRewardAd
            isLoadingReward = false
            rewardedAd = ad
            mutableState.value = if (ad == null) AdRewardState.FAIL else AdRewardState.LOADED
            notifyLoadCallbacks(rewardLoadCallbacks, ad != null, error)
        }
    }

    fun requestRewardInterAd(activity: Activity, callback: RewardAdCallBack) {
        canReloadAd = config.canReloadAds
        if (!adsAllowed(activity)) {
            mutableState.value = AdRewardState.FAIL
            callback.onAdFailedToLoad("Rewarded interstitial ads are disabled or consent is unavailable")
            return
        }
        rewardedInterstitialAd?.let {
            mutableState.value = AdRewardState.LOADED
            callback.onAdLoaded()
            return
        }
        if (!canReloadAd && mutableState.value == AdRewardState.CANCEL) {
            callback.onAdFailedToLoad("Rewarded interstitial loading is disabled")
            return
        }
        interstitialLoadCallbacks.add(callback)
        if (isLoadingInterstitial) return

        isLoadingInterstitial = true
        mutableState.value = AdRewardState.LOADING
        val generation = ++interstitialGeneration
        factory.requestRewardInterAd(
            activity,
            config.param.copy(adPlacement = config.adPlacement)
        ) { ad, error ->
            if (generation != interstitialGeneration) return@requestRewardInterAd
            isLoadingInterstitial = false
            rewardedInterstitialAd = ad
            mutableState.value = if (ad == null) AdRewardState.FAIL else AdRewardState.LOADED
            notifyLoadCallbacks(interstitialLoadCallbacks, ad != null, error)
        }
    }

    fun loadRewardInterAd(activity: Activity, callback: RewardAdCallBack) =
        requestRewardInterAd(activity, callback)

    fun showAd(activity: Activity, callback: RewardAdCallBack) {
        show(activity, callback, rewardedInterstitial = false)
    }

    fun showRewardInterAd(activity: Activity, callback: RewardAdCallBack) {
        show(activity, callback, rewardedInterstitial = true)
    }

    fun forceShowRewardAd(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        callback: RewardAdCallBack,
        timeoutMs: Long = 1_500L
    ) = forceShow(activity, lifecycleOwner, callback, rewardedInterstitial = false, timeoutMs)

    fun forceShowRewardInterAd(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        callback: RewardAdCallBack,
        timeoutMs: Long = 1_500L
    ) = forceShow(activity, lifecycleOwner, callback, rewardedInterstitial = true, timeoutMs)

    suspend fun waitForAdReady(timeoutMs: Long = 1_500L, rewardedInterstitial: Boolean = false): Boolean {
        require(timeoutMs >= 0L) { "Rewarded ad readiness timeout must not be negative" }
        val startedAt = System.currentTimeMillis()
        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            if (isReady(rewardedInterstitial)) return true
            if (!isLoading(rewardedInterstitial)) return false
            delay(50L)
        }
        return isReady(rewardedInterstitial)
    }

    fun cancel() {
        rewardGeneration++
        interstitialGeneration++
        isLoadingReward = false
        isLoadingInterstitial = false
        rewardedAd = null
        rewardedInterstitialAd = null
        mutableState.value = AdRewardState.CANCEL
        notifyLoadCallbacks(rewardLoadCallbacks, false, null)
        notifyLoadCallbacks(interstitialLoadCallbacks, false, null)
        dismissLoadingDialog()
    }

    private fun show(activity: Activity, callback: RewardAdCallBack, rewardedInterstitial: Boolean) {
        val showAction = RewardAdParam.Show(config.adPlacement)
        if (!adsAllowed(activity)) {
            mutableState.value = AdRewardState.SHOW_FAIL
            callback.onAdClosed()
            return
        }
        if (!isShowTimeValid()) {
            callback.onAdFailedToShow("Rewarded ad showByTime interval is active")
            return
        }
        val param = if (rewardedInterstitial) {
            rewardedInterstitialAd?.let {
                rewardedInterstitialAd = null
                RewardAdParam.ShowAd.RewardedInterstitial(it, showAction.adPlacement)
            }
        } else {
            rewardedAd?.let {
                rewardedAd = null
                RewardAdParam.ShowAd.Rewarded(it, showAction.adPlacement)
            }
        }
        if (param == null) {
            mutableState.value = AdRewardState.SHOW_FAIL
            callback.onAdFailedToShow("Rewarded ad is not ready")
            return
        }
        showLoadedAd(activity, param, callback, rewardedInterstitial)
    }

    private fun forceShow(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        callback: RewardAdCallBack,
        rewardedInterstitial: Boolean,
        timeoutMs: Long
    ) {
        require(timeoutMs >= 0L) { "Rewarded ad readiness timeout must not be negative" }
        if (!adsAllowed(activity) || (!isReady(rewardedInterstitial) && !isLoading(rewardedInterstitial))) {
            mutableState.value = AdRewardState.SHOW_FAIL
            callback.onAdFailedToShow("Rewarded ad is not loading or ready")
            return
        }
        lifecycleOwner.lifecycleScope.launch {
            if (isLoading(rewardedInterstitial)) showLoadingDialog(activity)
            try {
                val ready = waitForAdReady(timeoutMs, rewardedInterstitial)
                dismissLoadingDialog()
                if (!ready || activity.isFinishing || activity.isDestroyed) {
                    mutableState.value = AdRewardState.SHOW_FAIL
                    callback.onAdFailedToShow("Rewarded ad did not become ready")
                    return@launch
                }
                show(activity, callback, rewardedInterstitial)
            } finally {
                dismissLoadingDialog()
            }
        }
    }

    private fun showLoadedAd(
        activity: Activity,
        param: RewardAdParam.ShowAd,
        callback: RewardAdCallBack,
        rewardedInterstitial: Boolean
    ) {
        mutableState.value = AdRewardState.SHOW
        factory.showRewardAd(
            activity = activity,
            param = param,
            onShown = {
                lastShownTimestamp = System.currentTimeMillis()
                mutableState.value = AdRewardState.SHOWED
                callback.onAdShowed()
            },
            onDismissed = {
                AdmobManager.onAdDismissed()
                mutableState.value = AdRewardState.NONE
                callback.onAdClosed()
                if (canReloadAd && adsAllowed(activity)) {
                    if (rewardedInterstitial) requestRewardInterAd(activity, noOpCallback())
                    else loadAd(activity, noOpCallback())
                }
            },
            onFailedToShow = { error ->
                AdmobManager.onAdDismissed()
                mutableState.value = AdRewardState.SHOW_FAIL
                callback.onAdFailedToShow(error.message)
                if (canReloadAd && adsAllowed(activity)) {
                    if (rewardedInterstitial) requestRewardInterAd(activity, noOpCallback())
                    else loadAd(activity, noOpCallback())
                }
            },
            onEarnedReward = callback::onUserEarnedReward,
            onClicked = { callback.onAdClick(RewardAdParam.Clickable(config.adPlacement)) },
            onImpression = callback::onAdImpression
        )
    }

    private fun isReady(rewardedInterstitial: Boolean): Boolean =
        if (rewardedInterstitial) rewardedInterstitialAd != null else rewardedAd != null

    private fun isLoading(rewardedInterstitial: Boolean): Boolean =
        if (rewardedInterstitial) isLoadingInterstitial else isLoadingReward

    private fun adsAllowed(activity: Activity): Boolean =
        canRequestAds(activity, config.canShowAds) &&
            FirebaseConfigManager.isRewardedAdsEnabled()

    private fun isShowTimeValid(): Boolean =
        config.showByTime == 0L || System.currentTimeMillis() - lastShownTimestamp >= config.showByTime

    private fun notifyLoadCallbacks(
        callbacks: MutableList<RewardAdCallBack>,
        loaded: Boolean,
        error: LoadAdError?
    ) {
        val pending = callbacks.toList()
        callbacks.clear()
        pending.forEach { callback ->
            if (loaded) callback.onAdLoaded()
            else callback.onAdFailedToLoad(error?.message ?: "Rewarded ad failed to load")
        }
    }

    private fun noOpCallback() = object : RewardAdCallBack {
        override fun onAdLoaded() = Unit
        override fun onAdFailedToLoad(error: String) = Unit
        override fun onUserEarnedReward(rewardItem: RewardItem) = Unit
        override fun onAdClosed() = Unit
    }

    private fun showLoadingDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            if (loadingDialog == null) loadingDialog = LoadingAdsDialog(activity)
            loadingDialog?.show()
        } catch (error: Exception) {
            Log.e(tag, "Unable to show rewarded loading dialog", error)
        }
    }

    private fun dismissLoadingDialog() {
        try {
            loadingDialog?.dismiss()
        } catch (error: Exception) {
            Log.e(tag, "Unable to dismiss rewarded loading dialog", error)
        } finally {
            loadingDialog = null
        }
    }

    companion object {
        private val instances = mutableMapOf<String, RewardAdHelper>()

        @Synchronized
        fun getInstance(config: RewardAdConfig): RewardAdHelper =
            instances.getOrPut(config.placementKey) { RewardAdHelper(config) }
    }
}
