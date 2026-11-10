package com.ads.admob.helper.adnative.params

import com.google.android.gms.ads.nativead.NativeAd

sealed interface AdNativeState {
    data object None : AdNativeState
    data object Loading : AdNativeState
    data object Cancel : AdNativeState
    data class Fail(val message: String) : AdNativeState
    data class Loaded(val nativeAd: NativeAd) : AdNativeState
}
