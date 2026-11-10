package com.ads.admob.widget

import android.graphics.Typeface
import android.graphics.drawable.Drawable

class NativeTemplateStyle private constructor(
    val mainBackground: Drawable?,
    val mainBackgroundColor: Int?,
    val primaryTextFont: Typeface?,
    val secondaryTextFont: Typeface?,
    val tertiaryTextFont: Typeface?,
    val primaryTextSize: Float,
    val secondaryTextSize: Float,
    val tertiaryTextSize: Float,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val tertiaryTextColor: Int,
    val hasPrimaryTextColor: Boolean,
    val hasSecondaryTextColor: Boolean,
    val hasTertiaryTextColor: Boolean,
    val callToActionTextSize: Float,
    val callToActionTextFont: Typeface?,
    val callToActionTextColor: Int,
    val hasCallToActionTextColor: Boolean,
    val callToActionBackgroundColor: Drawable?,
    val callToActionBackgroundTint: Int?
) {
    class Builder {
        private var mainBackground: Drawable? = null
        private var mainBackgroundColor: Int? = null
        private var primaryTextFont: Typeface? = null
        private var secondaryTextFont: Typeface? = null
        private var tertiaryTextFont: Typeface? = null
        private var primaryTextSize = 0f
        private var secondaryTextSize = 0f
        private var tertiaryTextSize = 0f
        private var primaryTextColor = 0
        private var secondaryTextColor = 0
        private var tertiaryTextColor = 0
        private var hasPrimaryTextColor = false
        private var hasSecondaryTextColor = false
        private var hasTertiaryTextColor = false
        private var callToActionTextSize = 0f
        private var callToActionTextFont: Typeface? = null
        private var callToActionTextColor = 0
        private var hasCallToActionTextColor = false
        private var callToActionBackgroundColor: Drawable? = null
        private var callToActionBackgroundTint: Int? = null

        fun withMainBackground(background: Drawable?) = apply {
            mainBackground = background
            mainBackgroundColor = null
        }

        fun withMainBackgroundColor(color: Int) = apply {
            mainBackground = null
            mainBackgroundColor = color
        }

        fun withPrimaryTextFont(font: Typeface?) = apply { primaryTextFont = font }
        fun withSecondaryTextFont(font: Typeface?) = apply { secondaryTextFont = font }
        fun withTertiaryTextFont(font: Typeface?) = apply { tertiaryTextFont = font }
        fun withPrimaryTextSize(size: Float) = apply { primaryTextSize = size }
        fun withSecondaryTextSize(size: Float) = apply { secondaryTextSize = size }
        fun withTertiaryTextSize(size: Float) = apply { tertiaryTextSize = size }

        fun withPrimaryTextColor(color: Int) = apply {
            primaryTextColor = color
            hasPrimaryTextColor = true
        }

        fun withSecondaryTextColor(color: Int) = apply {
            secondaryTextColor = color
            hasSecondaryTextColor = true
        }

        fun withTertiaryTextColor(color: Int) = apply {
            tertiaryTextColor = color
            hasTertiaryTextColor = true
        }

        fun withCallToActionTextSize(size: Float) = apply { callToActionTextSize = size }
        fun withCallToActionTextFont(font: Typeface?) = apply { callToActionTextFont = font }

        fun withCallToActionBackgroundColor(background: Drawable?) = apply {
            callToActionBackgroundColor = background
            callToActionBackgroundTint = null
        }

        fun withCallToActionBackgroundColor(color: Int) = apply {
            callToActionBackgroundColor = null
            callToActionBackgroundTint = color
        }

        fun withCallToActionTextColor(color: Int) = apply {
            callToActionTextColor = color
            hasCallToActionTextColor = true
        }

        fun build() = NativeTemplateStyle(
            mainBackground = mainBackground,
            mainBackgroundColor = mainBackgroundColor,
            primaryTextFont = primaryTextFont,
            secondaryTextFont = secondaryTextFont,
            tertiaryTextFont = tertiaryTextFont,
            primaryTextSize = primaryTextSize,
            secondaryTextSize = secondaryTextSize,
            tertiaryTextSize = tertiaryTextSize,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            tertiaryTextColor = tertiaryTextColor,
            hasPrimaryTextColor = hasPrimaryTextColor,
            hasSecondaryTextColor = hasSecondaryTextColor,
            hasTertiaryTextColor = hasTertiaryTextColor,
            callToActionTextSize = callToActionTextSize,
            callToActionTextFont = callToActionTextFont,
            callToActionTextColor = callToActionTextColor,
            hasCallToActionTextColor = hasCallToActionTextColor,
            callToActionBackgroundColor = callToActionBackgroundColor,
            callToActionBackgroundTint = callToActionBackgroundTint
        )
    }
}
