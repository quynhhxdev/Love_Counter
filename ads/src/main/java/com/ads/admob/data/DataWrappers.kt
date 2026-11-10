package com.ads.admob.data

import com.android.billingclient.api.ProductDetails as GoogleProductDetails
import com.android.billingclient.api.Purchase as GooglePurchase

data class ProductDetails(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val priceAmount: Double,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val billingPeriod: String? = null,
    val offerToken: String? = null,
    val productType: String? = null
) {
    companion object {
        fun fromGooglePlay(
            product: GoogleProductDetails,
            offerToken: String? = null,
            productType: String? = null
        ): ProductDetails {
            val offer = product.subscriptionOfferDetails
                ?.firstOrNull { offerToken == null || it.offerToken == offerToken }
            val phase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
            val oneTimeOffer = product.oneTimePurchaseOfferDetails
            val amountMicros = phase?.priceAmountMicros ?: oneTimeOffer?.priceAmountMicros ?: 0L

            return ProductDetails(
                productId = product.productId,
                title = product.title,
                description = product.description,
                price = phase?.formattedPrice ?: oneTimeOffer?.formattedPrice.orEmpty(),
                priceAmount = amountMicros / 1_000_000.0,
                priceAmountMicros = amountMicros,
                currencyCode = phase?.priceCurrencyCode
                    ?: oneTimeOffer?.priceCurrencyCode
                    ?: "",
                billingPeriod = phase?.billingPeriod,
                offerToken = offer?.offerToken,
                productType = productType
            )
        }
    }
}

data class PurchaseInfo(
    val orderId: String?,
    val purchaseToken: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val isAcknowledged: Boolean,
    val signature: String,
    val originalJson: String,
    val packageName: String,
    val productIds: List<String>,
    val isAutoRenewing: Boolean,
    val quantity: Int,
    val obfuscatedAccountId: String? = null,
    val obfuscatedProfileId: String? = null
) {
    companion object {
        fun fromGooglePlay(purchase: GooglePurchase): PurchaseInfo {
            val accountIdentifiers = purchase.accountIdentifiers
            return PurchaseInfo(
                orderId = purchase.orderId,
                purchaseToken = purchase.purchaseToken,
                purchaseTime = purchase.purchaseTime,
                purchaseState = purchase.purchaseState,
                isAcknowledged = purchase.isAcknowledged,
                signature = purchase.signature,
                originalJson = purchase.originalJson,
                packageName = purchase.packageName,
                productIds = purchase.products.toList(),
                isAutoRenewing = purchase.isAutoRenewing,
                quantity = purchase.quantity,
                obfuscatedAccountId = accountIdentifiers?.obfuscatedAccountId,
                obfuscatedProfileId = accountIdentifiers?.obfuscatedProfileId
            )
        }
    }
}
