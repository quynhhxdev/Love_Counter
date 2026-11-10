package com.ads.admob.helper.banner.factory.admob

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.ads.admob.AdmobExtension
import com.ads.admob.AdmobManager
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.listener.BannerAdCallBack
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong

class AdmobBannerFactoryImpl : AdmobBannerFactory {
    private val tag = "AdmobBannerFactory"
    private val requestIds = WeakHashMap<ViewGroup, Long>()
    private val nextRequestId = AtomicLong()

    override fun requestBannerAd(
        context: Context,
        container: ViewGroup,
        param: BannerAdParam.Request,
        callback: BannerAdCallBack?
    ) {
        if (!com.ads.admob.config.FirebaseConfigManager.isBannerAdsEnabled()) {
            destroyBanner(container)
            callback?.onAdFailedToLoad("Banner ads are disabled")
            return
        }
        val requestId = nextRequestId.incrementAndGet()
        synchronized(requestIds) { requestIds[container] = requestId }
        AdmobFactoryImpl.INSTANCE.runWhenInitialized {
            if (isCurrent(container, requestId)) {
                loadInternal(context, container, param, param.adUnitId, param.fallbackAdUnitId, requestId, callback)
            }
        }
    }

    override fun getAdSize(context: Context, container: ViewGroup, param: BannerAdParam.Request): AdSize =
        if (param.useInlineAdaptive) {
            AdmobExtension.getInlineAdaptiveBannerSize(context, container, param.bannerInlineStyle)
        } else {
            AdmobExtension.getAnchoredAdaptiveBannerSize(context, container)
        }

    @SuppressLint("MissingPermission")
    private fun loadInternal(
        context: Context,
        container: ViewGroup,
        param: BannerAdParam.Request,
        adUnitId: String,
        fallbackAdUnitId: String?,
        requestId: Long,
        callback: BannerAdCallBack?
    ) {
        val adView = try {
            AdView(context).apply {
                setAdSize(getAdSize(context, container, param))
                this.adUnitId = adUnitId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        } catch (error: Exception) {
            Log.e(tag, "Unable to create banner ad view", error)
            callback?.onAdFailedToLoad(error.message ?: "Unable to create banner ad view")
            return
        }

        if (!isCurrent(container, requestId)) {
            adView.destroy()
            return
        }
        destroyAdViews(container)
        container.addView(adView)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (isCurrent(container, requestId)) {
                    callback?.onAdReady(BannerAdParam.Ready(adView, param.adPlacement))
                    callback?.onAdLoaded()
                } else {
                    adView.destroy()
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (!isCurrent(container, requestId)) {
                    adView.destroy()
                    return
                }
                Log.e(tag, "Banner ad failed to load with ID $adUnitId: ${error.message}")
                val fallback = fallbackAdUnitId?.takeIf { it.isNotBlank() && it != adUnitId }
                if (fallback != null) {
                    container.removeView(adView)
                    adView.destroy()
                    loadInternal(context, container, param, fallback, null, requestId, callback)
                } else {
                    container.removeView(adView)
                    adView.destroy()
                    callback?.onAdFailedToLoad(error.message)
                }
            }

            override fun onAdClicked() {
                if (!isCurrent(container, requestId)) return
                AdmobManager.onClickAd()
                callback?.onAdClick(BannerAdParam.Clickable(adView, param.adPlacement))
            }

            override fun onAdImpression() {
                if (isCurrent(container, requestId)) callback?.onAdImpression()
            }
        }

        try {
            adView.loadAd(AdmobExtension.createAdRequest(param.collapsibleGravity))
        } catch (error: Exception) {
            container.removeView(adView)
            adView.destroy()
            Log.e(tag, "Banner load exception", error)
            callback?.onAdFailedToLoad(error.message ?: "Unknown banner load error")
        }
    }

    override fun destroyBanner(container: ViewGroup) {
        synchronized(requestIds) { requestIds[container] = nextRequestId.incrementAndGet() }
        destroyAdViews(container)
        container.removeAllViews()
    }

    private fun destroyAdViews(container: ViewGroup) {
        for (index in container.childCount - 1 downTo 0) {
            (container.getChildAt(index) as? AdView)?.let { adView ->
                container.removeView(adView)
                adView.destroy()
            }
        }
    }

    private fun isCurrent(container: ViewGroup, requestId: Long): Boolean =
        synchronized(requestIds) { requestIds[container] == requestId }
}
