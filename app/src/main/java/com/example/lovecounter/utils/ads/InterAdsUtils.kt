package com.example.lovecounter.utils.ads

import android.app.Activity
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdShowCallBack

object InterAdsUtils {
    fun loadInterAd(
        activity: Activity,
        adId: String = AdPlacement.INTER_MAIN_ID,
        placementKey: String = AdPlacement.DEFAULT_INTER,
        fallbackAdId: String? = null,
        onLoaded: ((Boolean) -> Unit)? = null
    ) {
        if (!AdsPolicyManager.canLoadAd(activity, AdsPolicyManager.AdType.INTERSTITIAL)) {
            onLoaded?.invoke(false)
            return
        }

        AdsInitializer.runWhenReady {
            val helper = InterstitialAdsHelper.getInstance(
                InterstitialAdConfig(placementKey, InterstitialAdParam(adId, fallbackAdId))
            )
            helper.loadAd(activity, onLoaded)
        }
    }

    fun showInterAd(
        activity: Activity,
        adId: String = AdPlacement.INTER_MAIN_ID,
        placementKey: String = AdPlacement.DEFAULT_INTER,
        onAdClosed: () -> Unit = {}
    ) {
        if (!AdsPolicyManager.canShowAd(activity, AdsPolicyManager.AdType.INTERSTITIAL)) {
            onAdClosed()
            return
        }

        AdsInitializer.runWhenReady {
            val helper = InterstitialAdsHelper.getInstance(
                InterstitialAdConfig(placementKey, InterstitialAdParam(adId))
            )
            helper.showAd(activity, object : InterstitialAdShowCallBack {
                override fun onAdShowed() {}
                override fun onAdDismissed() { onAdClosed() }
                override fun onAdFailedToShow(error: String) { onAdClosed() }
            })
        }
    }
}
