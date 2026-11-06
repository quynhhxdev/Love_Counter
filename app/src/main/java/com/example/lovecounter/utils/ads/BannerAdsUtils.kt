package com.example.lovecounter.utils.ads

import android.content.Context
import android.view.ViewGroup
import com.example.lovecounter.utils.Logger
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object BannerAdsUtils {

    fun loadBanner(context: Context, container: ViewGroup, adId: String) {
        try {
            val adView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adId
            }
            container.removeAllViews()
            container.addView(adView)

            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Logger.e("Banner ad failed to load: ${error.message}")
                }
            }
            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Logger.e("Banner load exception: ${e.message}")
        }
    }
}
