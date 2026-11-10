package com.ads.admob.helper.appopen.factory

import android.app.Activity
import android.app.Application
import android.util.Log
import com.ads.admob.AdmobManager
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.appopen.AppOpenAdManager
import com.ads.admob.helper.appopen.params.AppOpenAdParam
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AdmobAppOpenAdFactoryImpl(private val application: Application) : AdmobAppOpenAdFactory {
    private val tag = "AdmobAppOpenFactory"

    override fun requestAppOpenAd(param: AppOpenAdParam.Request, onLoaded: (Boolean) -> Unit) {
        if (!FirebaseConfigManager.isAppOpenAdsEnabled()) {
            AppOpenAdManager.clearAd(param.adPlacement)
            onLoaded(false)
            return
        }
        if (AppOpenAdManager.isAdAvailable(param.adPlacement)) {
            onLoaded(true)
            return
        }
        loadInternal(param, param.adUnitId, param.fallbackAdUnitId, onLoaded)
    }

    private fun loadInternal(
        param: AppOpenAdParam.Request,
        adUnitId: String,
        fallbackAdUnitId: String?,
        onLoaded: (Boolean) -> Unit
    ) {
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AppOpenAdManager.cacheAd(
                        application,
                        ad,
                        param.adPlacement,
                        adUnitId,
                        param.provider
                    )
                    Log.d(tag, "App Open Ad loaded: ${param.adPlacement}")
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(tag, "App Open Ad failed to load $adUnitId: ${error.message}")
                    val fallback = fallbackAdUnitId?.takeIf {
                        it.isNotBlank() && it != adUnitId
                    }
                    if (fallback != null) {
                        loadInternal(param, fallback, null, onLoaded)
                    } else {
                        AppOpenAdManager.clearAd(param.adPlacement)
                        onLoaded(false)
                    }
                }
            }
        )
    }

    override fun showAppOpen(
        activity: Activity,
        param: AppOpenAdParam.Show,
        onClosed: () -> Unit,
        onShown: () -> Unit,
        onImpression: () -> Unit,
        onClicked: () -> Unit,
        onFailedToShow: (AdError) -> Unit
    ) {
        val ad = AppOpenAdManager.takeAd(param.adPlacement)
        if (ad == null) {
            onFailedToShow(AdError(1999, "App Open Ad is not available", tag))
            onClosed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AppOpenAdManager.isShowingAppOpenAd.set(true)
                AdmobManager.onAdShowing()
                onShown()
            }

            override fun onAdDismissedFullScreenContent() {
                AppOpenAdManager.isShowingAppOpenAd.set(false)
                AdmobManager.onAdDismissed()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AppOpenAdManager.isShowingAppOpenAd.set(false)
                AdmobManager.onAdDismissed()
                onFailedToShow(error)
                onClosed()
            }

            override fun onAdImpression() = onImpression()

            override fun onAdClicked() {
                AdmobManager.onClickAd()
                onClicked()
            }
        }
        try {
            ad.show(activity)
        } catch (error: Exception) {
            AppOpenAdManager.isShowingAppOpenAd.set(false)
            AdmobManager.onAdDismissed()
            val adError = AdError(1999, error.message ?: "Unable to show App Open Ad", tag)
            onFailedToShow(adError)
            onClosed()
        }
    }

    override fun clearAd(adPlacement: String) {
        AppOpenAdManager.clearAd(adPlacement)
    }
}
