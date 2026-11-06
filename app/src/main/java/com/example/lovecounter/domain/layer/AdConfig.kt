package com.example.lovecounter.domain.layer

data class AdConfig(
    val isEnableBanner: Boolean = true,
    val isEnableNative: Boolean = true,
    val isEnableInter: Boolean = true,
    val isEnableAppOpen: Boolean = true,
    val isEnableReward: Boolean = true
)
