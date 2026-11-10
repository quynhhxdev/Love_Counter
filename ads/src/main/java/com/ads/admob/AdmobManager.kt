package com.ads.admob

import java.util.concurrent.atomic.AtomicBoolean

object AdmobManager {
    var isFullScreenAdShowing = AtomicBoolean(false)
    var isClickAd = AtomicBoolean(false)

    fun onAdShowing() {
        isFullScreenAdShowing.set(true)
    }

    fun onAdDismissed() {
        isFullScreenAdShowing.set(false)
    }

    fun adsShowFullScreen() {
        isFullScreenAdShowing.set(true)
    }

    fun adsFullScreenDismiss() {
        isFullScreenAdShowing.set(false)
    }

    fun onClickAd() {
        isClickAd.set(true)
    }

    fun resetClickState() {
        isClickAd.set(false)
    }
}
