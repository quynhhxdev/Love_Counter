package com.ads.admob.helper.banner

import com.ads.admob.BannerCollapsibleGravity
import com.ads.admob.BannerInlineStyle
import com.ads.admob.helper.banner.params.BannerAdParam

data class BannerAdConfig(
    val idAds: String = "",
    val idAdsPriority: String? = null,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true,
    @BannerInlineStyle val bannerInlineStyle: Int = BannerInlineStyle.SMALL,
    val useInlineAdaptive: Boolean = false,
    val adPlacement: String = "banner",
    @BannerCollapsibleGravity val collapsibleGravity: String? = null
) {
    constructor(param: BannerAdParam.Request) : this(
        idAds = param.adUnitId,
        idAdsPriority = param.fallbackAdUnitId,
        bannerInlineStyle = param.bannerInlineStyle,
        useInlineAdaptive = param.useInlineAdaptive,
        adPlacement = param.adPlacement,
        collapsibleGravity = param.collapsibleGravity
    )

    val param: BannerAdParam.Request
        get() = BannerAdParam.Request(
            adUnitId = idAds,
            fallbackAdUnitId = idAdsPriority,
            bannerInlineStyle = bannerInlineStyle,
            useInlineAdaptive = useInlineAdaptive,
            adPlacement = adPlacement,
            collapsibleGravity = collapsibleGravity
        )
}
