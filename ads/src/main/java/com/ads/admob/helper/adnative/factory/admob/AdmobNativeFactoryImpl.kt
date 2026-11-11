package com.ads.admob.helper.adnative.factory.admob

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.ads.admob.R
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.adnative.params.AdNativeMediation
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.helper.adnative.params.NativeLayoutMediation
import com.ads.admob.widget.NativeTemplateStyle
import com.ads.admob.widget.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

class AdmobNativeFactoryImpl : AdmobNativeFactory {

    override fun requestNativeAd(
        context: Context,
        param: NativeAdParam,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit,
        onClicked: () -> Unit,
        onImpression: () -> Unit,
        onFailedWithErrorCode: ((String, Int) -> Unit)?
    ) {
        when (param) {
            is NativeAdParam.Ready -> {
                val ad = param.contentAd.nativeAd
                if (ad == null) onFailed("The supplied native ad is empty") else onLoaded(ad)
            }

            is NativeAdParam.Request -> {
                if (param.adUnitId.isBlank()) {
                    onFailed("Native ad unit ID must not be blank")
                    return
                }
                if (!FirebaseConfigManager.isNativeAdsEnabled()) {
                    onFailed("Native ads are disabled by remote configuration")
                    return
                }

                AdmobFactoryImpl.INSTANCE.runWhenInitialized {
                    val adUnitIds = listOfNotNull(
                        param.adUnitId,
                        param.waterfallAdUnitId?.takeIf(String::isNotBlank)
                    ).distinct()
                    requestFromAdUnit(
                        context = context.applicationContext,
                        adUnitIds = adUnitIds,
                        index = 0,
                        requestVideoContent = param.requestVideoContent,
                        onLoaded = onLoaded,
                        onFailed = onFailed,
                        onClicked = onClicked,
                        onImpression = onImpression,
                        onFailedWithErrorCode = onFailedWithErrorCode
                    )
                }
            }
        }
    }

    override fun populateNativeAdView(
        context: Context,
        nativeAd: NativeAd,
        container: ViewGroup,
        defaultLayoutId: Int,
        mediationLayouts: List<NativeLayoutMediation>,
        style: NativeTemplateStyle?
    ): Boolean {
        val mediation = AdNativeMediation.fromNativeAd(nativeAd)
        val layoutId = mediationLayouts.firstOrNull { it.mediation == mediation }?.layoutId
            ?: defaultLayoutId
        if (layoutId == 0) {
            Log.e(TAG, "Cannot populate native ad: no layout resource configured")
            return false
        }

        val rootView = try {
            LayoutInflater.from(context).inflate(layoutId, container, false)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Failed to inflate native ad layout $layoutId", error)
            return false
        }
        if (!bindNativeAdView(nativeAd, rootView, style)) return false

        container.removeAllViews()
        container.addView(rootView)
        container.visibility = View.VISIBLE
        return true
    }

    override fun bindNativeAdView(
        nativeAd: NativeAd,
        rootView: View,
        style: NativeTemplateStyle?
    ): Boolean {
        if (rootView is TemplateView) {
            style?.let(rootView::setStyles)
            rootView.setNativeAd(nativeAd)
            return true
        }

        val adView = rootView.findViewById<android.view.View>(R.id.nativeAdView) as? com.google.android.gms.ads.nativead.NativeAdView
            ?: rootView as? com.google.android.gms.ads.nativead.NativeAdView
        if (adView == null) {
            Log.e(TAG, "Native ad layout must contain a NativeAdView")
            return false
        }

        val headlineView = adView.findViewById<View>(R.id.ad_headline)
        val bodyView = adView.findViewById<View>(R.id.ad_body)
        val advertiserView = adView.findViewById<View>(R.id.ad_advertiser)
        val starRatingView = adView.findViewById<View>(R.id.ad_stars)
        val iconView = adView.findViewById<View>(R.id.ad_app_icon)
        val mediaView = adView.findViewById<View>(R.id.ad_media) as? MediaView
        val callToActionView = adView.findViewById<View>(R.id.ad_call_to_action)

        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.advertiserView = advertiserView
        adView.starRatingView = starRatingView
        adView.iconView = iconView
        adView.mediaView = mediaView
        adView.callToActionView = callToActionView

        (headlineView as? TextView)?.bindText(nativeAd.headline)
        (bodyView as? TextView)?.bindText(nativeAd.body)
        (advertiserView as? TextView)?.bindText(nativeAd.advertiser)
        (starRatingView as? RatingBar)?.apply {
            rating = nativeAd.starRating?.toFloat() ?: 0f
            visibility = if (nativeAd.starRating == null) View.GONE else View.VISIBLE
        }
        (iconView as? ImageView)?.apply {
            setImageDrawable(nativeAd.icon?.drawable)
            visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        }
        mediaView?.visibility =
            if (nativeAd.mediaContent == null) View.GONE else View.VISIBLE
        (callToActionView as? TextView)?.bindText(nativeAd.callToAction)

        applyStyle(style, adView, headlineView, bodyView, advertiserView, callToActionView)
        adView.setNativeAd(nativeAd)
        return true
    }

