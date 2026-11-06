package com.example.lovecounter.utils.ads

import android.content.Context
import com.example.lovecounter.utils.Logger
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

object NativeAdsUtils {

    private val cachedNativeAds = mutableMapOf<String, NativeAd>()

    fun preloadNative(
        context: Context,
        placementKey: String,
        adId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (cachedNativeAds.containsKey(placementKey)) {
            onComplete?.invoke(true)
            return
        }
        val builder = AdLoader.Builder(context, adId)
            .forNativeAd { nativeAd ->
                cachedNativeAds[placementKey] = nativeAd
                Logger.d("Native ad preloaded for key: $placementKey")
                onComplete?.invoke(true)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Logger.e("Native ad failed to load for key: $placementKey - ${error.message}")
                    onComplete?.invoke(false)
                }
            })
        builder.build().loadAd(AdRequest.Builder().build())
    }

    fun getPreloadedNative(placementKey: String): NativeAd? {
        return cachedNativeAds[placementKey]
    }

    fun destroyAll() {
        cachedNativeAds.values.forEach { it.destroy() }
        cachedNativeAds.clear()
    }
}
