package com.ads.admob.helper.interstitial

import com.ads.admob.helper.interstitial.params.InterstitialAdParam

data class InterstitialAdConfig(
    val placementKey: String,
    val param: InterstitialAdParam.Request,
    val showByTime: Long = 15_000L,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true,
    val adPlacement: String = placementKey
) {
    val idAds: String get() = param.adUnitId
    val idAdsPriority: String? get() = param.fallbackAdUnitId

    init {
        require(showByTime >= 0L) { "Interstitial show interval must not be negative" }
    }
}
