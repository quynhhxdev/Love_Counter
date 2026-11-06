package com.example.lovecounter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.utils.FirebaseConfigManager
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.ads.AppOpenAdsUtils
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // 1. init AdMob
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            Logger.e("MobileAds init error: ${e.message}")
        }

        // 2. Initialize Firebase only when its app configuration is available.
        val firebaseApp = try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Logger.e("FirebaseApp init error: ${e.message}")
            null
        }

        // 3. Remote Config requires a configured default FirebaseApp.
        if (firebaseApp != null) {
            FirebaseConfigManager.fetchAndActivate()
        } else {
            Logger.i("Firebase is not configured; skipping Remote Config initialization")
        }

        // 4. Register ActivityLifecycleCallbacks
        registerActivityLifecycleCallbacks(this)
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
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        var context: Context? = null
        var appResumeAdHelper: AppOpenAdsUtils? = null
    }
}
