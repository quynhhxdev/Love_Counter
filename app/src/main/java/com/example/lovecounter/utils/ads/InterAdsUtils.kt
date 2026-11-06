package com.example.lovecounter.utils.ads

import android.app.Activity
import com.example.lovecounter.utils.Logger
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterAdsUtils {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadInterAd(activity: Activity, adId: String, onLoaded: ((Boolean) -> Unit)? = null) {
        if (interstitialAd != null || isLoading) {
            onLoaded?.invoke(interstitialAd != null)
            return
        }
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, adId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoading = false
                Logger.d("Inter ad loaded")
                onLoaded?.invoke(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                isLoading = false
                Logger.e("Inter ad failed to load: ${error.message}")
                onLoaded?.invoke(false)
            }
        })
    }

    fun showInterAd(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            interstitialAd = null
            ad.show(activity)
            onAdClosed()
        } else {
            onAdClosed()
        }
    }
}
