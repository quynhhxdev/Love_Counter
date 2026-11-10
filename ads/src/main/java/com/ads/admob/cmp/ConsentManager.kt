package com.ads.admob.cmp

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {

    private const val TAG = "ConsentManager"
    private const val TCF_PURPOSE_CONSENTS_KEY = "IABTCF_PurposeConsents"

    /**
     * Kiểm tra ứng dụng có đủ quyền hạn hiển thị quảng cáo hay chưa.
     */
    fun canRequestAds(context: Context): Boolean {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        return consentInformation.canRequestAds() ||
                consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
    }

    /**
     * Khởi tạo cập nhật đồng thuận cho bản Release (môi trường sản phẩm thực tế).
     */
    fun initReleaseConsent(
        activity: Activity,
        callback: OnConsentResponse
    ) {
        requestConsentInternal(
            activity = activity,
            debugSettings = null,
            callback = callback
        )
    }

    /**
     * Khởi tạo cập nhật đồng thuận cho bản Debug / Thử nghiệm (giả lập vùng EEA).
     */
    fun initDebugConsent(
        activity: Activity,
        testDeviceHashedId: String? = null,
        callback: OnConsentResponse
    ) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .apply {
                if (!testDeviceHashedId.isNullOrBlank()) {
                    addTestDeviceHashedId(testDeviceHashedId)
                }
            }
            .build()

        requestConsentInternal(
            activity = activity,
            debugSettings = debugSettings,
            callback = callback
        )
    }

    /**
     * Phương thức khởi tạo đồng thuận tổng quát (Hỗ trợ tương thích ngược).
     */
    fun gatherConsent(
        activity: Activity,
        isTestDevice: Boolean = true,
        testDeviceHashedId: String? = null,
        callback: OnConsentResponse
    ) {
        if (isTestDevice) {
            initDebugConsent(activity, testDeviceHashedId, callback)
        } else {
            initReleaseConsent(activity, callback)
        }
    }

    private fun requestConsentInternal(
        activity: Activity,
        debugSettings: ConsentDebugSettings?,
        callback: OnConsentResponse
    ) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (debugSettings != null) {
                    setConsentDebugSettings(debugSettings)
                }
            }
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                val isRequired = checkForPrivacyOptions(activity)
                callback.onPolicyRequired(isRequired)

                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    val errorMsg = loadAndShowError?.message
                    if (errorMsg != null) {
                        Log.e(TAG, "Consent form error: $errorMsg")
                    }
                    val canRequest = canRequestAds(activity)
                    callback.onResponse(errorMsg)
                    callback.onConsentGathered(canRequest)
                }
            },
            { requestConsentError ->
                val errorMsg = requestConsentError.message
                Log.e(TAG, "Consent info update error: $errorMsg")
                val isRequired = checkForPrivacyOptions(activity)
                callback.onPolicyRequired(isRequired)
                val canRequest = canRequestAds(activity)
                callback.onResponse(errorMsg)
                callback.onConsentGathered(canRequest)
            }
        )
    }

    /**
     * Kiểm tra xem người dùng hiện tại có cần hiển thị nút Tùy chọn quyền riêng tư (Privacy Options) hay không.
     */
    fun checkForPrivacyOptions(context: Context): Boolean {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Hiển thị lại biểu mẫu Tùy chọn quyền riêng tư (Privacy Options Form) để người dùng thay đổi quyết định.
     */
    fun launchPrivacyForm(
        activity: Activity,
        onComplete: ((String?) -> Unit)? = null
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            val errorMsg = formError?.message
            if (errorMsg != null) {
                Log.e(TAG, "Error showing privacy options form: $errorMsg")
            } else {
                Log.d(TAG, "Privacy options form dismissed successfully.")
            }
            onComplete?.invoke(errorMsg)
        }
    }

    /**
     * Đọc chuỗi đồng thuận IAB TCF từ SharedPreferences mặc định.
     */
    fun getTcfPurposeConsent(context: Context): String? {
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        return prefs.getString(TCF_PURPOSE_CONSENTS_KEY, null)
    }

    /**
     * Kiểm tra xem mục đích (Purpose) TCF cụ thể (1-based index) có được chấp thuận hay không.
     */
    fun isPurposeConsentGranted(context: Context, purposeIndex: Int): Boolean {
        val purposeConsents = getTcfPurposeConsent(context) ?: return false
        val charIndex = purposeIndex - 1
        return charIndex in purposeConsents.indices && purposeConsents[charIndex] == '1'
    }
}
