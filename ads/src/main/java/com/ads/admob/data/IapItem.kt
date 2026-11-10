package com.ads.admob.data

data class IapItem(
    val productId: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val productType: String = "inapp",
    val trialId: String? = null,
    val itemId: String = productId
) {
    val type: String get() = productType
}
