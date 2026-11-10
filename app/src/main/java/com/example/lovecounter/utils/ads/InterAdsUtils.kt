package com.example.lovecounter.utils.ads

import android.app.Activity
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdShowCallBack

object InterAdsUtils {
    fun loadInterAd(activity: Activity, adId: String, fallbackAdId: String? = null, onLoaded: ((Boolean) -> Unit)? = null) {
        val helper = InterstitialAdsHelper.getInstance(
            InterstitialAdConfig("default_inter", InterstitialAdParam(adId, fallbackAdId))
        )
        helper.loadAd(activity, onLoaded)
    }

    fun showInterAd(activity: Activity, onAdClosed: () -> Unit) {
        val helper = InterstitialAdsHelper.getInstance(
            InterstitialAdConfig("default_inter", InterstitialAdParam(AdPlacement.TEST_INTERSTITIAL_ID))
        )
        helper.showAd(activity, object : InterstitialAdShowCallBack {
            override fun onAdShowed() {}
            override fun onAdDismissed() { onAdClosed() }
            override fun onAdFailedToShow(error: String) { onAdClosed() }
        })
    }
}
