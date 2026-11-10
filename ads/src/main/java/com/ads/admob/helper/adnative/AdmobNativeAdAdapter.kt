package com.ads.admob.helper.adnative

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.adnative.factory.admob.AdmobNativeFactoryImpl
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.widget.RecyclerViewAdapterWrapper
import com.google.android.gms.ads.nativead.NativeAd

class AdmobNativeAdAdapter<VH : RecyclerView.ViewHolder>(
    context: Context,
    val config: NativeAdapterConfig<VH>,
    private val nativeFactory: AdmobNativeFactoryImpl = AdmobNativeFactoryImpl()
) : RecyclerViewAdapterWrapper<VH>(
    originalAdapter = config.originalAdapter,
    nativeAds = emptyList(),
    insertInterval = config.adItemInterval,
    firstAdPosition = config.firstPositionNativeAd,
    adLayoutId = config.itemLayoutId,
    adBinder = { view, ad -> nativeFactory.bindNativeAdView(ad, view) }
) {

    private val appContext = context.applicationContext
    private val eligibility = object : AdsHelper<NativeAdParam>() {
        fun canRequestAds() = canRequestAds(appContext)
    }

    private val loadedAds = sortedMapOf<Int, NativeAd>()
    private var displayedAds: List<NativeAd> = emptyList()
    private val inFlightSlots = mutableSetOf<Int>()
    private val failedSlots = mutableSetOf<Int>()
    private var requestGeneration = 0L
    private var disposed = false
    private var observerRegistered = false
    private var gridLayoutManager: GridLayoutManager? = null
    private var previousSpanSizeLookup: GridLayoutManager.SpanSizeLookup? = null
    private var installedSpanSizeLookup: GridLayoutManager.SpanSizeLookup? = null

    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = synchronizeAds()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = synchronizeAds()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) =
            synchronizeAds()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) =
            synchronizeAds()

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) =
            synchronizeAds()

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
            synchronizeAds()
    }

    init {
        registerDataObserver()
        synchronizeAds()
    }

    fun refreshAds() {
        if (disposed) return
        requestGeneration++
        loadedAds.values.forEach(NativeAd::destroy)
        loadedAds.clear()
        displayedAds = emptyList()
        inFlightSlots.clear()
        failedSlots.clear()
        setNativeAds(emptyList())
        synchronizeAds()
    }

    fun destroy() {
        if (disposed) return
        disposed = true
        requestGeneration++
        unregisterDataObserver()
        restoreGridLayoutManager()
        loadedAds.clear()
        displayedAds = emptyList()
        inFlightSlots.clear()
        failedSlots.clear()
        destroyNativeAds()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        registerDataObserver()
        configureGridLayoutManager(recyclerView.layoutManager as? GridLayoutManager)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        restoreGridLayoutManager()
        unregisterDataObserver()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private fun synchronizeAds() {
        if (disposed) return
        val targetCount = targetAdCount()

        val expiredSlots = loadedAds.keys.filter { it >= targetCount }
        expiredSlots.forEach { slot ->
            loadedAds.remove(slot)?.destroy()
        }
        failedSlots.removeAll { it >= targetCount }
        publishAds()

        if (targetCount == 0 || !eligibility.canRequestAds()) return
        val generation = requestGeneration
        for (slot in 0 until targetCount) {
            if (slot in loadedAds || slot in inFlightSlots || slot in failedSlots) continue
            inFlightSlots += slot
            nativeFactory.requestNativeAd(
                context = appContext,
                param = NativeAdParam.Request(
                    adUnitId = config.adUnitId,
                    waterfallAdUnitId = config.param.waterfallAdUnitId,
                    requestVideoContent = config.param.requestVideoContent
                ),
                onLoaded = { nativeAd ->
                    if (disposed || generation != requestGeneration) {
                        nativeAd.destroy()
                        return@requestNativeAd
                    }
                    inFlightSlots.remove(slot)
                    if (slot >= targetAdCount()) {
                        nativeAd.destroy()
                        synchronizeAds()
                        return@requestNativeAd
                    }
                    loadedAds[slot] = nativeAd
                    publishAds()
                    synchronizeAds()
                },
                onFailed = { error ->
                    if (disposed || generation != requestGeneration) return@requestNativeAd
                    inFlightSlots.remove(slot)
                    failedSlots += slot
                    Log.w(TAG, "Native ad slot $slot failed: $error")
                },
                onClicked = {
                    Log.d(TAG, "Native ad clicked at ${config.adUnitId}")
                },
                onImpression = {
                    Log.d(TAG, "Native ad impression at ${config.adUnitId}")
                }
            )
        }
    }

    private fun targetAdCount(): Int {
        val potentialCount = NativeAdAdapterHelper.calculateAdCount(
            originalItemCount = config.originalAdapter.itemCount,
            firstPositionNativeAd = config.firstPositionNativeAd,
            adItemInterval = config.adItemInterval,
            repeatAds = config.repeatAds
        )
        return minOf(potentialCount, config.param.maxAds)
    }

    private fun publishAds() {
        val ads = loadedAds.values.toList()
        if (ads.size == displayedAds.size && ads.indices.all { ads[it] === displayedAds[it] }) {
            return
        }
        displayedAds = ads
        setNativeAds(ads)
    }

    private fun configureGridLayoutManager(layoutManager: GridLayoutManager?) {
        restoreGridLayoutManager()
        if (layoutManager == null) return

        gridLayoutManager = layoutManager
        previousSpanSizeLookup = layoutManager.spanSizeLookup
        installedSpanSizeLookup = NativeAdAdapterHelper.createGridSpanSizeLookup(
            layoutManager = layoutManager,
            isAdPosition = ::isNativeAdPosition,
            originalPositionFor = ::originalPositionFor
        )
        layoutManager.spanSizeLookup = installedSpanSizeLookup
        layoutManager.requestLayout()
    }

    private fun restoreGridLayoutManager() {
        val manager = gridLayoutManager
        val installed = installedSpanSizeLookup
        if (manager != null && installed != null && manager.spanSizeLookup === installed) {
            previousSpanSizeLookup?.let { manager.spanSizeLookup = it }
        }
        gridLayoutManager = null
        previousSpanSizeLookup = null
        installedSpanSizeLookup = null
    }

    private fun registerDataObserver() {
        if (!observerRegistered) {
            config.originalAdapter.registerAdapterDataObserver(dataObserver)
            observerRegistered = true
        }
    }

    private fun unregisterDataObserver() {
        if (observerRegistered) {
            config.originalAdapter.unregisterAdapterDataObserver(dataObserver)
            observerRegistered = false
        }
    }

    companion object {
        private const val TAG = "AdmobNativeAdAdapter"
    }
}
