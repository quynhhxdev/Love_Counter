package com.ads.admob.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.ads.admob.AdmobManager
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.EventConfig
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.data.ContentAd
import com.ads.admob.event.AdjustTrackingManager
import com.ads.admob.event.FacebookTrackingManager
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.BannerAdCallBack
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.listener.RewardAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdmobFactoryImpl private constructor() : AdmobFactory {

    private val TAG = "AdmobFactoryImpl"
    private var initialized = false
    private var isCancelRequest = false
    private var adConfig: AfAdConfig? = null
    private var previousAdCloseTime: Long = 0L
    private val pendingTasks = mutableListOf<() -> Unit>()

    override fun initAdmob(application: Application, adConfig: AfAdConfig, onInitialized: (() -> Unit)?) {
        this.adConfig = adConfig

        if (initialized) {
            onInitialized?.invoke()
            return
        }

        if (onInitialized != null) {
            synchronized(pendingTasks) {
                pendingTasks.add(onInitialized)
            }
        }

        try {
            if (adConfig.testDevices.isNotEmpty()) {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(adConfig.testDevices)
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)
            }

            MobileAds.initialize(application) { initializationStatus ->
                initialized = true
                Log.d(TAG, "AdMob initialized via AdmobFactoryImpl. Status: $initializationStatus")
                initMediation(application)
                initTracking(application, adConfig)

                val tasks = synchronized(pendingTasks) {
                    val list = pendingTasks.toList()
                    pendingTasks.clear()
                    list
                }
                tasks.forEach { it() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdmobFactoryImpl initialization error: ${e.message}")
            val tasks = synchronized(pendingTasks) {
                val list = pendingTasks.toList()
                pendingTasks.clear()
                list
            }
            tasks.forEach { it() }
        }

        try {
            FirebaseConfigManager.fetchAndActivate()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseConfigManager fetch error: ${e.message}")
        }
    }

    override fun initMediation(context: Context) {
        Log.d(TAG, "Init Mediation networks (AppLovin, Mintegral, Unity, Vungle, etc.)")
    }

    private fun initTracking(application: Application, adConfig: AfAdConfig) {
        adConfig.adjustConfig?.let { adjust ->
            if (adjust.appToken.isNotBlank()) {
                AdjustTrackingManager.initialize(application, adjust)
            } else {
                Log.w(TAG, "Adjust configuration provided without an app token; SDK not initialized")
            }
        }
        Log.d(TAG, "Firebase Analytics is ready; Facebook tracking is initialized by the app after consent.")
    }

    override fun isInitialized(): Boolean = initialized

    override fun runWhenInitialized(task: () -> Unit) {
        if (initialized) {
            task()
        } else {
            synchronized(pendingTasks) {
                pendingTasks.add(task)
            }
        }
    }

    override fun getConfig(): AfAdConfig? = adConfig

    override fun setEventConfig(config: AfAdConfig) {
        this.adConfig = config
    }

    override fun cancelRequestAndShowAllAds() {
        isCancelRequest = true
        Log.d(TAG, "Canceling all requests and hiding ads across the app.")
    }

    override fun isShowAdsIntervalValid(): Boolean {
        val intervalMs = adConfig?.interstitialIntervalMs ?: 15_000L
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - previousAdCloseTime) >= intervalMs
        Log.d(TAG, "isShowAdsIntervalValid: $isValid (elapsed: ${currentTime - previousAdCloseTime}ms, required: ${intervalMs}ms)")
        return isValid
    }

    @SuppressLint("MissingPermission")
    override fun requestBannerAd(
        context: Context,
        container: ViewGroup,
        adUnitId: String,
        callback: BannerAdCallBack?
    ) {
        if (isCancelRequest) return
        runWhenInitialized {
            try {
                val displayMetrics = context.resources.displayMetrics
                val widthPixels = container.width.takeIf { it > 0 } ?: displayMetrics.widthPixels
                val adWidth = (widthPixels / displayMetrics.density).toInt()
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

                val adView = AdView(context).apply {
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                }
                container.removeAllViews()
                container.addView(adView)

                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        callback?.onAdLoaded()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Banner failed to load: ${error.message}")
                        callback?.onAdFailedToLoad(error.message)
                    }

                    override fun onAdClicked() {
                        AdmobManager.onClickAd()
                        callback?.onAdClicked()
                    }

                    override fun onAdImpression() {
                        callback?.onAdImpression()
                    }
                }
                adView.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e(TAG, "requestBannerAd exception: ${e.message}")
                callback?.onAdFailedToLoad(e.message ?: "Unknown banner load error")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdCallback
    ) {
        if (isCancelRequest) return
        runWhenInitialized {
            val builder = AdLoader.Builder(context.applicationContext, adUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "Native ad loaded: $adUnitId")
                    callback.onAdLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Native ad failed to load: ${error.message}")
                        callback.onAdFailedToLoad(error.message)
                    }

                    override fun onAdClicked() {
                        AdmobManager.onClickAd()
                    }
                })
            builder.build().loadAd(AdRequest.Builder().build())
        }
    }

    override fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        try {
            adView.headlineView = adView.findViewById(android.R.id.text1)
            adView.bodyView = adView.findViewById(android.R.id.text2)
            adView.setNativeAd(nativeAd)
        } catch (e: Exception) {
            Log.e(TAG, "populateNativeAdView error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestInterstitialAds(
        context: Context,
        adUnitId: String,
        callback: InterstitialAdRequestCallBack
    ) {
        if (isCancelRequest) return
        runWhenInitialized {
            val request = AdRequest.Builder().build()
            InterstitialAd.load(context, adUnitId, request, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    callback.onAdFailedToLoad(error.message)
                }
            })
        }
    }

    override fun showInterstitial(
        activity: Activity,
        contentAd: ContentAd?,
        callback: InterstitialAdShowCallBack
    ) {
        val interAd = contentAd?.adObject as? InterstitialAd
        if (interAd != null && isShowAdsIntervalValid()) {
            interAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdmobManager.onAdShowing()
                    callback.onAdShowed()
                }

                override fun onAdDismissedFullScreenContent() {
                    previousAdCloseTime = System.currentTimeMillis()
                    AdmobManager.onAdDismissed()
                    callback.onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    callback.onAdFailedToShow(error.message)
                }

                override fun onAdClicked() {
                    AdmobManager.onClickAd()
                }
            }
            interAd.show(activity)
        } else {
            callback.onAdFailedToShow("Ad not ready or interval cooldown active")
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestAppOpenAds(
        context: Context,
        adUnitId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        callback: AppOpenAdCallBack
    ) {
        if (isCancelRequest) return
        runWhenInitialized {
            val request = AdRequest.Builder().build()
            AppOpenAd.load(context, adUnitId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad loaded: $adPlacement")
                    ad.setOnPaidEventListener { value ->
                        val mediationAdapter = ad.responseInfo.mediationAdapterClassName.orEmpty()
                        try {
                            AdjustTrackingManager.pushTrackEventAdmob(
                                value.valueMicros,
                                value.currencyCode,
                                adPlacement,
                                mediationAdapter
                            )
                            FirebaseTrackingManager.logRevenue(
                                context.applicationContext,
                                value.currencyCode,
                                value.valueMicros,
                                adPlacement,
                                mediationAdapter.ifBlank { "admob" },
                                adUnitId,
                                "app_open"
                            )
                        } catch (error: Exception) {
                            Log.e(TAG, "Failed to track App Open Ad revenue", error)
                        }
                        if (FacebookTrackingManager.isInitialized()) {
                            try {
                                FacebookTrackingManager.logAdImpression(
                                    context.applicationContext,
                                    value.valueMicros / 1_000_000.0,
                                    adPlacement,
                                    mediationAdapter.ifBlank { "admob" },
                                    adUnitId,
                                    "app_open",
                                    EventConfig(currencyCode = value.currencyCode)
                                )
                            } catch (error: Exception) {
                                Log.e(TAG, "Failed to track Facebook App Open Ad impression", error)
                            }
                        }
                    }
                    val contentAd = ContentAd(
                        adType = "app_open",
                        adUnitId = adUnitId,
                        isLoaded = true,
                        adObject = ad
                    )
                    callback.onAdLoaded(contentAd)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${error.message}")
                    callback.onAdFailedToLoad(error)
                }
            })
        }
    }

    override fun showAppOpen(
        activity: Activity,
        contentAd: ContentAd?,
        callback: AppOpenAdCallBack
    ) {
        val appOpenAd = contentAd?.adObject as? AppOpenAd
        if (appOpenAd != null) {
            appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdmobManager.onAdShowing()
                    callback.onAppOpenAdShow()
                }

                override fun onAdDismissedFullScreenContent() {
                    previousAdCloseTime = System.currentTimeMillis()
                    AdmobManager.onAdDismissed()
                    callback.onAppOpenAdClose()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    callback.onAdFailedToShow(error)
                }

                override fun onAdClicked() {
                    AdmobManager.onClickAd()
                    callback.onAdClicked()
                }

                override fun onAdImpression() {
                    callback.onAdImpression()
                }
            }
            appOpenAd.show(activity)
        } else {
            callback.onAdFailedToShow(AdError(1999, "No ad object to show", ""))
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestRewardAd(
        context: Context,
        adUnitId: String,
        callback: RewardAdCallBack
    ) {
        if (isCancelRequest) return
        runWhenInitialized {
            val request = AdRequest.Builder().build()
            RewardedAd.load(context, adUnitId, request, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                    callback.onAdFailedToLoad(error.message)
                }
            })
        }
    }

    override fun showRewardAd(
        activity: Activity,
        contentAd: ContentAd?,
        callback: RewardAdCallBack
    ) {
        val rewardAd = contentAd?.adObject as? RewardedAd
        if (rewardAd != null) {
            rewardAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdmobManager.onAdShowing()
                }

                override fun onAdDismissedFullScreenContent() {
                    previousAdCloseTime = System.currentTimeMillis()
                    AdmobManager.onAdDismissed()
                    callback.onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    callback.onAdClosed()
                }

                override fun onAdClicked() {
                    AdmobManager.onClickAd()
                }
            }
            rewardAd.show(activity) { rewardItem ->
                callback.onUserEarnedReward(rewardItem)
            }
        } else {
            callback.onAdClosed()
        }
    }

    companion object {
        val INSTANCE: AdmobFactory by lazy { AdmobFactoryImpl() }
    }
}
