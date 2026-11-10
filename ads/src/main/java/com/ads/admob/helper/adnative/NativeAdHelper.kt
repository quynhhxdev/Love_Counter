package com.ads.admob.helper.adnative

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.adnative.factory.admob.AdmobNativeFactoryImpl
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.widget.NativeAdShimmerView
import com.google.android.gms.ads.nativead.NativeAd
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NativeAdHelper(private val config: NativeAdConfig) : AdsHelper<NativeAdParam>() {

    private val factory = AdmobNativeFactoryImpl()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<AdNativeState>(AdNativeState.None)
    val state: StateFlow<AdNativeState> = _state.asStateFlow()

    private var applicationContext: Context? = null
    private var containerReference: WeakReference<ViewGroup>? = null
    private var callback: NativeAdCallback? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var currentNativeAd: NativeAd? = null
    private var refreshTimerJob: Job? = null
    private var requestGeneration = 0L
    private var hasReceivedFirstResume = false
    private var isDestroyed = false

    init {
        canReloadAd = config.allowReload
    }

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (!hasReceivedFirstResume) {
                    hasReceivedFirstResume = true
                    when (_state.value) {
                        is AdNativeState.Fail, AdNativeState.None -> beginLoad(force = true)
                        is AdNativeState.Loaded -> scheduleRefresh()
                        else -> Unit
                    }
                } else if (canReloadAd) {
                    beginLoad(force = true)
                }
            }

            Lifecycle.Event.ON_PAUSE -> refreshTimerJob?.cancel()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> Unit
        }
    }

    fun loadNative(context: Context, callback: NativeAdCallback) {
        loadNative(context, null, callback)
    }

    fun loadNative(context: Context, container: ViewGroup?, callback: NativeAdCallback) {
        check(!isDestroyed) { "NativeAdHelper has been destroyed" }
        applicationContext = context.applicationContext
        containerReference = container?.let(::WeakReference)
        this.callback = callback
        (context as? LifecycleOwner)?.let(::observeLifecycle)
        beginLoad()
    }

    fun start(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        container: ViewGroup?,
        callback: NativeAdCallback
    ) {
        check(!isDestroyed) { "NativeAdHelper has been destroyed" }
        applicationContext = context.applicationContext
        containerReference = container?.let(::WeakReference)
        this.callback = callback
        observeLifecycle(lifecycleOwner)
        beginLoad()
    }

    fun cancel() {
        if (isDestroyed) return
        requestGeneration++
        refreshTimerJob?.cancel()
        refreshTimerJob = null
        _state.value = AdNativeState.Cancel
        showShimmer(false)
    }

    fun destroy() {
        if (isDestroyed) return
        requestGeneration++
        refreshTimerJob?.cancel()
        refreshTimerJob = null
        _state.value = AdNativeState.Cancel
        showShimmer(false)
        isDestroyed = true
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        detachLifecycleOwner()
        lifecycleOwner = null
        currentNativeAd?.destroy()
        currentNativeAd = null
        containerReference?.get()?.let { container ->
            container.removeAllViews()
            container.visibility = View.GONE
        }
        containerReference = null
        callback = null
        scope.cancel()
    }

    private fun observeLifecycle(owner: LifecycleOwner) {
        if (lifecycleOwner === owner) return
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        hasReceivedFirstResume = false
        observeLifecycleOwner(owner)
        owner.lifecycle.addObserver(lifecycleObserver)
    }

    private fun beginLoad(force: Boolean = false) {
        if (isDestroyed || (!force && _state.value == AdNativeState.Loading)) return

        val context = applicationContext ?: return
        if (!config.isEnabled || !canRequestAds(context)) {
            fail("Native ads are disabled, unavailable, or not consented")
            return
        }

        refreshTimerJob?.cancel()
        _state.value = AdNativeState.Loading
        containerReference?.get()?.let { container ->
            container.visibility = View.VISIBLE
            if (currentNativeAd == null) showShimmer(true)
        }

        val generation = ++requestGeneration
        factory.requestNativeAd(
            context = context,
            param = config.param,
            onLoaded = { nativeAd ->
                if (isDestroyed || generation != requestGeneration) {
                    nativeAd.destroy()
                    return@requestNativeAd
                }

                val container = containerReference?.get()
                if (container != null && !factory.populateNativeAdView(
                        context = context,
                        nativeAd = nativeAd,
                        container = container,
                        defaultLayoutId = config.layoutId,
                        mediationLayouts = config.mediationLayouts
                    )
                ) {
                    nativeAd.destroy()
                    fail("Failed to populate the native ad layout")
                    return@requestNativeAd
                }

                currentNativeAd?.takeIf { it !== nativeAd }?.destroy()
                currentNativeAd = nativeAd
                showShimmer(false)
                container?.visibility = View.VISIBLE
                _state.value = AdNativeState.Loaded(nativeAd)
                callback?.onAdLoaded(nativeAd)
                scheduleRefresh()
            },
            onFailed = { error ->
                if (isDestroyed || generation != requestGeneration) return@requestNativeAd
                fail(error)
            }
        )
    }

    private fun fail(error: String) {
        _state.value = AdNativeState.Fail(error)
        showShimmer(false)
        if (currentNativeAd == null) containerReference?.get()?.visibility = View.GONE
        callback?.onAdFailedToLoad(error)
        scheduleRefresh()
    }

    private fun scheduleRefresh() {
        refreshTimerJob?.cancel()
        if (isDestroyed || !config.allowReload || config.refreshIntervalMillis <= 0L) return
        if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == false) {
            return
        }

        refreshTimerJob = scope.launch {
            delay(config.refreshIntervalMillis)
            beginLoad(force = true)
        }
    }

    private fun showShimmer(show: Boolean) {
        val container = containerReference?.get() ?: return
        visitViews(container) { view ->
            if (view is NativeAdShimmerView) {
                view.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
    }

    private fun visitViews(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                visitViews(view.getChildAt(index), action)
            }
        }
    }
}
