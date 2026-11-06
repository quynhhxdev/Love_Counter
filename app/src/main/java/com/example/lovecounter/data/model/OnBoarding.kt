package com.example.lovecounter.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnBoarding(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val imageRes: Int
)
