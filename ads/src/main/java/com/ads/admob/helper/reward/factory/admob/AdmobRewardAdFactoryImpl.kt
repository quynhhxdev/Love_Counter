package com.ads.admob.helper.reward.factory.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.reward.RewardAdParam
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class AdmobRewardAdFactoryImpl : AdmobRewardAdFactory {
    private val tag = "AdmobRewardFactory"

    @SuppressLint("MissingPermission")
    override fun requestRewardAd(
        context: Context,
        param: RewardAdParam.Request,
        onLoaded: (RewardedAd?, LoadAdError?) -> Unit
    ) {
        if (!FirebaseConfigManager.isRewardedAdsEnabled()) {
            onLoaded(null, null)
            return
        }
        val appContext = context.applicationContext
        AdmobFactoryImpl.INSTANCE.runWhenInitialized {
            loadRewarded(appContext, param.adUnitId, param.fallbackAdUnitId, onLoaded)
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestRewardInterAd(
        context: Context,
        param: RewardAdParam.Request,
        onLoaded: (RewardedInterstitialAd?, LoadAdError?) -> Unit
    ) {
        if (!FirebaseConfigManager.isRewardedAdsEnabled()) {
            onLoaded(null, null)
            return
        }
        val appContext = context.applicationContext
        AdmobFactoryImpl.INSTANCE.runWhenInitialized {
            loadRewardedInterstitial(appContext, param.adUnitId, param.fallbackAdUnitId, onLoaded)
        }
    }

    private fun loadRewarded(
        context: Context,
        adUnitId: String,
        fallbackAdUnitId: String?,
        onLoaded: (RewardedAd?, LoadAdError?) -> Unit
    ) {
        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(tag, "Rewarded ad loaded: $adUnitId")
                    onLoaded(ad, null)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadRewardedFallback(context, adUnitId, fallbackAdUnitId, error, onLoaded)
                }
            }
        )
    }

    private fun loadRewardedFallback(
        context: Context,
        adUnitId: String,
        fallbackAdUnitId: String?,
        error: LoadAdError,
        onLoaded: (RewardedAd?, LoadAdError?) -> Unit
    ) {
        Log.e(tag, "Rewarded ad failed to load $adUnitId: ${error.message}")
        val fallback = fallbackAdUnitId?.takeIf { it.isNotBlank() && it != adUnitId }
        if (fallback == null) {
            onLoaded(null, error)
        } else {
            loadRewarded(context, fallback, null, onLoaded)
        }
    }

    private fun loadRewardedInterstitial(
        context: Context,
        adUnitId: String,
        fallbackAdUnitId: String?,
        onLoaded: (RewardedInterstitialAd?, LoadAdError?) -> Unit
    ) {
        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    Log.d(tag, "Rewarded interstitial ad loaded: $adUnitId")
                    onLoaded(ad, null)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(tag, "Rewarded interstitial ad failed to load $adUnitId: ${error.message}")
                    val fallback = fallbackAdUnitId?.takeIf {
                        it.isNotBlank() && it != adUnitId
                    }
                    if (fallback == null) onLoaded(null, error)
                    else loadRewardedInterstitial(context, fallback, null, onLoaded)
                }
            }
        )
    }

    override fun showRewardAd(
        activity: Activity,
        param: RewardAdParam.ShowAd,
        onShown: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: (AdError) -> Unit,
        onEarnedReward: (RewardItem) -> Unit,
        onClicked: () -> Unit,
        onImpression: () -> Unit
    ) {
        when (param) {
            is RewardAdParam.ShowAd.Rewarded -> {
                param.ad.fullScreenContentCallback = fullScreenCallback(
                    onShown, onDismissed, onFailedToShow, onClicked, onImpression
                )
                try {
                    param.ad.show(activity, onEarnedReward)
                } catch (error: Exception) {
                    AdmobManager.onAdDismissed()
                    onFailedToShow(AdError(1999, error.message ?: "Unable to show rewarded ad", tag))
                }
            }

            is RewardAdParam.ShowAd.RewardedInterstitial -> {
                param.ad.fullScreenContentCallback = fullScreenCallback(
                    onShown, onDismissed, onFailedToShow, onClicked, onImpression
                )
                try {
                    param.ad.show(activity, onEarnedReward)
                } catch (error: Exception) {
                    AdmobManager.onAdDismissed()
                    onFailedToShow(AdError(1999, error.message ?: "Unable to show rewarded interstitial ad", tag))
                }
            }
        }
    }

    private fun fullScreenCallback(
        onShown: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: (AdError) -> Unit,
        onClicked: () -> Unit,
        onImpression: () -> Unit
    ) = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            AdmobManager.onAdShowing()
            onShown()
        }

        override fun onAdDismissedFullScreenContent() {
            AdmobManager.onAdDismissed()
            onDismissed()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            AdmobManager.onAdDismissed()
            onFailedToShow(error)
        }

        override fun onAdClicked() {
            AdmobManager.onClickAd()
            onClicked()
        }

        override fun onAdImpression() = onImpression()
    }
}
