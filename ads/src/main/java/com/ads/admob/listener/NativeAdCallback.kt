package com.ads.admob.listener

import com.google.android.gms.ads.nativead.NativeAd

interface NativeAdCallback {
    fun onAdLoaded(nativeAd: NativeAd)
    fun onAdFailedToLoad(error: String)
    fun onAdFailedToLoad(error: String, errorCode: Int) {
        onAdFailedToLoad(error)
    }
}
