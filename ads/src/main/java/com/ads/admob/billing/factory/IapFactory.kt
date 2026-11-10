package com.ads.admob.billing.factory

import android.app.Activity
import android.content.Context
import com.ads.admob.listener.BillingClientConnectionListener
import com.ads.admob.listener.PurchaseListener
import com.ads.admob.listener.UpdatePurchaseListener
import kotlinx.coroutines.flow.StateFlow

interface IapFactory {
    val isNoAdsPurchased: StateFlow<Boolean>

    // Khởi tạo dịch vụ thanh toán
    fun initialize(context: Context)
    fun initBilling(context: Context)

    // Đăng ký listener nhận sự kiện kết nối và thanh toán
    fun registerBillingClientConnectionListener(listener: BillingClientConnectionListener)
    fun unregisterBillingClientConnectionListener(listener: BillingClientConnectionListener)
    fun registerPurchaseServiceListener(listener: PurchaseListener)
    fun unregisterPurchaseServiceListener(listener: PurchaseListener)
    fun registerUpdatePurchaseListener(listener: UpdatePurchaseListener)
    fun unregisterUpdatePurchaseListener(listener: UpdatePurchaseListener)

    // Thực hiện mua hàng
    fun buyIap(activity: Activity, productId: String, productType: String = "inapp")
    fun launchBillingFlow(activity: Activity, productId: String)

    // Kiểm tra và khôi phục trạng thái mua hàng
    fun isProductPurchased(productId: String): Boolean
    fun checkPurchases()
    fun queryAndRestorePurchases()
    fun restorePurchases()
}
