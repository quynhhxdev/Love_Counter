package com.example.lovecounter.utils.ads

import com.example.lovecounter.BuildConfig

/**
 * AdPlacement — Đơn vị quản lý tập trung tất cả các Placement Keys (Tracking Tags)
 * và Ad Unit IDs (Google AdMob ID từ BuildConfig).
 */
object AdPlacement {

    // --- Placement Keys (Dùng làm Key phân biệt Ads Placement & Cache Tag) ---
    const val SPLASH_NATIVE = "splash_native"
    const val INTER_SPLASH = "inter_splash"
    const val LANGUAGE_NATIVE = "language_native"
    const val ONBOARDING_NATIVE = "onboarding_native"
    const val PERMISSION_NATIVE = "permission_native"
    const val MAIN_BANNER = "main_banner"
    const val MAIN_INTER = "main_inter"
    const val INTER_DAY = "inter_day"
    const val REWARD_UNLOCK = "reward_unlock"

    // Mặc định Placement Keys cho từng loại Ads
    const val DEFAULT_INTER = "default_inter"
    const val DEFAULT_REWARD = "default_reward"
    const val DEFAULT_NATIVE = "default_native"
    const val DEFAULT_BANNER = "default_banner"
    const val DEFAULT_APP_OPEN = "app_open_resume"

    // --- Ad Unit IDs (Lấy từ BuildConfig) ---
    val INTER_SPLASH_ID: String get() = BuildConfig.inter_splash
    val NATIVE_SPLASH_ID: String get() = BuildConfig.native_splash
    val INTER_MAIN_ID: String get() = BuildConfig.inter_main
    val APPOPEN_RESUME_ID: String get() = BuildConfig.appopen_resume
    val APP_OPEN_ID: String get() = BuildConfig.app_open
    val NATIVE_MAIN_ID: String get() = BuildConfig.native_main
    val NATIVE_LANGUAGE_ID: String get() = BuildConfig.native_language
    val NATIVE_ONBOARDING_ID: String get() = BuildConfig.native_onboarding
    val REWARD_UNLOCK_ID: String get() = BuildConfig.reward_unlock
    val BANNER_MAIN_ID: String get() = BuildConfig.banner_main
}
