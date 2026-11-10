package com.ads.admob.helper.adnative.params

import com.ads.admob.data.ContentAd
import com.ads.admob.helper.params.IAdsParam

sealed class NativeAdParam : IAdsParam {
    data class Request(
        val adUnitId: String,
        val waterfallAdUnitId: String? = null,
        val requestVideoContent: Boolean = true
    ) : NativeAdParam() {
        val fallbackAdUnitId: String?
            get() = waterfallAdUnitId
    }

    data class Ready(
        val contentAd: ContentAd.ApNativeAd
    ) : NativeAdParam()

    companion object {
        operator fun invoke(adUnitId: String, fallbackAdUnitId: String? = null): NativeAdParam =
            Request(adUnitId, fallbackAdUnitId)
    }
}
