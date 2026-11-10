package com.ads.admob.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseTrackingManager {
    private const val TAG = "FirebaseTracking"

    fun logEvent(context: Context, eventName: String, params: Bundle? = null) {
        try {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
            Log.d(TAG, "Firebase tracking event: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track Firebase event: $eventName", e)
        }
    }

    fun logScreenView(context: Context, screenName: String, screenClass: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(context, FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    fun logRevenue(
        context: Context,
        currency: String,
        valueMicros: Long,
        adPlacement: String,
        adSource: String,
        adUnitId: String,
        adFormat: String,
    ) {
        require(valueMicros >= 0) { "Ad revenue micros must not be negative" }
        logAdImpression(
            context = context,
            currency = currency,
            value = valueMicros / 1_000_000.0,
            adUnitId = adUnitId,
            adSource = adSource,
            adPlacement = adPlacement,
            adFormat = adFormat
        )
    }

    fun logAdImpression(
        context: Context,
        currency: String,
        value: Double,
        adUnitId: String,
        adSource: String,
        adPlacement: String = "",
        adFormat: String = "",
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, value)
            putString("ad_platform", "admob")
            putString("ad_source", adSource)
            putString("ad_unit_id", adUnitId)
            putString("ad_placement", adPlacement)
            putString("ad_format", adFormat)
        }
        logEvent(context, "ad_impression", bundle)
    }
}
