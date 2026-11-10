package com.ads.admob.data

import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

sealed class ContentAd(
    open val adType: String,
    open val adUnitId: String,
    open var isLoaded: Boolean = false,
    open var adObject: Any? = null
) {
    data class ApNativeAd(
        override val adUnitId: String,
        val nativeAd: NativeAd?,
        override var isLoaded: Boolean = nativeAd != null
    ) : ContentAd(adType = "native", adUnitId = adUnitId, isLoaded = isLoaded, adObject = nativeAd)

    data class ApRewardAd(
        override val adUnitId: String,
        val rewardAd: RewardedAd?,
        override var isLoaded: Boolean = rewardAd != null
    ) : ContentAd(adType = "rewarded", adUnitId = adUnitId, isLoaded = isLoaded, adObject = rewardAd)

    data class ApRewardInterAd(
        override val adUnitId: String,
        val rewardInterAd: RewardedInterstitialAd?,
        override var isLoaded: Boolean = rewardInterAd != null
    ) : ContentAd(adType = "rewarded_interstitial", adUnitId = adUnitId, isLoaded = isLoaded, adObject = rewardInterAd)

    data class ApInterstitialAd(
        override val adUnitId: String,
        val interstitialAd: InterstitialAd?,
        override var isLoaded: Boolean = interstitialAd != null
    ) : ContentAd(adType = "interstitial", adUnitId = adUnitId, isLoaded = isLoaded, adObject = interstitialAd)

    data class ApAppOpenAd(
        override val adUnitId: String,
        val appOpenAd: AppOpenAd?,
        override var isLoaded: Boolean = appOpenAd != null
    ) : ContentAd(adType = "app_open", adUnitId = adUnitId, isLoaded = isLoaded, adObject = appOpenAd)

    data class ApAppResumeAd(
        override val adUnitId: String,
        val appResumeAd: AppOpenAd?,
        override var isLoaded: Boolean = appResumeAd != null
    ) : ContentAd(adType = "app_resume", adUnitId = adUnitId, isLoaded = isLoaded, adObject = appResumeAd)

    data class ApBannerAd(
        override val adUnitId: String,
        val bannerAdView: AdView?,
        override var isLoaded: Boolean = bannerAdView != null
    ) : ContentAd(adType = "banner", adUnitId = adUnitId, isLoaded = isLoaded, adObject = bannerAdView)

    data class GenericAd(
        override val adType: String,
        override val adUnitId: String,
        override var isLoaded: Boolean = false,
        override var adObject: Any? = null
    ) : ContentAd(adType, adUnitId, isLoaded, adObject)

    companion object {
        operator fun invoke(
            adType: String,
            adUnitId: String,
            isLoaded: Boolean = false,
            adObject: Any? = null
        ): ContentAd = GenericAd(adType, adUnitId, isLoaded, adObject)
    }
}
