package com.ads.admob

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.ads.admob.config.NetworkProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.ads.mediation.admob.AdMobAdapter

@IntDef(
    BannerInlineStyle.SMALL,
    BannerInlineStyle.MEDIUM,
    BannerInlineStyle.LARGE
)
@Retention(AnnotationRetention.SOURCE)
annotation class BannerInlineStyle {
    companion object {
        const val SMALL = 0
        const val MEDIUM = 1
        const val LARGE = 2
    }
}

@StringDef(BannerCollapsibleGravity.TOP, BannerCollapsibleGravity.BOTTOM)
@Retention(AnnotationRetention.SOURCE)
annotation class BannerCollapsibleGravity {
    companion object {
        const val TOP = "top"
        const val BOTTOM = "bottom"
    }
}

@StringDef(RewardType.REWARDED, RewardType.REWARDED_INTERSTITIAL)
@Retention(AnnotationRetention.SOURCE)
annotation class RewardType {
    companion object {
        const val REWARDED = "rewarded"
        const val REWARDED_INTERSTITIAL = "rewarded_interstitial"
    }
}

fun Context.createAdRequest(
    @BannerCollapsibleGravity collapsibleGravity: String? = null
): AdRequest = AdmobExtension.createAdRequest(collapsibleGravity)

fun ViewGroup.getAnchoredAdaptiveBannerSize(): AdSize =
    AdmobExtension.getAnchoredAdaptiveBannerSize(context, this)

fun ViewGroup.getInlineAdaptiveBannerSize(
    @BannerInlineStyle style: Int = BannerInlineStyle.SMALL
): AdSize = AdmobExtension.getInlineAdaptiveBannerSize(context, this, style)

fun String.idToNetworkProvider(): NetworkProvider? = AdmobExtension.idToNetworkProvider(this)

object AdmobExtension {

    private const val APPLOVIN_MAX_ERROR_DOMAIN = "com.applovin"
    private val admobIdPattern = Regex("""^ca-app-pub-\d{16}(?:~\d{10}|/\d{10})$""")
    private val appLovinMaxIdPattern = Regex("""^[A-Za-z0-9]{16}$""")

    fun createAdRequest(
        @BannerCollapsibleGravity collapsibleGravity: String? = null
    ): AdRequest {
        val builder = AdRequest.Builder()
        when (collapsibleGravity) {
            null -> Unit
            BannerCollapsibleGravity.TOP, BannerCollapsibleGravity.BOTTOM -> {
                builder.addNetworkExtrasBundle(
                    AdMobAdapter::class.java,
                    Bundle().apply { putString("collapsible", collapsibleGravity) }
                )
            }
            else -> throw IllegalArgumentException("Unsupported collapsible banner gravity: $collapsibleGravity")
        }
        return builder.build()
    }

    fun getAdaptiveBannerSize(context: Context, container: ViewGroup): AdSize {
        return getAnchoredAdaptiveBannerSize(context, container)
    }

    fun getAnchoredAdaptiveBannerSize(context: Context, container: ViewGroup): AdSize {
        val widthDp = getAvailableWidthDp(context, container)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    fun getInlineAdaptiveBannerSize(
        context: Context,
        container: ViewGroup,
        @BannerInlineStyle style: Int = BannerInlineStyle.SMALL
    ): AdSize {
        val maxHeightDp = when (style) {
            BannerInlineStyle.SMALL -> 50
            BannerInlineStyle.MEDIUM -> 90
            BannerInlineStyle.LARGE -> 250
            else -> throw IllegalArgumentException("Unsupported inline banner style: $style")
        }
        return AdSize.getInlineAdaptiveBannerAdSize(
            getAvailableWidthDp(context, container),
            maxHeightDp
        )
    }

    fun appLovinMaxErrorToAdError(errorCode: Int, errorMessage: String): AdError {
        return AdError(errorCode, errorMessage, APPLOVIN_MAX_ERROR_DOMAIN)
    }

    fun idToNetworkProvider(adUnitId: String): NetworkProvider? {
        val normalizedId = adUnitId.trim()
        return when {
            admobIdPattern.matches(normalizedId) -> NetworkProvider.ADMOB
            appLovinMaxIdPattern.matches(normalizedId) -> NetworkProvider.MAX
            else -> null
        }
    }

    private fun getAvailableWidthDp(context: Context, container: ViewGroup): Int {
        val displayMetrics = context.resources.displayMetrics
        val containerWidth = container.width - container.paddingLeft - container.paddingRight
        val widthPixels = containerWidth.takeIf { it > 0 }
            ?: (displayMetrics.widthPixels - container.paddingLeft - container.paddingRight)
        return (widthPixels / displayMetrics.density).toInt().coerceAtLeast(1)
    }
}
