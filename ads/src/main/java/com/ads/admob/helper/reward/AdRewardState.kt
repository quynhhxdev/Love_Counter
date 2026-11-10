package com.ads.admob.helper.reward

enum class AdRewardState {
    NONE,
    FAIL,
    LOADING,
    LOADED,
    SHOW_FAIL,
    SHOWED,
    CANCEL,
    SHOW,

    @Deprecated("Use NONE") IDLE,
    @Deprecated("Use FAIL") FAILED,
    @Deprecated("Use SHOWED") SHOWING
}
