package com.ads.admob.helper.interstitial.factory.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdmobInterstitialAdFactoryImpl : AdmobInterstitialAdFactory {
    private val tag = "AdmobInterstitialFactory"

    @SuppressLint("MissingPermission")
    override fun requestInterstitialAd(
        context: Context,
        param: InterstitialAdParam.Request,
        onLoaded: (InterstitialAd?) -> Unit
    ) {
        if (!FirebaseConfigManager.isInterstitialAdsEnabled()) {
            onLoaded(null)
            return
        }
        val appContext = context.applicationContext
        AdmobFactoryImpl.INSTANCE.runWhenInitialized {
            loadInternal(appContext, param.adUnitId, param.fallbackAdUnitId, onLoaded)
        }
    }

    private fun loadInternal(
        context: Context,
        adUnitId: String,
        fallbackAdUnitId: String?,
        onLoaded: (InterstitialAd?) -> Unit
    ) {
        InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(tag, "Interstitial ad loaded: $adUnitId")
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(tag, "Interstitial ad failed to load $adUnitId: ${error.message}")
                    val fallback = fallbackAdUnitId?.takeIf {
                        it.isNotBlank() && it != adUnitId
                    }
                    if (fallback == null) {
                        onLoaded(null)
                    } else {
                        loadInternal(context, fallback, null, onLoaded)
                    }
                }
            }
        )
    }

    override fun showInterstitial(
        activity: Activity,
        param: InterstitialAdParam.ShowAd,
        onShown: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: (AdError) -> Unit,
        onClicked: () -> Unit,
        onImpression: () -> Unit
    ) {
        param.ad.fullScreenContentCallback = object : FullScreenContentCallback() {
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
        try {
            param.ad.show(activity)
        } catch (error: Exception) {
            AdmobManager.onAdDismissed()
            onFailedToShow(AdError(1999, error.message ?: "Unable to show Interstitial Ad", tag))
        }
    }
}
