package com.ads.admob.listener

import com.ads.admob.data.ContentAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface AppOpenAdCallBack {
    fun onAppOpenAdShow()
    fun onAppOpenAdClose()
    fun onAdLoaded(data: ContentAd)
    fun onAdFailedToLoad(loadAdError: LoadAdError)
    fun onAdClicked()
    fun onAdImpression()
    fun onAdFailedToShow(adError: AdError)
}
