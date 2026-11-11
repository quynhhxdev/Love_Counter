package com.ads.admob.helper.banner

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.banner.factory.admob.AdmobBannerFactoryImpl
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.helper.banner.params.AdBannerState
import com.ads.admob.listener.BannerAdCallBack
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

class BannerAdHelper(private val config: BannerAdConfig) :
    AdsHelper<BannerAdParam>() {
    constructor(
        config: BannerAdConfig,
        context: Context,
        container: ViewGroup,
        callback: BannerAdCallBack? = null
    ) : this(config) {
        loadBanner(context, container, callback)
    }

    private val factory = AdmobBannerFactoryImpl()
    private val mutableState = MutableStateFlow(AdBannerState.NONE)
    val state: StateFlow<AdBannerState> = mutableState

    private var contextRef: WeakReference<Context>? = null
    private var containerRef: WeakReference<ViewGroup>? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var shimmerView: ShimmerFrameLayout? = null
    private var callback: BannerAdCallBack? = null
    private var requestGeneration = 0L
    private var hasCompletedInitialRequest = false
    private var isCancelled = false

    fun loadBanner(context: Context, container: ViewGroup, callback: BannerAdCallBack? = null) {
        loadBanner(context, context as? LifecycleOwner, container, callback)
    }

    fun loadBanner(
        context: Context,
        owner: LifecycleOwner?,
        container: ViewGroup,
        callback: BannerAdCallBack? = null
    ) {
        canReloadAd = config.canReloadAds
        val previousContainer = containerRef?.get()
        if (previousContainer != null && previousContainer !== container) {
            factory.destroyBanner(previousContainer)
        }
        contextRef = WeakReference(context)
        containerRef = WeakReference(container)
        this.callback = callback
        isCancelled = false
        attachLifecycleOwner(owner)
        loadInternal(context, container)
    }

    fun reloadBanner(force: Boolean = false) {
        if (!canReloadAd) return
        if (!force && (mutableState.value == AdBannerState.LOADED || mutableState.value == AdBannerState.LOADING)) {
            return
        }
        val context = contextRef?.get() ?: return
        val container = containerRef?.get() ?: return
        if (!isCancelled) loadInternal(context, container)
    }

    fun cancel() {
        isCancelled = true
        requestGeneration++
        shimmerView?.stopShimmer()
        shimmerView = null
        containerRef?.get()?.let(factory::destroyBanner)
        mutableState.value = AdBannerState.CANCEL
    }

    fun dispose() {
        cancel()
        lifecycleOwner?.lifecycle?.removeObserver(this)
        detachLifecycleOwner()
        lifecycleOwner = null
        contextRef?.clear()
        contextRef = null
        containerRef?.clear()
        containerRef = null
        callback = null
    }

    override fun onResume(owner: LifecycleOwner) {
        // Do not reload banner on Activity/Fragment onResume.
        // Google Mobile Ads SDK handles banner auto-refresh natively.
    }

    override fun onDestroy(owner: LifecycleOwner) {
        dispose()
        super.onDestroy(owner)
    }

    private fun attachLifecycleOwner(owner: LifecycleOwner?) {
        if (lifecycleOwner === owner) return
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = owner
        observeLifecycleOwner(owner)
        owner?.lifecycle?.addObserver(this)
    }

    private fun loadInternal(context: Context, container: ViewGroup) {
        val requestId = ++requestGeneration
        if (!config.canShowAds || !canRequestAds(context, config.canShowAds) || config.idAds.isBlank()) {
            failRequest(requestId, container, "Banner ads are disabled or the ad unit ID is empty")
            return
        }

        mutableState.value = AdBannerState.LOADING
        container.visibility = View.VISIBLE
        factory.destroyBanner(container)
        showShimmer(context, container)
        factory.requestBannerAd(context, container, config.param, object : BannerAdCallBack {
            override fun onAdLoaded() {
                if (requestId != requestGeneration || isCancelled) return
                hasCompletedInitialRequest = true
                mutableState.value = AdBannerState.LOADED
                shimmerView?.stopShimmer()
                shimmerView?.let(container::removeView)
                shimmerView = null
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: String) {
                failRequest(requestId, container, error)
            }

            override fun onAdClicked() {
                if (requestId == requestGeneration && !isCancelled) callback?.onAdClicked()
            }

            override fun onAdImpression() {
                if (requestId == requestGeneration && !isCancelled) callback?.onAdImpression()
            }

            override fun onAdReady(ready: com.ads.admob.helper.banner.params.BannerAdParam.Ready) = Unit

            override fun onAdClick(clickable: com.ads.admob.helper.banner.params.BannerAdParam.Clickable) {
                if (requestId == requestGeneration && !isCancelled) callback?.onAdClicked()
            }
        })
    }

    private fun failRequest(requestId: Long, container: ViewGroup, error: String) {
        if (requestId != requestGeneration || isCancelled) return
        hasCompletedInitialRequest = true
        shimmerView?.stopShimmer()
        shimmerView = null
        factory.destroyBanner(container)
        container.visibility = View.GONE
        mutableState.value = AdBannerState.FAIL
        callback?.onAdFailedToLoad(error)
    }

    private fun showShimmer(context: Context, container: ViewGroup) {
        val shimmerHeightDp = if (config.useInlineAdaptive) {
            when (config.bannerInlineStyle) {
                com.ads.admob.BannerInlineStyle.MEDIUM -> 90
                com.ads.admob.BannerInlineStyle.LARGE -> 250
                else -> 50
            }
        } else {
            50
        }
        val placeholder = View(context).apply {
            setBackgroundColor(SHIMMER_BASE_COLOR)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        shimmerView = ShimmerFrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (shimmerHeightDp * context.resources.displayMetrics.density).toInt()
            )
            addView(placeholder)
            startShimmer()
        }
        container.addView(shimmerView)
    }

    companion object {
        private const val SHIMMER_BASE_COLOR = 0xFFE5E5E5.toInt()
    }
}
