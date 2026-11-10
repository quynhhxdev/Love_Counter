package com.example.lovecounter.utils.ads

import android.content.Context
import com.ads.admob.cmp.ConsentManager
import com.example.lovecounter.utils.FirebaseConfigManager

/**
 * AdsPolicyManager
 *
 * Quản lý chính sách hiển thị quảng cáo tập trung.
 * Kiểm tra đầy đủ: Ads SDK, Remote Config (Ads Enabled), và Consent (UMP).
 */
object AdsPolicyManager {

    enum class AdType {
        BANNER, INTERSTITIAL, NATIVE, REWARDED, APP_OPEN
    }

    /**
     * Kiểm tra xem loại quảng cáo cụ thể có được phép request/load không.
     */
    fun canLoadAd(context: Context, adType: AdType): Boolean {
        // 1. Kiểm tra SDK đã sẵn sàng chưa
        if (AdsInitializer.state != AdsInitializer.SdkState.INITIALIZED) {
            return false
        }

        // 2. Kiểm tra Firebase Remote Config
        return isAdEnabled(adType) && ConsentManager.canRequestAds(context)
    }

    /**
     * Kiểm tra xem loại quảng cáo cụ thể có được phép show không.
     */
    fun canShowAd(context: Context, adType: AdType): Boolean {
        return isAdEnabled(adType)
    }

    private fun isAdEnabled(adType: AdType): Boolean {
        return when (adType) {
            AdType.BANNER -> FirebaseConfigManager.isBannerAdsEnabled()
            AdType.INTERSTITIAL -> FirebaseConfigManager.isInterstitialAdsEnabled()
            AdType.NATIVE -> FirebaseConfigManager.isNativeAdsEnabled()
            AdType.REWARDED -> FirebaseConfigManager.isRewardedAdsEnabled()
            AdType.APP_OPEN -> FirebaseConfigManager.isAppOpenAdsEnabled()
        }
    }
}
