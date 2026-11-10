package com.ads.admob.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.helper.adnative.NativeAdAdapterHelper
import com.google.android.gms.ads.nativead.NativeAd

open class RecyclerViewAdapterWrapper<VH : RecyclerView.ViewHolder>(
    private val originalAdapter: RecyclerView.Adapter<VH>,
    nativeAds: List<NativeAd> = emptyList(),
    private val insertInterval: Int = 5,
    private val adTemplateSize: TemplateView.TemplateSize = TemplateView.TemplateSize.MEDIUM,
    private val firstAdPosition: Int = insertInterval,
    private val adLayoutId: Int? = null,
    private val adBinder: ((View, NativeAd) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    constructor(
        originalAdapter: RecyclerView.Adapter<VH>,
        nativeAd: NativeAd?,
        insertInterval: Int = 5,
        adTemplateSize: TemplateView.TemplateSize = TemplateView.TemplateSize.MEDIUM,
        firstAdPosition: Int = insertInterval
    ) : this(
        originalAdapter,
        listOfNotNull(nativeAd),
        insertInterval,
        adTemplateSize,
        firstAdPosition
    )

    private class AdViewHolder(val adView: View) : RecyclerView.ViewHolder(adView)

    private var nativeAds = nativeAds.toList()
    private val originalToWrappedViewTypes = mutableMapOf<Int, Int>()
    private val wrappedToOriginalViewTypes = mutableMapOf<Int, Int>()
    private var nextWrappedViewType = Int.MIN_VALUE + 1
    private var observerRegistered = false

    private val originalObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            notifyOriginalRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            notifyOriginalRangeChanged(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            notifyDataSetChanged()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            notifyDataSetChanged()
        }
    }

    init {
        require(insertInterval > 0) { "insertInterval must be greater than zero" }
        require(firstAdPosition >= 0) { "firstAdPosition must not be negative" }
        registerOriginalObserver()
    }

    override fun getItemCount(): Int {
        val originalCount = originalAdapter.itemCount
        return originalCount + adCount(originalCount)
    }

    override fun getItemViewType(position: Int): Int {
        if (isNativeAdPosition(position)) return AD_VIEW_TYPE
        val originalType = originalAdapter.getItemViewType(originalPositionFor(position))
        return originalToWrappedViewTypes.getOrPut(originalType) {
            allocateWrappedViewType(originalType)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == AD_VIEW_TYPE) {
            val adView = if (adLayoutId == null) {
                TemplateView(parent.context, adTemplateSize)
            } else {
                LayoutInflater.from(parent.context).inflate(adLayoutId, parent, false)
            }
            adView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return AdViewHolder(adView)
        }

        val originalType = checkNotNull(wrappedToOriginalViewTypes[viewType]) {
            "Unknown wrapped view type: $viewType"
        }
        return originalAdapter.onCreateViewHolder(parent, originalType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adIndex = getAdIndex(position)
        if (adIndex != null) {
            val adView = (holder as AdViewHolder).adView
            val ad = nativeAds.getOrNull(adIndex) ?: return
            if (adView is TemplateView) {
                adView.setNativeAd(ad)
            } else {
                adBinder?.invoke(adView, ad)
            }
            return
        }

        @Suppress("UNCHECKED_CAST")
        originalAdapter.onBindViewHolder(holder as VH, originalPositionFor(position))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (isNativeAdPosition(position)) {
            onBindViewHolder(holder, position)
            return
        }

        @Suppress("UNCHECKED_CAST")
        originalAdapter.onBindViewHolder(
            holder as VH,
            originalPositionFor(position),
            payloads
        )
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder !is AdViewHolder) {
            @Suppress("UNCHECKED_CAST")
            originalAdapter.onViewAttachedToWindow(holder as VH)
        }
        super.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder !is AdViewHolder) {
            @Suppress("UNCHECKED_CAST")
            originalAdapter.onViewDetachedFromWindow(holder as VH)
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdViewHolder) {
            (holder.adView as? TemplateView)?.clearNativeAd()
        } else {
            @Suppress("UNCHECKED_CAST")
            originalAdapter.onViewRecycled(holder as VH)
        }
        super.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        if (holder is AdViewHolder) return super.onFailedToRecycleView(holder)
        @Suppress("UNCHECKED_CAST")
        return originalAdapter.onFailedToRecycleView(holder as VH)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        registerOriginalObserver()
        originalAdapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        originalAdapter.onDetachedFromRecyclerView(recyclerView)
        unregisterOriginalObserver()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setNativeAds(ads: List<NativeAd>) {
        nativeAds = ads.toList()
        notifyDataSetChanged()
    }

    fun destroyNativeAds() {
        val destroyedAds = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<NativeAd, Boolean>()
        )
        nativeAds.forEach { ad ->
            if (destroyedAds.add(ad)) ad.destroy()
        }
        nativeAds = emptyList()
        notifyDataSetChanged()
    }

    fun isNativeAdPosition(position: Int): Boolean = getAdIndex(position) != null

    fun originalPositionFor(position: Int): Int =
        NativeAdAdapterHelper.originalPositionFor(
            adapterPosition = position,
            loadedAdCount = adCount(originalAdapter.itemCount),
            firstPositionNativeAd = firstAdPosition,
            adItemInterval = insertInterval
        )

    private fun adCount(originalCount: Int): Int {
        val possibleAds = if (originalCount <= firstAdPosition) {
            0
        } else {
            1 + (originalCount - 1 - firstAdPosition) / insertInterval
        }
        return minOf(possibleAds, nativeAds.size)
    }

    private fun getAdIndex(position: Int): Int? {
        val adCount = adCount(originalAdapter.itemCount)
        if (!NativeAdAdapterHelper.isAdPosition(
                adapterPosition = position,
                loadedAdCount = adCount,
                firstPositionNativeAd = firstAdPosition,
                adItemInterval = insertInterval
            )
        ) return null
        return (position - firstAdPosition) / (insertInterval + 1)
    }

    private fun wrappedPositionForOriginal(originalPosition: Int): Int {
        return NativeAdAdapterHelper.wrappedPositionForOriginal(
            originalPosition = originalPosition,
            loadedAdCount = adCount(originalAdapter.itemCount),
            firstPositionNativeAd = firstAdPosition,
            adItemInterval = insertInterval
        )
    }

    private fun notifyOriginalRangeChanged(
        positionStart: Int,
        itemCount: Int,
        payload: Any? = null
    ) {
        for (originalPosition in positionStart until positionStart + itemCount) {
            val wrappedPosition = wrappedPositionForOriginal(originalPosition)
            if (payload == null) {
                notifyItemChanged(wrappedPosition)
            } else {
                notifyItemChanged(wrappedPosition, payload)
            }
        }
    }

    private fun allocateWrappedViewType(originalType: Int): Int {
        while (nextWrappedViewType == AD_VIEW_TYPE ||
            wrappedToOriginalViewTypes.containsKey(nextWrappedViewType)
        ) {
            nextWrappedViewType++
        }
        val wrappedType = nextWrappedViewType++
        wrappedToOriginalViewTypes[wrappedType] = originalType
        return wrappedType
    }

    private fun registerOriginalObserver() {
        if (!observerRegistered) {
            originalAdapter.registerAdapterDataObserver(originalObserver)
            observerRegistered = true
        }
    }

    private fun unregisterOriginalObserver() {
        if (observerRegistered) {
            originalAdapter.unregisterAdapterDataObserver(originalObserver)
            observerRegistered = false
        }
    }

    companion object {
        private const val AD_VIEW_TYPE = Int.MIN_VALUE
    }
}
