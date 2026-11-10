package com.ads.admob.listener

import com.google.android.gms.ads.rewarded.RewardItem

interface RewardAdCallBack {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
    fun onUserEarnedReward(rewardItem: RewardItem)
    fun onAdClosed()

    fun onAdShowed() = Unit
    fun onAdFailedToShow(error: String) = onAdClosed()
    fun onAdClicked() = Unit
    fun onAdClick(param: com.ads.admob.helper.reward.RewardAdParam.Clickable) = onAdClicked()
    fun onAdImpression() = Unit
}
