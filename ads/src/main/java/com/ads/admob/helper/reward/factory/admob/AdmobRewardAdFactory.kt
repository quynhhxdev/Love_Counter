package com.ads.admob.helper.reward.factory.admob

import android.app.Activity
import android.content.Context
import com.ads.admob.helper.reward.RewardAdParam
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

interface AdmobRewardAdFactory {
    fun requestRewardAd(
        context: Context,
        param: RewardAdParam.Request,
        onLoaded: (RewardedAd?, LoadAdError?) -> Unit
    )

    fun requestRewardInterAd(
        context: Context,
        param: RewardAdParam.Request,
        onLoaded: (RewardedInterstitialAd?, LoadAdError?) -> Unit
    )

    fun showRewardAd(
        activity: Activity,
        param: RewardAdParam.ShowAd,
        onShown: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: (AdError) -> Unit,
        onEarnedReward: (RewardItem) -> Unit,
        onClicked: () -> Unit = {},
        onImpression: () -> Unit = {}
    )
}
