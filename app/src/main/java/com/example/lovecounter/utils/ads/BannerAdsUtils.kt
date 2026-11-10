package com.example.lovecounter.utils.ads

import android.content.Context
import android.view.ViewGroup
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack

object BannerAdsUtils {
    fun loadBanner(
        context: Context,
        container: ViewGroup,
        adId: String,
        fallbackAdId: String? = null,
        callback: BannerAdCallBack? = null
    ): BannerAdHelper {
        val helper = BannerAdHelper(BannerAdConfig(BannerAdParam(adId, fallbackAdId)))
        helper.loadBanner(context, container, callback)
        return helper
    }
}
