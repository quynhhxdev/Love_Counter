package com.example.lovecounter.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object FirebaseConfigManager {

    private const val KEY_AD_CONFIG = "ad_config"

    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    Logger.d("RemoteConfig fetch result: ${task.isSuccessful}")
                    onComplete?.invoke(task.isSuccessful)
                }
        } catch (e: Exception) {
            Logger.e("FirebaseConfigManager error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    fun getAdConfigJson(): String {
        return try {
            FirebaseRemoteConfig.getInstance().getString(KEY_AD_CONFIG)
        } catch (e: Exception) {
            ""
        }
    }
}
