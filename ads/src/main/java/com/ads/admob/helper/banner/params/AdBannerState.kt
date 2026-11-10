package com.ads.admob.helper.banner.params

enum class AdBannerState {
    NONE,
    FAIL,
    LOADING,
    CANCEL,
    LOADED,

    @Deprecated("Use NONE") IDLE,
    @Deprecated("Use FAIL") FAILED
}
