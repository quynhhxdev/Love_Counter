package com.ads.admob.helper.banner.params

import android.view.View
import com.ads.admob.BannerCollapsibleGravity
import com.ads.admob.BannerInlineStyle
import com.ads.admob.helper.params.IAdsParam
import com.google.android.gms.ads.AdView

sealed class BannerAdParam : IAdsParam {
    data class Request(
        val adUnitId: String,
        val fallbackAdUnitId: String? = null,
        @BannerInlineStyle val bannerInlineStyle: Int = BannerInlineStyle.SMALL,
        val useInlineAdaptive: Boolean = false,
        val adPlacement: String = "banner",
        @BannerCollapsibleGravity val collapsibleGravity: String? = null
    ) : BannerAdParam()

    data class Ready(val adView: AdView, val adPlacement: String) : BannerAdParam()

    data class Clickable(val view: View, val adPlacement: String) : BannerAdParam()

    companion object {
        operator fun invoke(
            adUnitId: String,
            fallbackAdUnitId: String? = null
        ): Request = Request(adUnitId, fallbackAdUnitId)
    }
}
