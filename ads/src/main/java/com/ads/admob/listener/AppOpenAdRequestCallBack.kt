package com.ads.admob.listener

import com.ads.admob.data.ContentAd
import com.google.android.gms.ads.LoadAdError

interface AppOpenAdRequestCallBack {
    fun onAdLoaded(data: ContentAd)
    fun onAdFailedToLoad(loadAdError: LoadAdError)
}
