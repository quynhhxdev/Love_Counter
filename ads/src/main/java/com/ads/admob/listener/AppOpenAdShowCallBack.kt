package com.ads.admob.listener

import com.google.android.gms.ads.AdError

interface AppOpenAdShowCallBack {
    fun onAppOpenShow()
    fun onNextAction()
    fun onAdFailedToShow(adError: AdError)
}
