package com.example.lovecounter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.example.lovecounter.utils.FirebaseConfigManager as AdsFirebaseConfigManager
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.AfAdConfig
import com.ads.admob.cmp.ConsentManager
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.ads.AppOpenAdsUtils
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        val firebaseApp = try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Logger.e("FirebaseApp init error: ${e.message}")
            null
        }

        AdmobFactoryImpl.INSTANCE.initAdmob(this, AfAdConfig.Builder().build()) {
            appResumeAdHelper = AppOpenAdsUtils(this@App, "ca-app-pub-3940256099942544/9257395921")
            appResumeAdHelper?.setCurrentActivity(currentActivity)
            if (AdsFirebaseConfigManager.isAppOpenAdsEnabled()) {
                if (ConsentManager.canRequestAds(this@App)) {
                    appResumeAdHelper?.fetchAd()
                }
            }
        }

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
        var context: Context? = null
        var appResumeAdHelper: AppOpenAdsUtils? = null
    }
}
