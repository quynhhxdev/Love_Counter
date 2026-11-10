package com.ads.admob.helper.interstitial.factory.admob

import android.app.Activity
import android.content.Context
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.interstitial.InterstitialAd

interface AdmobInterstitialAdFactory {
    fun requestInterstitialAd(
        context: Context,
        param: InterstitialAdParam.Request,
        onLoaded: (InterstitialAd?) -> Unit
    )

    fun showInterstitial(
        activity: Activity,
        param: InterstitialAdParam.ShowAd,
        onShown: () -> Unit,
        onDismissed: () -> Unit,
        onFailedToShow: (AdError) -> Unit,
        onClicked: () -> Unit = {},
        onImpression: () -> Unit = {}
    )
}
