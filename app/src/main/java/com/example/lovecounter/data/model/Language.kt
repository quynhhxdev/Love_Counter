package com.example.lovecounter.data.model

data class Language(
    val languageName: String,
    val languageCode: String,
    val flagRes: Int,
    var isSelected: Boolean = false
)
