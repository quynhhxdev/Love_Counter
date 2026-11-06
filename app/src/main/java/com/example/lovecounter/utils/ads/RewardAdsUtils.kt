package com.example.lovecounter.utils.ads

import android.app.Activity
import com.example.lovecounter.utils.Logger
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardAdsUtils {

    private var rewardedAd: RewardedAd? = null

    fun loadRewardAd(activity: Activity, adId: String, onLoaded: ((Boolean) -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, adId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                Logger.d("Reward ad loaded")
                onLoaded?.invoke(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                Logger.e("Reward ad failed to load: ${error.message}")
                onLoaded?.invoke(false)
            }
        })
    }

    fun showRewardAd(activity: Activity, onRewardGranted: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                Logger.d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardGranted()
            }
            rewardedAd = null
        } else {
            onRewardGranted()
        }
    }
}
