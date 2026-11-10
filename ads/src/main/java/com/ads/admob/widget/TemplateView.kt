package com.ads.admob.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.ads.admob.R

class TemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    templateSize: TemplateSize = TemplateSize.MEDIUM
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class TemplateSize { SMALL, MEDIUM }

    constructor(context: Context, templateSize: TemplateSize) : this(context, null, 0, templateSize)

    private var selectedSize = templateSize
    private var nativeAd: NativeAd? = null
    private var styles: NativeTemplateStyle? = null
    private var nativeAdView: NativeAdView? = null
    private var primaryView: TextView? = null
    private var secondaryView: TextView? = null
    private var tertiaryView: TextView? = null
    private var ratingView: RatingBar? = null
    private var iconView: ImageView? = null
    private var mediaView: MediaView? = null
    private var callToActionView: TextView? = null

    init {
        inflateTemplate()
    }

    fun setTemplateSize(size: TemplateSize) {
        if (selectedSize == size) return
        selectedSize = size
        reloadTemplate()
    }

    fun setNativeAd(ad: NativeAd) {
        nativeAd = ad
        bindNativeAd(ad)
    }

    fun setStyles(style: NativeTemplateStyle) {
        styles = style
        applyStyles(style)
    }

    fun clearNativeAd() {
        nativeAd = null
        reloadTemplate()
    }

    fun destroyNativeAd() {
        nativeAd?.destroy()
        clearNativeAd()
    }

    private fun reloadTemplate() {
        removeAllViews()
        inflateTemplate()
        styles?.let(::applyStyles)
        nativeAd?.let(::bindNativeAd)
    }

    private fun inflateTemplate() {
        val layout = when (selectedSize) {
            TemplateSize.SMALL -> R.layout.custom_native_admob_small
            TemplateSize.MEDIUM -> R.layout.custom_native_admod_medium
        }
        LayoutInflater.from(context).inflate(layout, this, true)
        nativeAdView = findViewById(R.id.nativeAdView)
        primaryView = findViewById(R.id.ad_headline)
        secondaryView = findViewById(R.id.ad_body)
        tertiaryView = findViewById(R.id.ad_advertiser)
        ratingView = findViewById(R.id.ad_stars)
        iconView = findViewById(R.id.ad_app_icon)
        mediaView = findViewById(R.id.ad_media)
        callToActionView = findViewById(R.id.ad_call_to_action)
    }

    private fun bindNativeAd(ad: NativeAd) {
        val adView = nativeAdView ?: return
        adView.headlineView = primaryView
        adView.bodyView = secondaryView
        adView.advertiserView = tertiaryView
        adView.starRatingView = ratingView
        adView.iconView = iconView
        adView.mediaView = mediaView
        adView.callToActionView = callToActionView

        setText(primaryView, ad.headline)
        setText(secondaryView, ad.body)
        setText(tertiaryView, ad.advertiser)

        iconView?.apply {
            setImageDrawable(ad.icon?.drawable)
            visibility = if (ad.icon == null) View.GONE else View.VISIBLE
        }
        ratingView?.apply {
            rating = ad.starRating?.toFloat() ?: 0f
            visibility = if (ad.starRating == null) View.GONE else View.VISIBLE
        }
        mediaView?.visibility =
            if (ad.mediaContent == null) View.GONE else View.VISIBLE
        callToActionView?.apply {
            text = ad.callToAction
            visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        adView.setNativeAd(ad)
    }

    private fun setText(view: TextView?, value: String?) {
        view?.apply {
            text = value
            visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun applyStyles(style: NativeTemplateStyle) {
        nativeAdView?.apply {
            when {
                style.mainBackground != null -> background = style.mainBackground
                style.mainBackgroundColor != null -> setBackgroundColor(style.mainBackgroundColor)
            }
        }
        applyTextStyle(
            primaryView,
            style.primaryTextFont,
            style.primaryTextSize,
            style.primaryTextColor,
            style.hasPrimaryTextColor
        )
        applyTextStyle(
            secondaryView,
            style.secondaryTextFont,
            style.secondaryTextSize,
            style.secondaryTextColor,
            style.hasSecondaryTextColor
        )
        applyTextStyle(
            tertiaryView,
            style.tertiaryTextFont,
            style.tertiaryTextSize,
            style.tertiaryTextColor,
            style.hasTertiaryTextColor
        )
        callToActionView?.apply {
            style.callToActionTextFont?.let(::setTypeface)
            if (style.callToActionTextSize > 0f) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, style.callToActionTextSize)
            }
            if (style.hasCallToActionTextColor) setTextColor(style.callToActionTextColor)
            style.callToActionBackgroundColor?.let { background = it }
            style.callToActionBackgroundTint?.let {
                backgroundTintList = android.content.res.ColorStateList.valueOf(it)
            }
        }
    }

    private fun applyTextStyle(
        view: TextView?,
        typeface: Typeface?,
        textSize: Float,
        textColor: Int,
        hasTextColor: Boolean
    ) {
        view?.apply {
            typeface?.let(::setTypeface)
            if (textSize > 0f) setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            if (hasTextColor) setTextColor(textColor)
        }
    }
}
