package com.ads.admob.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ads.admob.config.EventConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger

object FacebookTrackingManager {
    private const val TAG = "FacebookTracking"

    @Volatile
    private var logger: AppEventsLogger? = null
    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(context: Context, appId: String? = null, clientToken: String? = null) {
        if (initialized) return
        appId?.takeIf(String::isNotBlank)?.let(FacebookSdk::setApplicationId)
        clientToken?.takeIf(String::isNotBlank)?.let(FacebookSdk::setClientToken)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        logger = AppEventsLogger.newLogger(context.applicationContext)
        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    fun logEvent(context: Context, eventName: String, params: Bundle? = null) {
        val eventLogger = logger ?: synchronized(this) {
            logger ?: AppEventsLogger.newLogger(context.applicationContext).also { logger = it }
        }
        eventLogger.logEvent(eventName, params)
        Log.d(TAG, "Facebook tracking event: $eventName")
    }

    fun logPurchase(
        context: Context,
        amount: Double,
        config: EventConfig = EventConfig(),
        params: Bundle? = null
    ) {
        require(amount.isFinite() && amount >= 0.0) { "Purchase amount must be finite and non-negative" }
        val convertedAmount = convertAmount(amount, config)
        val eventParams = Bundle(params ?: Bundle()).apply {
            putString(AppEventsConstants.EVENT_PARAM_CURRENCY, config.currencyCode)
        }
        val eventLogger = logger ?: synchronized(this) {
            logger ?: AppEventsLogger.newLogger(context.applicationContext).also { logger = it }
        }
        eventLogger.logEvent(
            "fb_mobile_purchase",
            convertedAmount,
            eventParams
        )
        Log.d(TAG, "Facebook purchase event tracked")
    }

    fun logAdImpression(
        context: Context,
        amount: Double,
        placementName: String,
        adNetwork: String,
        adUnitId: String,
        adFormat: String,
        config: EventConfig = EventConfig()
    ) {
        require(amount.isFinite() && amount >= 0.0) { "Ad revenue must be finite and non-negative" }
        val convertedAmount = convertAmount(amount, config)
        val params = Bundle().apply {
            putString(AppEventsConstants.EVENT_PARAM_CURRENCY, config.currencyCode)
            putString("placement_name", placementName)
            putString("ad_network", adNetwork)
            putString("ad_unit_id", adUnitId)
            putString("ad_format", adFormat)
        }
        val eventLogger = logger ?: synchronized(this) {
            logger ?: AppEventsLogger.newLogger(context.applicationContext).also { logger = it }
        }
        eventLogger.logEvent("AdImpression", convertedAmount, params)
        Log.d(TAG, "Facebook ad impression tracked")
    }

    private fun convertAmount(amount: Double, config: EventConfig): Double {
        require(config.currencyCode.isNotBlank()) { "Currency must not be blank" }
        require(config.exchangeRate.isFinite() && config.exchangeRate > 0.0) {
            "Exchange rate must be finite and greater than zero"
        }
        return amount * config.exchangeRate
    }
}
