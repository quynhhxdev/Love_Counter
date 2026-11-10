package com.ads.admob.helper.banner.factory.admob

import android.content.Context
import android.view.ViewGroup
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.google.android.gms.ads.AdSize

interface AdmobBannerFactory {
    fun requestBannerAd(
        context: Context,
        container: ViewGroup,
        param: BannerAdParam.Request,
        callback: BannerAdCallBack? = null
    )

    fun getAdSize(context: Context, container: ViewGroup, param: BannerAdParam.Request): AdSize

    fun destroyBanner(container: ViewGroup)
}
