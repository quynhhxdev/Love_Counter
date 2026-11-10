package com.ads.admob.helper.adnative.params

import com.google.android.gms.ads.nativead.NativeAd

enum class AdNativeMediation {
    ADMOB,
    FACEBOOK,
    APPLOVIN,
    UNKNOWN;

    companion object {
        fun fromNativeAd(nativeAd: NativeAd): AdNativeMediation =
            fromAdapterClassName(nativeAd.responseInfo?.mediationAdapterClassName)

        fun fromAdapterClassName(adapterClassName: String?): AdNativeMediation {
            val name = adapterClassName.orEmpty().lowercase()
            return when {
                "facebook" in name || "meta" in name -> FACEBOOK
                "applovin" in name -> APPLOVIN
                "admob" in name || "google" in name || name.isBlank() -> ADMOB
                else -> UNKNOWN
            }
        }
    }
}
