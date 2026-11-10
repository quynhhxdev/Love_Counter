package com.ads.admob.helper.interstitial

import com.ads.admob.helper.interstitial.params.InterstitialAdParam

data class InterstitialAdSplashConfig(
    val param: InterstitialAdParam.Request,
    val timeOut: Long = 3_000L,
    val timeDelay: Long = 0L,
    val showReady: Boolean = true,
    val holdShow: Boolean = true,
    val adPlacement: String = param.adPlacement
) {
    val idAds: String get() = param.adUnitId
    val idAdsPriority: String? get() = param.fallbackAdUnitId

    init {
        require(timeOut >= 0L) { "Splash interstitial timeout must not be negative" }
        require(timeDelay >= 0L) { "Splash interstitial delay must not be negative" }
    }
}
