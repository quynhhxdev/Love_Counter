package com.ads.admob.helper.reward

data class RewardAdConfig(
    val placementKey: String,
    val param: RewardAdParam.Request,
    val showByTime: Long = 15_000L,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true,
    val adPlacement: String = placementKey
) {
    val idAds: String get() = param.adUnitId
    val idAdsPriority: String? get() = param.fallbackAdUnitId

    init {
        require(showByTime >= 0L) { "Reward show interval must not be negative" }
    }
}
