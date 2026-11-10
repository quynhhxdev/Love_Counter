package com.ads.admob.helper.appopen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.admob.AdmobManager
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.appopen.factory.AdmobAppOpenAdFactoryImpl
import com.ads.admob.helper.appopen.params.AppOpenAdParam

open class AppResumeAdHelper(
    private val application: Application,
    private val config: AppResumeAdConfig
) : AdsHelper<AppOpenAdParam>(), Application.ActivityLifecycleCallbacks {

    constructor(application: Application, config: AppOpenAdConfig) : this(
        application,
        AppResumeAdConfig(
            param = config.param,
            loadOnResume = true,
            canShowAds = config.canShowAds,
            canReloadAds = config.canReloadAds
        )
    )

    private val factory = AdmobAppOpenAdFactoryImpl(application)
    private var currentActivity: Activity? = null
    private var isShowing = false
    private var isAdLoaded = false
    private var isLoading = false
    private var shouldShowOnForeground = false
    private var hasBeenBackgrounded = false
    private var requestGeneration = 0

    init {
        canReloadAd = config.canReloadAds
        observeLifecycleOwner(ProcessLifecycleOwner.get())
        application.registerActivityLifecycleCallbacks(this)
    }

    fun fetchAd() {
        if (!adsAllowed() || isLoading || isAdLoaded) return
        isLoading = true
        val generation = ++requestGeneration
        val request = config.param.copy(provider = config.provider)
        factory.requestAppOpenAd(request) { loaded ->
            if (generation != requestGeneration) {
                factory.clearAd(request.adPlacement)
                return@requestAppOpenAd
            }
            isLoading = false
            isAdLoaded = loaded
            if (loaded) showIfReady()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isShowing) {
            hasBeenBackgrounded = false
            shouldShowOnForeground = false
            return
        }

        val isResume = hasBeenBackgrounded
        hasBeenBackgrounded = false
        if (!adsAllowed()) {
            shouldShowOnForeground = false
            isAdLoaded = false
            factory.clearAd(config.param.adPlacement)
            return
        }
        if (!isResume) {
            shouldShowOnForeground = false
            fetchAd()
            return
        }

        val wasClicked = AdmobManager.isClickAd.getAndSet(false)
        if (AdmobManager.isFullScreenAdShowing.get() || wasClicked) {
            shouldShowOnForeground = false
            if (config.loadOnResume) fetchAd()
            return
        }
        shouldShowOnForeground = true
        if (isCurrentActivityInvalid()) {
            shouldShowOnForeground = false
            return
        }
        if (isAdLoaded) {
            showIfReady()
        } else if (config.loadOnResume) {
            fetchAd()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (!isShowing) hasBeenBackgrounded = true
        shouldShowOnForeground = false
    }

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
        if (activity != null) showIfReady()
    }

    fun clearAd() {
        requestGeneration++
        isLoading = false
        isAdLoaded = false
        shouldShowOnForeground = false
        factory.clearAd(config.param.adPlacement)
    }

    fun dispose() {
        clearAd()
        currentActivity = null
        detachLifecycleOwner()
        application.unregisterActivityLifecycleCallbacks(this)
    }

    private fun adsAllowed(): Boolean =
        canRequestAds(application, config.canShowAds) && FirebaseConfigManager.isAppOpenAdsEnabled()

    private fun isCurrentActivityInvalid(): Boolean {
        val activity = currentActivity ?: return false
        return AppOpenAdManager.isActivityDisabled(activity.javaClass) ||
            config.listClassInValid.any { it.isAssignableFrom(activity.javaClass) }
    }

    private fun showIfReady() {
        if (!shouldShowOnForeground || !isAdLoaded || isShowing) return
        val activity = currentActivity ?: return
        if (activity.isFinishing || activity.isDestroyed ||
            !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) return
        if (isCurrentActivityInvalid() || !adsAllowed()) {
            shouldShowOnForeground = false
            return
        }
        val wasClicked = AdmobManager.isClickAd.getAndSet(false)
        if (AdmobManager.isFullScreenAdShowing.get() || wasClicked) {
            shouldShowOnForeground = false
            return
        }

        shouldShowOnForeground = false
        isAdLoaded = false
        isShowing = true
        AppOpenAdManager.isShowingAppOpenAd.set(true)
        factory.showAppOpen(
            activity = activity,
            param = AppOpenAdParam.Show(config.param.adPlacement),
            onClosed = {
                isShowing = false
                AppOpenAdManager.isShowingAppOpenAd.set(false)
                if (canReloadAd && adsAllowed()) fetchAd()
            },
            onFailedToShow = {
                isShowing = false
                AppOpenAdManager.isShowingAppOpenAd.set(false)
                if (canReloadAd && adsAllowed()) fetchAd()
            }
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        showIfReady()
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }
}
