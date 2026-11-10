package com.ads.admob.listener

import com.ads.admob.helper.banner.params.BannerAdParam

interface BannerAdCallBack {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
    fun onAdClicked()
    fun onAdImpression()

    fun onAdReady(ready: BannerAdParam.Ready) = Unit
    fun onAdClick(clickable: BannerAdParam.Clickable) = onAdClicked()
}
