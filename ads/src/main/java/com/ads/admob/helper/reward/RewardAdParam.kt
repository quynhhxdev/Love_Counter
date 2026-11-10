package com.ads.admob.helper.reward

import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.ads.admob.helper.params.IAdsParam

sealed class RewardAdParam : IAdsParam {
    data class Request(
        val adUnitId: String,
        val fallbackAdUnitId: String? = null,
        val adPlacement: String = "reward"
    ) : RewardAdParam()

    data class Show(val adPlacement: String = "reward") : RewardAdParam()

    sealed class ShowAd : RewardAdParam() {
        data class Rewarded(
            val ad: RewardedAd,
            val adPlacement: String = "reward"
        ) : ShowAd()

        data class RewardedInterstitial(
            val ad: RewardedInterstitialAd,
            val adPlacement: String = "reward"
        ) : ShowAd()
    }

    data class Clickable(val adPlacement: String = "reward") : RewardAdParam()

    companion object {
        operator fun invoke(
            adUnitId: String,
            fallbackAdUnitId: String? = null,
            adPlacement: String = "reward"
        ): Request = Request(adUnitId, fallbackAdUnitId, adPlacement)
    }
}
