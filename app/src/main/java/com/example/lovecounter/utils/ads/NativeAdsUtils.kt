package com.example.lovecounter.utils.ads

import android.content.Context
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.listener.NativeAdCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

object NativeAdsUtils {
    private val cachedNativeAds = mutableMapOf<String, NativeAd>()
    private val pendingCallbacks = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()

    fun preloadNative(
        context: Context,
        placementKey: String,
        adId: String,
        fallbackAdId: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        var alreadyCached = false
        var shouldLoad = false
        synchronized(cachedNativeAds) {
            if (cachedNativeAds.containsKey(placementKey)) {
                alreadyCached = true
            } else {
                val callbacks = pendingCallbacks[placementKey]
                if (callbacks == null) {
                    pendingCallbacks[placementKey] = mutableListOf()
                    shouldLoad = true
                }
                onComplete?.let { pendingCallbacks[placementKey]?.add(it) }
            }
        }
        if (alreadyCached) {
            onComplete?.invoke(true)
            return
        }
        if (!shouldLoad) return

        val helper = NativeAdHelper(NativeAdConfig(placementKey, NativeAdParam(adId, fallbackAdId)))
        helper.loadNative(context, object : NativeAdCallback {
            override fun onAdLoaded(nativeAd: NativeAd) {
                val callbacks = synchronized(cachedNativeAds) {
                    cachedNativeAds[placementKey] = nativeAd
                    pendingCallbacks.remove(placementKey).orEmpty()
                }
                callbacks.forEach { it(true) }
            }
            override fun onAdFailedToLoad(error: String) {
                val callbacks = synchronized(cachedNativeAds) {
                    pendingCallbacks.remove(placementKey).orEmpty()
                }
                callbacks.forEach { it(false) }
            }
        })
    }

    fun getPreloadedNative(placementKey: String): NativeAd? {
        return synchronized(cachedNativeAds) { cachedNativeAds[placementKey] }
    }

    fun destroyNative(placementKey: String) {
        synchronized(cachedNativeAds) {
            cachedNativeAds.remove(placementKey)?.destroy()
        }
    }

    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        val context = adView.context
        val pkg = context.packageName

        adView.headlineView = adView.findViewById(context.resources.getIdentifier("ad_headline", "id", pkg))
        adView.bodyView = adView.findViewById(context.resources.getIdentifier("ad_body", "id", pkg))
        adView.callToActionView = adView.findViewById(context.resources.getIdentifier("ad_call_to_action", "id", pkg))
        adView.iconView = adView.findViewById(context.resources.getIdentifier("ad_app_icon", "id", pkg))
        adView.mediaView = adView.findViewById(context.resources.getIdentifier("ad_media", "id", pkg))

        (adView.headlineView as? android.widget.TextView)?.text = nativeAd.headline

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = android.view.View.GONE
        } else {
            adView.bodyView?.visibility = android.view.View.VISIBLE
            (adView.bodyView as? android.widget.TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = android.view.View.GONE
        } else {
            adView.callToActionView?.visibility = android.view.View.VISIBLE
            (adView.callToActionView as? android.widget.Button)?.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = android.view.View.GONE
        } else {
            adView.iconView?.visibility = android.view.View.VISIBLE
            (adView.iconView as? android.widget.ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        }

        adView.setNativeAd(nativeAd)
    }

    fun destroyAll() {
        synchronized(cachedNativeAds) {
            cachedNativeAds.values.forEach { it.destroy() }
            cachedNativeAds.clear()
        }
    }
}
