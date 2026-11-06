package com.example.lovecounter.data.model

import com.example.lovecounter.R

enum class ZodiacSign(
    val displayNameRes: Int,
    val iconRes: Int
) {
    ARIES(R.string.zodiac_aries, R.drawable.ic_aries),
    TAURUS(R.string.zodiac_taurus, R.drawable.ic_taurus),
    GEMINI(R.string.zodiac_gemini, R.drawable.ic_gemini),
    CANCER(R.string.zodiac_cancer, R.drawable.ic_cancer),
    LEO(R.string.zodiac_leo, R.drawable.ic_leo),
    VIRGO(R.string.zodiac_virgo, R.drawable.ic_virgo),
    LIBRA(R.string.zodiac_libra, R.drawable.ic_libra),
    SCORPIO(R.string.zodiac_scorpio, R.drawable.ic_scorpio),
    SAGITTARIUS(R.string.zodiac_sagittarius, R.drawable.ic_sagittarius),
    CAPRICORN(R.string.zodiac_capricorn, R.drawable.ic_capricorn),
    AQUARIUS(R.string.zodiac_aquarius, R.drawable.ic_aquarius),
    PISCES(R.string.zodiac_pisces, R.drawable.ic_pisces);

    companion object {
        fun fromId(id: String?): ZodiacSign? =
            entries.firstOrNull { it.name == id }
    }
}

data class HoroscopeSelection(
    val user: ZodiacSign = ZodiacSign.ARIES,
    val partner: ZodiacSign = ZodiacSign.LEO
)
