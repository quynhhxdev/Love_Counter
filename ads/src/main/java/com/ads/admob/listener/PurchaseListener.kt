package com.ads.admob.listener

interface PurchaseListener {
    fun onPurchaseSuccess(productId: String)
    fun onPurchaseFailed(errorCode: Int, message: String)
}
