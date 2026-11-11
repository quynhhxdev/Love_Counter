package com.example.lovecounter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.example.lovecounter.utils.FirebaseConfigManager as AdsFirebaseConfigManager
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.AfAdConfig
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.event.AdjustTrackingManager
import com.ads.admob.event.FacebookTrackingManager
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.AdsInitializer
import com.example.lovecounter.utils.ads.AdsPolicyManager
import com.example.lovecounter.utils.ads.AppOpenAdsUtils
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {
    private var currentActivity: Activity? = null
    private var trackingInitializationAttempted = false

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        val firebaseApp = try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Logger.e("FirebaseApp init error: ${e.message}")
            null
        }

        AdsInitializer.initialize(this) {
            appResumeAdHelper = AppOpenAdsUtils(this@App, AdPlacement.APPOPEN_RESUME_ID)
            appResumeAdHelper?.setCurrentActivity(currentActivity)
            if (AdsPolicyManager.canLoadAd(this@App, AdsPolicyManager.AdType.APP_OPEN)) {
                appResumeAdHelper?.fetchAd()
            }
        }

        registerActivityLifecycleCallbacks(this)
    }

    @Synchronized
    fun initializeTrackingManagers() {
        if (trackingInitializationAttempted) return
        trackingInitializationAttempted = true

        if (BuildConfig.ADJUST_APP_TOKEN.isBlank()) {
            Log.w(TAG, "Adjust tracking is not initialized: ADJUST_APP_TOKEN is missing.")
        } else {
            try {
                AdjustTrackingManager.initialize(
                    this,
                    BuildConfig.ADJUST_APP_TOKEN,
                    BuildConfig.ADJUST_ENVIRONMENT
                )
            } catch (error: Exception) {
                Log.e(TAG, "Adjust tracking initialization failed.", error)
            }
        }

        if (BuildConfig.FACEBOOK_APP_ID.isBlank() || BuildConfig.FACEBOOK_CLIENT_TOKEN.isBlank()) {
            Log.w(TAG, "Facebook tracking is not initialized: app ID or client token is missing.")
        } else {
            try {
                FacebookTrackingManager.initialize(
                    this,
                    BuildConfig.FACEBOOK_APP_ID,
                    BuildConfig.FACEBOOK_CLIENT_TOKEN
                )
            } catch (error: Exception) {
                Log.e(TAG, "Facebook tracking initialization failed.", error)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val languageCode = try {
            runBlocking { CoupleProfileDataStore.readLanguageCode(this@App) }
        } catch (error: Exception) {
            Logger.e("Failed to reload saved language", throwable = error)
            "en"
        }
        LocaleHelper.onAttach(this, languageCode)
        super.onConfigurationChanged(newConfig)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        appResumeAdHelper?.setCurrentActivity(activity)
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
            appResumeAdHelper?.setCurrentActivity(null)
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private const val TAG = "LoveCounterApp"
        var context: Context? = null
        var appResumeAdHelper: AppOpenAdsUtils? = null
    }
}
