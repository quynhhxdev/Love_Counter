package com.ads.admob.helper.appopen

import android.app.Activity
import com.ads.admob.helper.adnative.params.AdNativeMediation
import com.ads.admob.helper.appopen.params.AppOpenAdParam

data class AppResumeAdConfig(
    val param: AppOpenAdParam.Request,
    val listClassInValid: List<Class<out Activity>> = emptyList(),
    val loadOnResume: Boolean = true,
    val provider: AdNativeMediation = param.provider,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true
)
