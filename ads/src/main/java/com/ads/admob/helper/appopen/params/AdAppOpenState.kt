package com.ads.admob.helper.appopen.params

enum class AdAppOpenState {
    NONE,
    FAIL,
    LOADING,
    LOADED,
    SHOW_FAIL,
    SHOWED,
    CANCEL,

    @Deprecated("Use NONE") IDLE,
    @Deprecated("Use FAIL") FAILED,
    @Deprecated("Use SHOWED") SHOWING
}
