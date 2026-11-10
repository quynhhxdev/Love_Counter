package com.ads.admob.helper.interstitial.params

import com.google.android.gms.ads.interstitial.InterstitialAd
import android.view.View
import com.ads.admob.helper.params.IAdsParam

sealed class InterstitialAdParam : IAdsParam {
    data class Request(
        val adUnitId: String,
        val fallbackAdUnitId: String? = null,
        val adPlacement: String = "interstitial"
    ) : InterstitialAdParam()

    data class Show(val adPlacement: String = "interstitial") : InterstitialAdParam()

    data class ShowAd(
        val ad: InterstitialAd,
        val adPlacement: String = "interstitial"
    ) : InterstitialAdParam()

    data class Clickable(val view: View, val adPlacement: String = "interstitial") : InterstitialAdParam()

    companion object {
        operator fun invoke(
            adUnitId: String,
            fallbackAdUnitId: String? = null,
            adPlacement: String = "interstitial"
        ): Request = Request(adUnitId, fallbackAdUnitId, adPlacement)
    }
}
