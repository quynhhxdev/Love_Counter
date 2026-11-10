package com.ads.admob.helper.adnative

import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.R
import com.ads.admob.helper.adnative.params.NativeAdAdapterParam

data class NativeAdapterConfig<VH : RecyclerView.ViewHolder>(
    val adUnitId: String,
    val originalAdapter: RecyclerView.Adapter<VH>,
    val itemLayoutId: Int = R.layout.view_native_ad_medium,
    val firstPositionNativeAd: Int = 5,
    val adItemInterval: Int = 5,
    val repeatAds: Boolean = true,
    val param: NativeAdAdapterParam = NativeAdAdapterParam()
) {
    class Builder<VH : RecyclerView.ViewHolder> {
        private var adUnitId: String = ""
        private var originalAdapter: RecyclerView.Adapter<VH>? = null
        private var itemLayoutId: Int = R.layout.view_native_ad_medium
        private var firstPositionNativeAd: Int = 5
        private var adItemInterval: Int = 5
        private var repeatAds: Boolean = true
        private var param: NativeAdAdapterParam = NativeAdAdapterParam()

        fun setAdUnitId(value: String) = apply { adUnitId = value }
        fun setOriginalAdapter(value: RecyclerView.Adapter<VH>) =
            apply { originalAdapter = value }

        fun setItemLayoutId(value: Int) = apply { itemLayoutId = value }
        fun setFirstPositionNativeAd(value: Int) =
            apply { firstPositionNativeAd = value }

        fun setAdItemInterval(value: Int) = apply { adItemInterval = value }
        fun setRepeatAds(value: Boolean) = apply { repeatAds = value }
        fun setParam(value: NativeAdAdapterParam) = apply { param = value }

        fun build(): NativeAdapterConfig<VH> {
            require(adUnitId.isNotBlank()) { "Native ad unit ID must not be blank" }
            val adapter = requireNotNull(originalAdapter) {
                "Original RecyclerView adapter must be provided"
            }
            require(itemLayoutId != 0) { "Native ad item layout must be provided" }
            require(firstPositionNativeAd >= 0) {
                "firstPositionNativeAd must not be negative"
            }
            require(adItemInterval > 0) { "adItemInterval must be greater than zero" }
            return NativeAdapterConfig(
                adUnitId = adUnitId,
                originalAdapter = adapter,
                itemLayoutId = itemLayoutId,
                firstPositionNativeAd = firstPositionNativeAd,
                adItemInterval = adItemInterval,
                repeatAds = repeatAds,
                param = param
            )
        }
    }
}
