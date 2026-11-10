package com.ads.admob.config

import androidx.annotation.IntDef

@IntDef(
    NetworkProviderValue.ADMOB,
    NetworkProviderValue.MAX,
    NetworkProviderValue.MIX
)
@Retention(AnnotationRetention.SOURCE)
annotation class NetworkProviderType

object NetworkProviderValue {
    const val ADMOB = 0
    const val MAX = 1
    const val MIX = 3
}

enum class NetworkProvider(val value: Int) {
    ADMOB(NetworkProviderValue.ADMOB),
    MAX(NetworkProviderValue.MAX),
    MIX(NetworkProviderValue.MIX),
    MINTEGRAL(4),
    UNITY(5),
    IRONSOURCE(6);

    companion object {
        const val ADMOB_VAL = 0
        const val MAX_VAL = 1
        const val MIX_VAL = 3

        fun fromValue(value: Int): NetworkProvider {
            return entries.firstOrNull { it.value == value } ?: ADMOB
        }
    }
}
