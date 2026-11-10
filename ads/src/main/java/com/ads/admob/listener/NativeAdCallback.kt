package com.ads.admob.listener

import com.google.android.gms.ads.nativead.NativeAd

interface NativeAdCallback {
    fun onAdLoaded(nativeAd: NativeAd)
    fun onAdFailedToLoad(error: String)
}
