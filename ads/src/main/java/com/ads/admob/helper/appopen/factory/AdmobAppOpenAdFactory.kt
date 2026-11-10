package com.ads.admob.helper.appopen.factory

import android.app.Activity
import com.ads.admob.helper.appopen.params.AppOpenAdParam
import com.google.android.gms.ads.AdError

interface AdmobAppOpenAdFactory {
    fun requestAppOpenAd(param: AppOpenAdParam.Request, onLoaded: (Boolean) -> Unit)

    fun showAppOpen(
        activity: Activity,
        param: AppOpenAdParam.Show,
        onClosed: () -> Unit,
        onShown: () -> Unit = {},
        onImpression: () -> Unit = {},
        onClicked: () -> Unit = {},
        onFailedToShow: (AdError) -> Unit = {}
    )

    fun clearAd(adPlacement: String)

    fun fetchAd(param: AppOpenAdParam.Request, onLoaded: (Boolean) -> Unit) =
        requestAppOpenAd(param, onLoaded)

    fun showAd(activity: Activity, onClosed: () -> Unit) =
        showAppOpen(activity, AppOpenAdParam.Show(), onClosed)

    fun clearAd() = clearAd("app_open")
}
