package com.ads.admob.helper.interstitial.params

enum class AdInterstitialState {
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
