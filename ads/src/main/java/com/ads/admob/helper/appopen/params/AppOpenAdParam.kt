package com.ads.admob.helper.appopen.params

import com.ads.admob.helper.adnative.params.AdNativeMediation
import com.ads.admob.helper.params.IAdsParam

sealed class AppOpenAdParam : IAdsParam {
    data class Request(
        val adUnitId: String,
        val fallbackAdUnitId: String? = null,
        val adPlacement: String = "app_open",
        val provider: AdNativeMediation = AdNativeMediation.ADMOB
    ) : AppOpenAdParam()

    data class Show(val adPlacement: String = "app_open") : AppOpenAdParam()

    data class Clickable(val adPlacement: String = "app_open") : AppOpenAdParam()

    companion object {
        operator fun invoke(
            adUnitId: String,
            fallbackAdUnitId: String? = null,
            adPlacement: String = "app_open",
            provider: AdNativeMediation = AdNativeMediation.ADMOB
        ): Request = Request(adUnitId, fallbackAdUnitId, adPlacement, provider)
    }
}
