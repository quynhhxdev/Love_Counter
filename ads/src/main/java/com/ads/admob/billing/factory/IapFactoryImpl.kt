package com.ads.admob.billing.factory

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.listener.BillingClientConnectionListener
import com.ads.admob.listener.PurchaseListener
import com.ads.admob.listener.UpdatePurchaseListener
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class IapFactoryImpl private constructor() : IapFactory {

    private val TAG = "IapFactoryImpl"

    private val _isNoAdsPurchased = MutableStateFlow(false)
    override val isNoAdsPurchased: StateFlow<Boolean> = _isNoAdsPurchased.asStateFlow()

    private lateinit var billingClient: BillingClient
    private val cachedProductDetails = mutableMapOf<String, ProductDetails>()
    private val purchasedProductIds = mutableSetOf<String>()

    private val connectionListeners = CopyOnWriteArrayList<BillingClientConnectionListener>()
    private val purchaseListeners = CopyOnWriteArrayList<PurchaseListener>()
    private val updatePurchaseListeners = CopyOnWriteArrayList<UpdatePurchaseListener>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else {
            Log.e(TAG, "Purchases updated error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            notifyPurchaseFailed(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    override fun initialize(context: Context) {
        initBilling(context)
    }

    override fun initBilling(context: Context) {
        if (::billingClient.isInitialized && billingClient.isReady) {
            Log.d(TAG, "BillingClient already initialized and ready.")
            return
        }

        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully")
                    connectionListeners.forEach { it.onConnected() }
                    queryAndRestorePurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    connectionListeners.forEach { it.onDisconnected() }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected. Attempting reconnection...")
                connectionListeners.forEach { it.onDisconnected() }
                startConnection()
            }
        })
    }

    override fun registerBillingClientConnectionListener(listener: BillingClientConnectionListener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener)
        }
    }

    override fun unregisterBillingClientConnectionListener(listener: BillingClientConnectionListener) {
        connectionListeners.remove(listener)
    }

    override fun registerPurchaseServiceListener(listener: PurchaseListener) {
        if (!purchaseListeners.contains(listener)) {
            purchaseListeners.add(listener)
        }
    }

    override fun unregisterPurchaseServiceListener(listener: PurchaseListener) {
        purchaseListeners.remove(listener)
    }

    override fun registerUpdatePurchaseListener(listener: UpdatePurchaseListener) {
        if (!updatePurchaseListeners.contains(listener)) {
            updatePurchaseListeners.add(listener)
        }
    }

    override fun unregisterUpdatePurchaseListener(listener: UpdatePurchaseListener) {
        updatePurchaseListeners.remove(listener)
    }

    override fun checkPurchases() {
        queryAndRestorePurchases()
    }

    override fun restorePurchases() {
        queryAndRestorePurchases()
    }

    override fun queryAndRestorePurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient is not ready for queryAndRestorePurchases")
            return
        }

        // Truy vấn sản phẩm mua 1 lần (INAPP)
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchasesList)
            }
        }

        // Truy vấn gói thuê bao (SUBS)
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchasesList)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var noAdsPurchased = false

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                for (productId in purchase.products) {
                    purchasedProductIds.add(productId)
                    notifyPurchaseSuccess(productId)
                    if (productId == SKU_NO_ADS) {
                        noAdsPurchased = true
                    }
                }

                // Tự động xác nhận đơn hàng (Acknowledge)
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }

        if (noAdsPurchased) {
            _isNoAdsPurchased.value = true
            AdmobFactoryImpl.INSTANCE.cancelRequestAndShowAllAds()
        }

        notifyUpdatePurchases(_isNoAdsPurchased.value)
        Log.d(TAG, "Purchases handled. No Ads status: ${_isNoAdsPurchased.value}, All Purchased IDs: $purchasedProductIds")
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            Log.d(TAG, "Acknowledge purchase result: ${billingResult.responseCode}")
        }
    }

    override fun isProductPurchased(productId: String): Boolean {
        return purchasedProductIds.contains(productId) || (productId == SKU_NO_ADS && _isNoAdsPurchased.value)
    }

    override fun buyIap(activity: Activity, productId: String, productType: String) {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient is not ready for buyIap")
            notifyPurchaseFailed(-1, "BillingClient is not ready")
            return
        }

        val cachedDetails = cachedProductDetails[productId]
        if (cachedDetails != null) {
            launchFlowWithDetails(activity, cachedDetails)
        } else {
            val type = if (productType.lowercase() == "subs") BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
            queryProductDetails(listOf(productId), type) {
                val details = cachedProductDetails[productId]
                if (details != null) {
                    launchFlowWithDetails(activity, details)
                } else {
                    Log.e(TAG, "ProductDetails not found for $productId")
                    notifyPurchaseFailed(-1, "Product details not found for $productId")
                }
            }
        }
    }

    override fun launchBillingFlow(activity: Activity, productId: String) {
        buyIap(activity, productId)
    }

    private fun queryProductDetails(
        productIds: List<String>,
        productType: String,
        onComplete: (() -> Unit)? = null
    ) {
        val productList = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(productType)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (productDetails in productDetailsList) {
                    cachedProductDetails[productDetails.productId] = productDetails
                }
            } else {
                Log.e(TAG, "Failed to query ProductDetails: ${billingResult.debugMessage}")
            }
            onComplete?.invoke()
        }
    }

    private fun launchFlowWithDetails(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (productDetails.productType == BillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { token ->
                productDetailsParamsBuilder.setOfferToken(token)
            }
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: ${billingResult.debugMessage}")
            notifyPurchaseFailed(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    private fun notifyPurchaseSuccess(productId: String) {
        purchaseListeners.forEach { it.onPurchaseSuccess(productId) }
    }

    private fun notifyPurchaseFailed(errorCode: Int, message: String) {
        purchaseListeners.forEach { it.onPurchaseFailed(errorCode, message) }
    }

    private fun notifyUpdatePurchases(isPurchased: Boolean) {
        updatePurchaseListeners.forEach { it.onPurchasesUpdated(isPurchased) }
    }

    companion object {
        const val SKU_NO_ADS = "remove_ads_product_id"
        val INSTANCE: IapFactory by lazy { IapFactoryImpl() }
    }
}
