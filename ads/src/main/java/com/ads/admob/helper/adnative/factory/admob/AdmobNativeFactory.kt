package com.ads.admob.helper.adnative.factory.admob

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.helper.adnative.params.NativeLayoutMediation
import com.ads.admob.widget.NativeTemplateStyle
import com.google.android.gms.ads.nativead.NativeAd

interface AdmobNativeFactory {
    fun requestNativeAd(
        context: Context,
        param: NativeAdParam,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit,
        onClicked: () -> Unit = {},
        onImpression: () -> Unit = {}
    )

    fun populateNativeAdView(
        context: Context,
        nativeAd: NativeAd,
        container: ViewGroup,
        defaultLayoutId: Int,
        mediationLayouts: List<NativeLayoutMediation> = emptyList(),
        style: NativeTemplateStyle? = null
    ): Boolean

    fun bindNativeAdView(
        nativeAd: NativeAd,
        rootView: View,
        style: NativeTemplateStyle? = null
    ): Boolean

    companion object {
        @JvmStatic
        fun getInstance(): com.ads.admob.helper.adnative.factory.admob.AdmobNativeFactory = AdmobNativeFactoryImpl()
    }
}
