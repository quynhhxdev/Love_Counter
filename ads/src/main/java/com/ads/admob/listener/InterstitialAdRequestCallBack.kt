package com.ads.admob.listener

interface InterstitialAdRequestCallBack {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
}
