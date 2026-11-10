package com.ads.admob.helper.adnative.params

import com.ads.admob.helper.params.IAdsParam

data class NativeAdAdapterParam(
    val waterfallAdUnitId: String? = null,
    val requestVideoContent: Boolean = true,
    val maxAds: Int = 5
) : IAdsParam {
    init {
        require(maxAds > 0) { "maxAds must be greater than zero" }
    }
}
