package com.ads.admob.config

import android.app.Application

data class EventConfig(
    val currencyCode: String = "USD",
    val exchangeRate: Double = 1.0
)

class AfAdConfig private constructor(
    val application: Application?,
    val isVariantProduce: Boolean,
    val isDebug: Boolean,
    val provider: NetworkProvider,
    val mainNetworkProvider: NetworkProvider,
    val listDevices: List<String>,
    val testDevices: List<String>,
    val intervalBetweenInterstitial: Long,
    val interstitialIntervalMs: Long,
    val adjustConfig: AfAdjustConfig?,
    val eventConfig: EventConfig?
) {
    class Builder {
        private var application: Application? = null
        private var isVariantProduce: Boolean = false
        private var isDebug: Boolean = true
        private var provider: NetworkProvider = NetworkProvider.ADMOB
        private var listDevices: MutableList<String> = mutableListOf()
        private var intervalBetweenInterstitial: Long = 15_000L
        private var adjustConfig: AfAdjustConfig? = null
        private var eventConfig: EventConfig? = null

        fun setApplication(app: Application) = apply { this.application = app }
        fun setVariantProduce(isProduce: Boolean) = apply {
            this.isVariantProduce = isProduce
            this.isDebug = !isProduce
        }
        fun setDebug(debug: Boolean) = apply {
            this.isDebug = debug
            this.isVariantProduce = !debug
        }
        fun setProvider(provider: NetworkProvider) = apply { this.provider = provider }
        fun setMainNetworkProvider(provider: NetworkProvider) = apply { this.provider = provider }
        fun setListDevices(devices: List<String>) = apply { this.listDevices = devices.toMutableList() }
        fun setTestDevices(devices: List<String>) = apply { this.listDevices = devices.toMutableList() }
        fun addTestDevice(deviceHashedId: String) = apply { this.listDevices.add(deviceHashedId) }
        fun setIntervalBetweenInterstitial(intervalMs: Long) = apply { this.intervalBetweenInterstitial = intervalMs }
        fun setInterstitialIntervalMs(intervalMs: Long) = apply { this.intervalBetweenInterstitial = intervalMs }
        fun setAdjustConfig(config: AfAdjustConfig) = apply { this.adjustConfig = config }
        fun setEventConfig(config: EventConfig) = apply { this.eventConfig = config }
        fun setEventConfig(currencyCode: String, exchangeRate: Double) = apply {
            this.eventConfig = EventConfig(currencyCode, exchangeRate)
        }

        fun build(): AfAdConfig {
            return AfAdConfig(
                application = application,
                isVariantProduce = isVariantProduce,
                isDebug = isDebug,
                provider = provider,
                mainNetworkProvider = provider,
                listDevices = listDevices,
                testDevices = listDevices,
                intervalBetweenInterstitial = intervalBetweenInterstitial,
                interstitialIntervalMs = intervalBetweenInterstitial,
                adjustConfig = adjustConfig,
                eventConfig = eventConfig
            )
        }
    }
}
