package com.ads.admob.helper.adnative

import androidx.recyclerview.widget.GridLayoutManager

object NativeAdAdapterHelper {
    fun calculateAdCount(
        originalItemCount: Int,
        firstPositionNativeAd: Int,
        adItemInterval: Int,
        repeatAds: Boolean
    ): Int {
        require(originalItemCount >= 0) { "originalItemCount must not be negative" }
        require(firstPositionNativeAd >= 0) { "firstPositionNativeAd must not be negative" }
        require(adItemInterval > 0) { "adItemInterval must be greater than zero" }

        if (originalItemCount <= firstPositionNativeAd) return 0
        val possibleCount =
            1 + (originalItemCount - 1 - firstPositionNativeAd) / adItemInterval
        return if (repeatAds) possibleCount else minOf(possibleCount, 1)
    }

    fun isAdPosition(
        adapterPosition: Int,
        loadedAdCount: Int,
        firstPositionNativeAd: Int,
        adItemInterval: Int
    ): Boolean {
        if (adapterPosition < firstPositionNativeAd || loadedAdCount <= 0) return false
        val relativePosition = adapterPosition - firstPositionNativeAd
        val itemBlockSize = adItemInterval + 1
        return relativePosition % itemBlockSize == 0 &&
            relativePosition / itemBlockSize < loadedAdCount
    }

    fun originalPositionFor(
        adapterPosition: Int,
        loadedAdCount: Int,
        firstPositionNativeAd: Int,
        adItemInterval: Int
    ): Int {
        if (adapterPosition < firstPositionNativeAd) return adapterPosition
        val relativePosition = adapterPosition - firstPositionNativeAd
        val adsBefore = minOf(
            (relativePosition + adItemInterval) / (adItemInterval + 1),
            loadedAdCount
        )
        return adapterPosition - adsBefore
    }

    fun wrappedPositionForOriginal(
        originalPosition: Int,
        loadedAdCount: Int,
        firstPositionNativeAd: Int,
        adItemInterval: Int
    ): Int {
        if (originalPosition < firstPositionNativeAd) return originalPosition
        val adsBefore = minOf(
            1 + (originalPosition - firstPositionNativeAd) / adItemInterval,
            loadedAdCount
        )
        return originalPosition + adsBefore
    }

    fun createGridSpanSizeLookup(
        layoutManager: GridLayoutManager,
        isAdPosition: (Int) -> Boolean,
        originalPositionFor: (Int) -> Int
    ): GridLayoutManager.SpanSizeLookup {
        val originalLookup = layoutManager.spanSizeLookup
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (isAdPosition(position)) return layoutManager.spanCount
                return originalLookup.getSpanSize(originalPositionFor(position))
                    .coerceIn(1, layoutManager.spanCount)
            }
        }.apply {
            isSpanIndexCacheEnabled = originalLookup.isSpanIndexCacheEnabled
        }
    }
}
