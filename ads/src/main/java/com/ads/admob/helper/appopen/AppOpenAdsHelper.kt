package com.ads.admob.helper.appopen

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.AdmobManager
import com.ads.admob.data.ContentAd
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.appopen.params.AppOpenAdParam
import com.ads.admob.helper.appopen.params.AdAppOpenState
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.AppOpenAdRequestCallBack
import com.ads.admob.listener.AppOpenAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppOpenAdsHelper private constructor(private val adPlacement: String) :
    AdsHelper<AppOpenAdParam>() {
    private var appOpenAdConfig: AppOpenAdConfig =
        AppOpenAdConfig(idAds = "", canShowAds = true, canReloadAds = true, adPlacement = adPlacement)

    fun setAppOpenAdConfig(config: AppOpenAdConfig) {
        appOpenAdConfig = config
        canReloadAd = config.canReloadAds
    }

    private var adAppOpenState: AdAppOpenState = AdAppOpenState.NONE

    fun AdAppOpenState.emit(state: AdAppOpenState) = apply {
        adAppOpenState = state
    }

    private var loadingJob: Job? = null
    var appOpenAdValue: ContentAd? = null
        private set
    private var loadingAdsDialog: LoadingAdsDialog? = null
    private var isCancelRequestAndShowAllAds = false

    fun cancelRequestAndShowAllAds(isPurchased: Boolean) {
        adAppOpenState.emit(if (isPurchased) AdAppOpenState.CANCEL else AdAppOpenState.NONE)
        appOpenAdValue = null
        isCancelRequestAndShowAllAds = isPurchased
        if (isPurchased) {
            synchronized(AppOpenAdsHelper::class.java) {
                instances.remove(adPlacement)
            }
        }
    }

    private fun requestAdsAlternate(
        activity: Context,
        idAdPriority: String,
        idAdNormal: String,
        appOpenAdCallback: AppOpenAdCallBack
    ) {
        Log.d(TAG, "requestAdsAlternate: ")
        AdmobFactoryImpl.INSTANCE
            .requestAppOpenAds(
                activity,
                idAdPriority,
                appOpenAdConfig.adPlacement,
                false,
                object : AppOpenAdCallBack {
                    override fun onAppOpenAdShow() {
                        appOpenAdCallback.onAppOpenAdShow()
                    }

                    override fun onAppOpenAdClose() {
                        appOpenAdCallback.onAppOpenAdClose()
                    }

                    override fun onAdLoaded(data: ContentAd) {
                        appOpenAdValue = data
                        appOpenAdCallback.onAdLoaded(data)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AdmobFactoryImpl.INSTANCE
                            .requestAppOpenAds(
                                activity,
                                idAdNormal,
                                appOpenAdConfig.adPlacement,
                                appOpenAdConfig.reloadIfFirstFail,
                                object : AppOpenAdCallBack {
                                    override fun onAppOpenAdShow() {
                                        appOpenAdCallback.onAppOpenAdShow()
                                    }

                                    override fun onAppOpenAdClose() {
                                        appOpenAdCallback.onAppOpenAdClose()
                                    }

                                    override fun onAdLoaded(data: ContentAd) {
                                        appOpenAdCallback.onAdLoaded(data)
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        appOpenAdCallback.onAdFailedToLoad(loadAdError)
                                    }

                                    override fun onAdClicked() {
                                        appOpenAdCallback.onAdClicked()
                                    }

                                    override fun onAdImpression() {
                                        appOpenAdCallback.onAdImpression()
                                    }

                                    override fun onAdFailedToShow(adError: AdError) {
                                        appOpenAdCallback.onAdFailedToShow(adError)
                                    }

                                })
                    }

                    override fun onAdClicked() {
                        appOpenAdCallback.onAdClicked()
                    }

                    override fun onAdImpression() {
                        appOpenAdCallback.onAdImpression()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        appOpenAdCallback.onAdFailedToShow(adError)
                    }

                })
    }

    fun requestAppOpenAds(
        activity: Activity,
        isLoadAndShow: Boolean = false,
        appOpenAdRequestCallBack: AppOpenAdRequestCallBack
    ) {
        if (canRequestAppOpenAds(activity)) {
            if (requestValid(activity)) {
                Log.e(TAG, "requestAppOpenAds: $adPlacement")
                adAppOpenState.emit(AdAppOpenState.LOADING)
                if (isLoadAndShow) showDialogLoading(activity)
                if (appOpenAdConfig.idAdsPriority != null) {
                    requestAdsAlternate(
                        activity,
                        appOpenAdConfig.idAdsPriority!!,
                        appOpenAdConfig.idAds,
                        object : AppOpenAdCallBack {
                            override fun onAppOpenAdShow() {
                                adAppOpenState.emit(AdAppOpenState.SHOWED)
                            }

                            override fun onAppOpenAdClose() {

                            }

                            override fun onAdLoaded(data: ContentAd) {
                                appOpenAdValue = data
                                adAppOpenState.emit(AdAppOpenState.LOADED)
                                appOpenAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adAppOpenState.emit(AdAppOpenState.FAIL)
                                dismissDialog()
                                appOpenAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adAppOpenState.emit(AdAppOpenState.SHOWED)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                        })
                } else {
                    AdmobFactoryImpl.INSTANCE.requestAppOpenAds(
                        activity,
                        appOpenAdConfig.idAds,
                        appOpenAdConfig.adPlacement,
                        appOpenAdConfig.reloadIfFirstFail,
                        object : AppOpenAdCallBack {
                            override fun onAppOpenAdShow() {
                                adAppOpenState.emit(AdAppOpenState.SHOWED)
                            }

                            override fun onAppOpenAdClose() {

                            }

                            override fun onAdLoaded(data: ContentAd) {
                                appOpenAdValue = data
                                adAppOpenState.emit(AdAppOpenState.LOADED)
                                appOpenAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adAppOpenState.emit(AdAppOpenState.FAIL)
                                dismissDialog()
                                appOpenAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adAppOpenState.emit(AdAppOpenState.SHOWED)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                        })
                }
            } else {
                Log.e(TAG, "requestAppOpenAds $adPlacement: Invalid")
            }
        } else {
            Log.e(TAG, "requestAppOpenAds $adPlacement: canRequestAds = false")
            adAppOpenState.emit(AdAppOpenState.FAIL)
            appOpenAdRequestCallBack.onAdFailedToLoad(
                LoadAdError(
                    99,
                    "Request Invalid",
                    "",
                    null,
                    null
                )
            )
        }
    }

    private suspend fun waitForAdReady(timeoutMs: Long = 1200L): Boolean {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (
                adAppOpenState == AdAppOpenState.LOADED &&
                appOpenAdValue != null
            ) {
                return true
            }
            delay(50)
        }

        return false
    }

    fun forceShowAppOpen(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        appOpenAdShowCallBack: AppOpenAdShowCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            failShow(appOpenAdShowCallBack, "App Open Ads were disabled")
            return
        }
        if (adAppOpenState != AdAppOpenState.LOADING && adAppOpenState != AdAppOpenState.LOADED) {
            failShow(appOpenAdShowCallBack, "App Open Ad is not loading or ready")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            failShow(appOpenAdShowCallBack, "Activity is not available")
            return
        }

        cancelLoadingJob()
        loadingJob = lifecycleOwner.lifecycleScope.launch {
            if (adAppOpenState == AdAppOpenState.LOADING) showDialogLoading(activity)
            val ready = waitForAdReady(1200L)
            dismissDialog()
            if (!ready || appOpenAdValue == null || activity.isFinishing || activity.isDestroyed) {
                failShow(appOpenAdShowCallBack, "App Open Ad did not become ready")
                return@launch
            }
            AdmobManager.adsShowFullScreen()
            showAppOpenInternal(activity, appOpenAdShowCallBack)
        }
    }

    private fun failShow(callback: AppOpenAdShowCallBack, message: String) {
        adAppOpenState.emit(AdAppOpenState.SHOW_FAIL)
        dismissDialog()
        AdmobManager.adsFullScreenDismiss()
        callback.onAdFailedToShow(AdError(1999, message, "AppOpenAdsHelper"))
        callback.onNextAction()
    }

    private fun showAppOpenInternal(
        activity: Activity,
        callback: AppOpenAdShowCallBack
    ) {
        AdmobFactoryImpl.INSTANCE.showAppOpen(
            activity,
            appOpenAdValue,
            object : AppOpenAdCallBack {
                override fun onAppOpenAdShow() {
                    callback.onAppOpenShow()
                    adAppOpenState.emit(AdAppOpenState.SHOWED)
                }

                override fun onAppOpenAdClose() {
                    AdmobManager.adsFullScreenDismiss()
                    appOpenAdValue = null
                    adAppOpenState.emit(AdAppOpenState.NONE)
                    callback.onNextAction()
                    cancelLoadingJob()
                    dismissDialog()
                    if (canReloadAd && canRequestAppOpenAds(activity)) {
                        requestAppOpenAds(activity, appOpenAdRequestCallBack = noOpRequestCallback())
                    }
                }

                override fun onAdLoaded(data: ContentAd) = Unit

                override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit

                override fun onAdClicked() = AdmobManager.onClickAd()

                override fun onAdImpression() {
                    adAppOpenState.emit(AdAppOpenState.SHOWED)
                }

                override fun onAdFailedToShow(adError: AdError) {
                    appOpenAdValue = null
                    cancelLoadingJob()
                    dismissDialog()
                    AdmobManager.adsFullScreenDismiss()
                    callback.onAdFailedToShow(adError)
                    callback.onNextAction()
                    adAppOpenState.emit(AdAppOpenState.SHOW_FAIL)
                    if (canReloadAd && canRequestAppOpenAds(activity)) {
                        requestAppOpenAds(activity, appOpenAdRequestCallBack = noOpRequestCallback())
                    }
                }
            }
        )
    }

    private fun noOpRequestCallback() = object : AppOpenAdRequestCallBack {
        override fun onAdLoaded(data: ContentAd) = Unit
        override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
    }

    private fun cancelLoadingJob() {
        loadingJob?.cancel()
        loadingJob = null
    }

    private fun dismissDialog() {
        try {
            val activity = loadingAdsDialog?.ownerActivity
            if (activity != null && !activity.isDestroyed && activity.windowManager != null) {
                loadingAdsDialog?.dismiss()
                loadingAdsDialog = null
            }
        } catch (_: Exception) {
        }
    }

    private fun showDialogLoading(context: Activity) {
        if (context.isFinishing || context.isDestroyed) return
        try {
            if (loadingAdsDialog == null) {
                loadingAdsDialog = LoadingAdsDialog(context)
            }
            loadingAdsDialog?.setOwnerActivity(context)
            loadingAdsDialog?.show()
        } catch (_: Exception) {
        }

    }

    private fun requestValid(activity: Activity?): Boolean {
        val valueValid =
            (appOpenAdValue == null
                    && (adAppOpenState != AdAppOpenState.LOADING && adAppOpenState != AdAppOpenState.LOADED)
                    )
                    || adAppOpenState == AdAppOpenState.SHOWED
        return canRequestAppOpenAds(activity) && valueValid
    }

    private fun canRequestAppOpenAds(activity: Activity?): Boolean =
        activity != null &&
            canRequestAds(
                activity,
                appOpenAdConfig.canShowAds && !isCancelRequestAndShowAllAds
            )

    companion object {
        private val TAG = AppOpenAdsHelper::class.simpleName

        private val instances = mutableMapOf<String, AppOpenAdsHelper>()

        @Synchronized
        fun getInstance(adPlacement: String): AppOpenAdsHelper {
            return instances.getOrPut(adPlacement) { AppOpenAdsHelper(adPlacement) }
        }
    }
}
