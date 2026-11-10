package com.ads.admob.helper.adnative

import com.ads.admob.R
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.helper.adnative.params.NativeLayoutMediation

data class NativeAdConfig(
    val placementKey: String,
    val param: NativeAdParam,
    val isEnabled: Boolean = true,
    val allowReload: Boolean = true,
    val layoutId: Int = R.layout.view_native_ad_medium,
    val adPlacement: String = placementKey,
    val refreshIntervalMillis: Long = 0L,
    val mediationLayouts: List<NativeLayoutMediation> = emptyList()
) {
    init {
        require(layoutId != 0) { "Native ad layout ID must not be zero" }
        require(refreshIntervalMillis >= 0L) {
            "refreshIntervalMillis must not be negative"
        }
    }

    val adUnitId: String
        get() = (param as? NativeAdParam.Request)?.adUnitId.orEmpty()

    val waterfallAdUnitId: String?
        get() = (param as? NativeAdParam.Request)?.waterfallAdUnitId

    class Builder {
        private var placementKey: String = ""
        private var adUnitId: String = ""
        private var waterfallAdUnitId: String? = null
        private var param: NativeAdParam? = null
        private var isEnabled: Boolean = true
        private var allowReload: Boolean = true
        private var layoutId: Int = R.layout.view_native_ad_medium
        private var adPlacement: String? = null
        private var refreshIntervalMillis: Long = 0L
        private var mediationLayouts: List<NativeLayoutMediation> = emptyList()

        fun setPlacementKey(value: String) = apply { placementKey = value }
        fun setAdUnitId(value: String) = apply { adUnitId = value }
        fun setWaterfallAdUnitId(value: String?) = apply { waterfallAdUnitId = value }
        fun setParam(value: NativeAdParam) = apply { param = value }
        fun setEnabled(value: Boolean) = apply { isEnabled = value }
        fun setAllowReload(value: Boolean) = apply { allowReload = value }
        fun setLayoutId(value: Int) = apply { layoutId = value }
        fun setAdPlacement(value: String?) = apply { adPlacement = value }
        fun setRefreshIntervalMillis(value: Long) = apply { refreshIntervalMillis = value }
        fun setMediationLayouts(value: List<NativeLayoutMediation>) =
            apply { mediationLayouts = value.toList() }

        fun build(): NativeAdConfig {
            require(refreshIntervalMillis >= 0L) {
                "refreshIntervalMillis must not be negative"
            }
            val requestParam = param ?: NativeAdParam.Request(
                adUnitId = adUnitId,
                waterfallAdUnitId = waterfallAdUnitId
            )
            return NativeAdConfig(
                placementKey = placementKey,
                param = requestParam,
                isEnabled = isEnabled,
                allowReload = allowReload,
                layoutId = layoutId,
                adPlacement = adPlacement ?: placementKey,
                refreshIntervalMillis = refreshIntervalMillis,
                mediationLayouts = mediationLayouts
            )
        }
    }
}
