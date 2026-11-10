package com.ads.admob.config

data class AfAdjustConfig(
    val adjustToken: String = "",
    val environmentProduct: Boolean = false,
    val adRevenueKey: String = "",
    val adRevenueMintegralKey: String = "",
    val appToken: String = adjustToken,
    val environment: String = if (environmentProduct) "production" else "sandbox"
) {
    class Builder {
        private var adjustToken: String = ""
        private var environmentProduct: Boolean = false
        private var adRevenueKey: String = ""
        private var adRevenueMintegralKey: String = ""

        fun setAdjustToken(token: String) = apply { this.adjustToken = token }
        fun setAppToken(token: String) = apply { this.adjustToken = token }
        fun setEnvironmentProduct(isProduct: Boolean) = apply { this.environmentProduct = isProduct }
        fun setEnvironment(env: String) = apply { this.environmentProduct = env.equals("production", ignoreCase = true) }
        fun setAdRevenueKey(key: String) = apply { this.adRevenueKey = key }
        fun setAdRevenueMintegralKey(key: String) = apply { this.adRevenueMintegralKey = key }

        fun build(): AfAdjustConfig {
            return AfAdjustConfig(
                adjustToken = adjustToken,
                environmentProduct = environmentProduct,
                adRevenueKey = adRevenueKey,
                adRevenueMintegralKey = adRevenueMintegralKey,
                appToken = if (adjustToken.isNotBlank()) adjustToken else "",
                environment = if (environmentProduct) "production" else "sandbox"
            )
        }
    }
}
