package com.example.lovecounter.utils.ads

import android.content.Context
import android.view.ViewGroup
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.listener.BannerAdCallBack

object BannerAdsUtils {
    fun loadBanner(
        context: Context,
        container: ViewGroup,
        adId: String = AdPlacement.BANNER_MAIN_ID,
        placementKey: String = AdPlacement.MAIN_BANNER,
        fallbackAdId: String? = null,
        callback: BannerAdCallBack? = null
    ): BannerAdHelper {
        val helper = BannerAdHelper(
            BannerAdConfig(
                idAds = adId,
                idAdsPriority = fallbackAdId,
                adPlacement = placementKey
            )
        )

        // Hiển thị container và trạng thái loading (shimmer placeholder) ngay lập tức
        // trong lúc chờ SDK khởi tạo và phản hồi từ Remote Config / Consent (UMP).
        container.visibility = ViewGroup.VISIBLE
        helper.loadBanner(context, container, callback)

        // Kiểm tra điều kiện policy ở background; nếu bị tắt thì ẩn container và hủy request.
        AdsInitializer.runWhenReady {
            if (!AdsPolicyManager.canLoadAd(context, AdsPolicyManager.AdType.BANNER)) {
                container.visibility = ViewGroup.GONE
                helper.cancel()
                callback?.onAdFailedToLoad("Banner ads disabled by policy or config")
            }
        }

        return helper
    }
}
