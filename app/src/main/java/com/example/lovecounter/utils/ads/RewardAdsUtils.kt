package com.example.lovecounter.utils.ads

import android.app.Activity
import com.ads.admob.helper.reward.RewardAdConfig
import com.ads.admob.helper.reward.RewardAdHelper
import com.ads.admob.helper.reward.RewardAdParam
import com.ads.admob.listener.RewardAdCallBack
import com.google.android.gms.ads.rewarded.RewardItem

object RewardAdsUtils {
    fun loadRewardAd(activity: Activity, adId: String, fallbackAdId: String? = null, onLoaded: ((Boolean) -> Unit)? = null) {
        val helper = RewardAdHelper.getInstance(
            RewardAdConfig("default_reward", RewardAdParam(adId, fallbackAdId))
        )
        helper.loadAd(activity, object : RewardAdCallBack {
            override fun onAdLoaded() { onLoaded?.invoke(true) }
            override fun onAdFailedToLoad(error: String) { onLoaded?.invoke(false) }
            override fun onUserEarnedReward(rewardItem: RewardItem) {}
            override fun onAdClosed() {}
        })
    }

    fun showRewardAd(activity: Activity, onRewardGranted: () -> Unit) {
        val helper = RewardAdHelper.getInstance(
            RewardAdConfig("default_reward", RewardAdParam(AdPlacement.REWARD_TEST))
        )
        helper.showAd(activity, object : RewardAdCallBack {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(error: String) {}
            override fun onUserEarnedReward(rewardItem: RewardItem) {
                onRewardGranted()
            }
            override fun onAdClosed() {}
        })
    }
}