    private fun requestFromAdUnit(
        context: Context,
        adUnitIds: List<String>,
        index: Int,
        requestVideoContent: Boolean,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit,
        onClicked: () -> Unit,
        onImpression: () -> Unit,
        onFailedWithErrorCode: ((String, Int) -> Unit)?
    ) {
        val adUnitId = adUnitIds.getOrNull(index)
        if (adUnitId == null) {
            onFailed("Native ad failed to load from all configured ad unit IDs")
            return
        }

        val optionsBuilder = NativeAdOptions.Builder()
        if (requestVideoContent) {
            optionsBuilder.setVideoOptions(
                VideoOptions.Builder()
                    .setStartMuted(true)
                    .build()
            )
        }
        val options = optionsBuilder.build()
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded from configured ad unit")
                onLoaded(nativeAd)
            }
            .withNativeAdOptions(options)
            .withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    onClicked()
                }

                override fun onAdImpression() {
                    onImpression()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad request failed: ${error.message}")
                    if (index + 1 < adUnitIds.size) {
                        requestFromAdUnit(
                            context,
                            adUnitIds,
                            index + 1,
                            requestVideoContent,
                            onLoaded,
                            onFailed,
                            onClicked,
                            onImpression,
                            onFailedWithErrorCode
                        )
                    } else {
                        onFailedWithErrorCode?.invoke(error.message, error.code)
                            ?: onFailed(error.message)
                    }
                }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    private fun applyStyle(
        style: NativeTemplateStyle?,
        adView: com.google.android.gms.ads.nativead.NativeAdView,
        headline: View?,
        body: View?,
        advertiser: View?,
        callToAction: View?
    ) {
        if (style == null) return
        when {
            style.mainBackground != null -> adView.background = style.mainBackground
            style.mainBackgroundColor != null -> adView.setBackgroundColor(style.mainBackgroundColor)
        }
        style.primaryTextFont?.let { (headline as? TextView)?.typeface = it }
        style.secondaryTextFont?.let { (body as? TextView)?.typeface = it }
        style.tertiaryTextFont?.let { (advertiser as? TextView)?.typeface = it }
        style.callToActionTextFont?.let { (callToAction as? TextView)?.typeface = it }
        if (style.primaryTextSize > 0f) {
            (headline as? TextView)?.textSize = style.primaryTextSize
        }
        if (style.secondaryTextSize > 0f) {
            (body as? TextView)?.textSize = style.secondaryTextSize
        }
        if (style.tertiaryTextSize > 0f) {
            (advertiser as? TextView)?.textSize = style.tertiaryTextSize
        }
        if (style.callToActionTextSize > 0f) {
            (callToAction as? TextView)?.textSize = style.callToActionTextSize
        }
        if (style.hasPrimaryTextColor) {
            (headline as? TextView)?.setTextColor(style.primaryTextColor)
        }
        if (style.hasSecondaryTextColor) {
            (body as? TextView)?.setTextColor(style.secondaryTextColor)
        }
        if (style.hasTertiaryTextColor) {
            (advertiser as? TextView)?.setTextColor(style.tertiaryTextColor)
        }
        if (style.hasCallToActionTextColor) {
            (callToAction as? TextView)?.setTextColor(style.callToActionTextColor)
        }
        style.callToActionBackgroundColor?.let { callToAction?.background = it }
        style.callToActionBackgroundTint?.let {
            callToAction?.backgroundTintList = android.content.res.ColorStateList.valueOf(it)
        }
    }

    private fun TextView.bindText(value: String?) {
        text = value
        visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    companion object {
        private const val TAG = "AdmobNativeFactory"
    }
}
