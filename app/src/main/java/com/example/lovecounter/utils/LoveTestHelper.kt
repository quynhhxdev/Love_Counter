package com.example.lovecounter.utils

import android.os.Bundle

enum class LoveTestType {
    NAME,
    HOROSCOPE
}

data class LoveTestResult(
    val score: Int,
    val adviceText: String,
    val firstSubject: String,
    val secondSubject: String,
    val testType: LoveTestType = LoveTestType.NAME,
    val firstZodiacId: String? = null,
    val secondZodiacId: String? = null
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_USER_NAME, firstSubject)
        putString(KEY_PARTNER_NAME, secondSubject)
        putInt(KEY_SCORE, score)
        putString(KEY_ADVICE, adviceText)
        putString(KEY_TEST_TYPE, testType.name)
        putString(KEY_FIRST_ZODIAC, firstZodiacId)
        putString(KEY_SECOND_ZODIAC, secondZodiacId)
    }

    companion object {
        const val KEY_USER_NAME = "userName"
        const val KEY_PARTNER_NAME = "partnerName"
        const val KEY_SCORE = "score"
        const val KEY_ADVICE = "adviceText"
        const val KEY_TEST_TYPE = "testType"
        const val KEY_FIRST_ZODIAC = "firstZodiacId"
        const val KEY_SECOND_ZODIAC = "secondZodiacId"

        fun fromBundle(bundle: Bundle?): LoveTestResult {
            val user = bundle?.getString(KEY_USER_NAME) ?: "Sasuke"
            val partner = bundle?.getString(KEY_PARTNER_NAME) ?: "Naruto"
            val score = bundle?.getInt(KEY_SCORE, 20) ?: 20
            val typeStr = bundle?.getString(KEY_TEST_TYPE)
            val testType = runCatching { LoveTestType.valueOf(typeStr ?: "") }.getOrDefault(LoveTestType.NAME)
            val advice = bundle?.getString(KEY_ADVICE) ?: LoveTestHelper.getAdvice(score, testType)
            val firstZodiac = bundle?.getString(KEY_FIRST_ZODIAC)
            val secondZodiac = bundle?.getString(KEY_SECOND_ZODIAC)
            return LoveTestResult(score, advice, user, partner, testType, firstZodiac, secondZodiac)
        }
    }
}

object LoveTestHelper {

    fun calculateNameCompatibility(yourName: String, partnerName: String): LoveTestResult {
        val combined = (yourName + partnerName).lowercase()
        // Generate score in 0..100 range based on hash
        val score = Math.floorMod(combined.hashCode(), 101)
        val advice = getNameTestAdvice(score)
        return LoveTestResult(
            score = score,
            adviceText = advice,
            firstSubject = yourName,
            secondSubject = partnerName,
            testType = LoveTestType.NAME
        )
    }

    fun calculateHoroscopeCompatibility(
        userZodiac: String,
        partnerZodiac: String,
        userZodiacId: String? = null,
        partnerZodiacId: String? = null
    ): LoveTestResult {
        val combined = (userZodiac + partnerZodiac).lowercase()
        // Generate score in 0..100 range based on hash
        val score = Math.floorMod(combined.hashCode(), 101)
        val advice = getHoroscopeTestAdvice(score)
        return LoveTestResult(
            score = score,
            adviceText = advice,
            firstSubject = userZodiac,
            secondSubject = partnerZodiac,
            testType = LoveTestType.HOROSCOPE,
            firstZodiacId = userZodiacId,
            secondZodiacId = partnerZodiacId
        )
    }

    fun getAdvice(score: Int, testType: LoveTestType = LoveTestType.NAME): String {
        return when (testType) {
            LoveTestType.NAME -> getNameTestAdvice(score)
            LoveTestType.HOROSCOPE -> getHoroscopeTestAdvice(score)
        }
    }

    fun getNameTestAdvice(score: Int): String = when {
        score in 0..20 -> "You should be friends not fall in love! It will either be the best relationship of your life, or you will lose that person forever. Sometimes, the greatest love stories are the ones that remain unspoken between best friends"
        score in 21..50 -> "More than friends, we should be our loves! Love story should be started writing now! Love is like the wind, you can’t see it but you can feel it."
        score in 51..80 -> "I want to hold you close to me and feel our hearts beat as one forever! You call it madness, but I call it love. We can only learn to love by loving"
        else -> "I was born to be your lover! You are the apple of my eyes! We must be our own before we can be another’s. My soul and your soul are forever tangled"
    }

    fun getHoroscopeTestAdvice(score: Int): String = when {
        score in 0..5 -> "Everything can become chaotic because it is difficult for them to trust each other and cannot sympathize with the other person's behavior. If there is a misunderstanding in a relationship, the emotions of both people often tend to go out of control, easily causing arguments to become fierce and lead to not very good results"
        score in 6..20 -> "In a relationship, both partners may try to change for each other, but eventually they may feel exhausted and unable to maintain a long-term relationship"
        score in 21..35 -> "Overall, both are stubborn but in different ways, lacking patience and understanding, feelings are hurt and distance appears in the relationship."
        score in 36..50 -> "The couple is not too intense, nor too sweet and melting, they are together, cultivating love with sympathy, understanding and connection."
        score in 51..70 -> "A beautiful, peaceful and harmonious love affair like a calm sea. They stay together, silently trying to achieve their goals.They stay together, silently trying to achieve their goals."
        score in 71..90 -> "Love each other in our own way, protect each other from the smallest things. Even when far away, both halves silently watch, waiting to support the other"
        else -> "They have many things in common, attracted to each other by harmony and passion. The couple is always full of bold ideas and willing to spend time implementing them togethe"
    }
}
