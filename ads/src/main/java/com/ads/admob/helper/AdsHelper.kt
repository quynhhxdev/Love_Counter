package com.ads.admob.helper

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.billing.factory.IapFactoryImpl
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.config.FirebaseConfigManager
import com.ads.admob.helper.params.IAdsParam
import java.lang.ref.WeakReference

abstract class AdsHelper<P : IAdsParam> : DefaultLifecycleObserver {
    @Volatile
    var flagActive: Boolean = true
        protected set

    @Volatile
    var canReloadAd: Boolean = true
        protected set

    private var lifecycleOwnerReference: WeakReference<LifecycleOwner>? = null

    protected open fun canShowAds(context: Context, configAllowsAds: Boolean = true): Boolean {
        observeLifecycleOwner(context as? LifecycleOwner)
        return flagActive &&
            configAllowsAds &&
            FirebaseConfigManager.isAdsEnabled() &&
            !isUserPurchasedNoAds(context)
    }

    protected open fun canRequestAds(context: Context, configAllowsAds: Boolean = true): Boolean =
        canShowAds(context, configAllowsAds) &&
            isNetworkAvailable(context) &&
            isGdprConsentGranted(context)

    protected fun observeLifecycleOwner(owner: LifecycleOwner?) {
        if (owner == null || lifecycleOwnerReference?.get() === owner) return
        detachLifecycleOwner()
        lifecycleOwnerReference = WeakReference(owner)
        flagActive = true
        owner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (lifecycleOwnerReference?.get() === owner) detachLifecycleOwner()
        else owner.lifecycle.removeObserver(this)
    }

    protected fun detachLifecycleOwner() {
        lifecycleOwnerReference?.get()?.lifecycle?.removeObserver(this)
        lifecycleOwnerReference?.clear()
        lifecycleOwnerReference = null
        flagActive = false
    }

    @SuppressLint("MissingPermission")
    protected open fun isNetworkAvailable(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (error: SecurityException) {
            logZ("Unable to verify network connectivity", error)
            return false
        }
    }

    protected open fun isUserPurchasedNoAds(context: Context): Boolean {
        return IapFactoryImpl.INSTANCE.isProductPurchased(IapFactoryImpl.SKU_NO_ADS) ||
            IapFactoryImpl.INSTANCE.isNoAdsPurchased.value
    }

    protected open fun isGdprConsentGranted(context: Context): Boolean {
        return ConsentManager.canRequestAds(context)
    }

    protected fun logZ(message: String, error: Throwable? = null) {
        val tag = javaClass.simpleName.ifEmpty { "AdsHelper" }
        if (error == null) Log.d(tag, message) else Log.w(tag, message, error)
    }
}
