package com.ads.admob.listener

interface InterstitialAdShowCallBack {
    fun onAdShowed()
    fun onAdDismissed()
    fun onAdFailedToShow(error: String)
}
