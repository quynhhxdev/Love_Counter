package com.ads.admob.helper.appopen

import com.ads.admob.helper.appopen.params.AppOpenAdParam

data class AppOpenAdConfig(
    val idAds: String = "",
    val idAdsPriority: String? = null,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true,
    val reloadIfFirstFail: Boolean = true,
    val adPlacement: String = "app_open",
    val param: AppOpenAdParam.Request = AppOpenAdParam.Request(idAds, idAdsPriority, adPlacement)
)
