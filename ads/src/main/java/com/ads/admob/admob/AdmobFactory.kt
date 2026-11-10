package com.ads.admob.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.ViewGroup
import com.ads.admob.config.AfAdConfig
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.BannerAdCallBack
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.listener.RewardAdCallBack
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

interface AdmobFactory {
    fun initAdmob(application: Application, adConfig: AfAdConfig, onInitialized: (() -> Unit)? = null)
    fun initMediation(context: Context)
    fun isInitialized(): Boolean
    fun runWhenInitialized(task: () -> Unit)
    fun getConfig(): AfAdConfig?
    fun setEventConfig(config: AfAdConfig)
    fun cancelRequestAndShowAllAds()
    fun isShowAdsIntervalValid(): Boolean

    // Banner Ads
    fun requestBannerAd(
        context: Context,
        container: ViewGroup,
        adUnitId: String,
        callback: BannerAdCallBack? = null
    )

    // Native Ads
    fun requestNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdCallback
    )
    fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView
    )

    // Interstitial Ads
    fun requestInterstitialAds(
        context: Context,
        adUnitId: String,
        callback: InterstitialAdRequestCallBack
    )
    fun showInterstitial(
        activity: Activity,
        contentAd: ContentAd?,
        callback: InterstitialAdShowCallBack
    )

    // App Open Ads
    fun requestAppOpenAds(
        context: Context,
        adUnitId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        callback: AppOpenAdCallBack
    )
    fun showAppOpen(
        activity: Activity,
        contentAd: ContentAd?,
        callback: AppOpenAdCallBack
    )

    // Rewarded Ads
    fun requestRewardAd(
        context: Context,
        adUnitId: String,
        callback: RewardAdCallBack
    )
    fun showRewardAd(
        activity: Activity,
        contentAd: ContentAd?,
        callback: RewardAdCallBack
    )
}

