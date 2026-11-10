package com.ads.admob.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object FirebaseConfigManager {

    private const val TAG = "FirebaseConfigManager"

    const val KEY_ADS_ENABLED = "ads_enabled"
    const val KEY_APP_OPEN_ADS_ENABLED = "app_open_ads_enabled"
    const val KEY_INTERSTITIAL_ADS_ENABLED = "interstitial_ads_enabled"
    const val KEY_NATIVE_ADS_ENABLED = "native_ads_enabled"
    const val KEY_BANNER_ADS_ENABLED = "banner_ads_enabled"
    const val KEY_REWARDED_ADS_ENABLED = "rewarded_ads_enabled"

    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            val defaults = mapOf(
                KEY_ADS_ENABLED to true,
                KEY_APP_OPEN_ADS_ENABLED to true,
                KEY_INTERSTITIAL_ADS_ENABLED to true,
                KEY_NATIVE_ADS_ENABLED to true,
                KEY_BANNER_ADS_ENABLED to true,
                KEY_REWARDED_ADS_ENABLED to true
            )
            remoteConfig.setDefaultsAsync(defaults)

            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    Log.d(TAG, "RemoteConfig fetch result: ${task.isSuccessful}")
                    onComplete?.invoke(task.isSuccessful)
                }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseConfigManager error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    fun isAdsEnabled(): Boolean {
        return try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }

    fun isAppOpenAdsEnabled(): Boolean {
        return isAdsEnabled() && try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_APP_OPEN_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }

    fun isInterstitialAdsEnabled(): Boolean {
        return isAdsEnabled() && try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_INTERSTITIAL_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }

    fun isNativeAdsEnabled(): Boolean {
        return isAdsEnabled() && try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_NATIVE_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }

    fun isBannerAdsEnabled(): Boolean {
        return isAdsEnabled() && try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_BANNER_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }

    fun isRewardedAdsEnabled(): Boolean {
        return isAdsEnabled() && try {
            FirebaseRemoteConfig.getInstance().getBoolean(KEY_REWARDED_ADS_ENABLED)
        } catch (e: Exception) {
            true
        }
    }
}
