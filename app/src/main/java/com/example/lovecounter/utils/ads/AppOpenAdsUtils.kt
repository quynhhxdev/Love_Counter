package com.example.lovecounter.utils.ads

import android.app.Application
import com.ads.admob.helper.appopen.AppOpenAdConfig
import com.ads.admob.helper.appopen.AppResumeAdHelper
import com.ads.admob.helper.appopen.params.AppOpenAdParam

class AppOpenAdsUtils(
    application: Application,
    adId: String = AdPlacement.APPOPEN_RESUME_ID,
    placementKey: String = AdPlacement.DEFAULT_APP_OPEN
) {
    private val helper = AppResumeAdHelper(
        application,
        AppOpenAdConfig(
            idAds = adId,
            adPlacement = placementKey,
            param = AppOpenAdParam(adId, adPlacement = placementKey)
        )
    )

    fun fetchAd() {
        helper.fetchAd()
    }

    fun setCurrentActivity(activity: android.app.Activity?) {
        helper.setCurrentActivity(activity)
    }

    fun clearAd() {
        helper.clearAd()
    }
}
