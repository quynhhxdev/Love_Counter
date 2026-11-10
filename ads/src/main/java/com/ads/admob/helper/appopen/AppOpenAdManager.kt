package com.ads.admob.helper.appopen

import android.content.Context
import android.util.Log
import com.ads.admob.event.AdjustTrackingManager
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.helper.adnative.params.AdNativeMediation
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object AppOpenAdManager {
    private const val TAG = "AppOpenAdManager"
    const val AD_CACHE_DURATION_MS = 4L * 60L * 60L * 1000L

    var isShowingAppOpenAd = AtomicBoolean(false)
    private val disabledActivities = ConcurrentHashMap.newKeySet<Class<*>>()
    private val cachedAds = ConcurrentHashMap<String, CachedAd>()

    private data class CachedAd(
        val ad: AppOpenAd,
        val loadedAtMillis: Long
    )

    fun cacheAd(
        context: Context,
        ad: AppOpenAd,
        adPlacement: String,
        adUnitId: String,
        provider: AdNativeMediation = AdNativeMediation.ADMOB
    ) {
        ad.setOnPaidEventListener { value ->
            val mediationAdapter = ad.responseInfo.mediationAdapterClassName
                ?.takeIf { it.isNotBlank() }
                ?: provider.name.lowercase()
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
                    mediationAdapter,
                    adUnitId,
                    "app_open"
                )
            } catch (error: Exception) {
                Log.e(TAG, "Failed to track App Open Ad revenue", error)
            }
        }
        cachedAds[adPlacement] = CachedAd(ad, System.currentTimeMillis())
    }

    fun isAdAvailable(adPlacement: String = "app_open"): Boolean = synchronized(cachedAds) {
        val cached = cachedAds[adPlacement] ?: return@synchronized false
        if (System.currentTimeMillis() - cached.loadedAtMillis >= AD_CACHE_DURATION_MS) {
            cachedAds.remove(adPlacement)
            return@synchronized false
        }
        true
    }

    fun takeAd(adPlacement: String = "app_open"): AppOpenAd? = synchronized(cachedAds) {
        if (!isAdAvailable(adPlacement)) return@synchronized null
        cachedAds.remove(adPlacement)?.ad
    }

    fun clearAd(adPlacement: String = "app_open") {
        cachedAds.remove(adPlacement)
    }

    fun disableAppOpenAdForActivity(activityClass: Class<*>) {
        disabledActivities.add(activityClass)
    }

    fun enableAppOpenAdForActivity(activityClass: Class<*>) {
        disabledActivities.remove(activityClass)
    }

    fun isActivityDisabled(activityClass: Class<*>): Boolean =
        disabledActivities.contains(activityClass)
}
