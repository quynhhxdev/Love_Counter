package com.ads.admob.event

import android.content.Context
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.ads.admob.config.AfAdjustConfig

object AdjustTrackingManager {
    private const val TAG = "AdjustTracking"
    private const val MICROS_PER_UNIT = 1_000_000.0
    private const val SOURCE_ADMOB = "admob_sdk"
    private const val SOURCE_APPLOVIN_MAX = "applovin_max_sdk"
    private const val SOURCE_OTHER = "other_sdk"

    const val ENVIRONMENT_SANDBOX = AdjustConfig.ENVIRONMENT_SANDBOX
    const val ENVIRONMENT_PRODUCTION = AdjustConfig.ENVIRONMENT_PRODUCTION

    val SUPPORTED_AD_NETWORKS = listOf(
        "admob",
        "applovin_max",
        "ironsource",
        "unity",
        "facebook",
        "pangle",
        "mintegral",
        "chartboost",
    )

    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(
        context: Context,
        appToken: String,
        environment: String
    ) {
        require(appToken.isNotBlank()) { "Adjust app token must not be blank" }
        require((environment == ENVIRONMENT_SANDBOX) || (environment == ENVIRONMENT_PRODUCTION)) {
            "Unsupported Adjust environment: $environment"
        }
        if (initialized) return

        Adjust.initSdk(AdjustConfig(context.applicationContext, appToken, environment))
        initialized = true
    }

    fun initialize(context: Context, config: AfAdjustConfig) {
        initialize(context, config.appToken, config.environment)
    }

    fun trackEvent(eventToken: String) {
        Adjust.trackEvent(AdjustEvent(eventToken))
    }

    fun trackInteraction(eventToken: String) = trackEvent(eventToken)

    fun onTrackRevenuePurchase(
        eventToken: String,
        revenue: Double,
        currency: String,
        orderId: String? = null
    ) {
        val event = AdjustEvent(eventToken)
        event.setRevenue(revenue, currency)
        orderId?.let(event::setOrderId)
        Adjust.trackEvent(event)
    }

    fun pushTrackEventAdmob(
        revenueMicros: Long,
        currency: String,
        placementName: String,
        adNetwork: String
    ) {
        trackAdRevenue(
            source = SOURCE_ADMOB,
            revenue = revenueMicros / MICROS_PER_UNIT,
            currency = currency,
            placementName = placementName,
            adNetwork = adNetwork
        )
    }

    fun pushTrackEventApplovin(
        revenue: Double,
        currency: String,
        placementName: String,
        adNetwork: String
    ) {
        trackAdRevenue(SOURCE_APPLOVIN_MAX, revenue, currency, placementName, adNetwork)
    }

    fun pushTrackEventApplovinMicros(
        revenueMicros: Long,
        currency: String,
        placementName: String,
        adNetwork: String
    ) {
        pushTrackEventApplovin(
            revenueMicros / MICROS_PER_UNIT,
            currency,
            placementName,
            adNetwork
        )
    }

    fun trackAdRevenue(revenue: Double, currency: String) {
        trackAdRevenue(SOURCE_OTHER, revenue, currency, placementName = "", adNetwork = "")
    }

    private fun trackAdRevenue(
        source: String,
        revenue: Double,
        currency: String,
        placementName: String,
        adNetwork: String
    ) {
        require((revenue.isFinite()) && (revenue >= 0.0)) {
            "Ad revenue must be finite and non-negative"
        }
        require(currency.isNotBlank()) { "Currency must not be blank" }

        val adRevenue = AdjustAdRevenue(source).apply {
            setRevenue(revenue, currency)
            if (placementName.isNotBlank()) setAdRevenuePlacement(placementName)
            if (adNetwork.isNotBlank()) setAdRevenueNetwork(adNetwork)
        }
        Adjust.trackAdRevenue(adRevenue)
        Log.d(TAG, "Tracked Adjust ad revenue: $revenue $currency from $source")
    }
}
