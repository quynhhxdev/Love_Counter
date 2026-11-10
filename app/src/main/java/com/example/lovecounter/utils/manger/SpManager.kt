package com.example.lovecounter.utils.manger

import android.content.SharedPreferences
import com.example.lovecounter.utils.Constant
import com.example.lovecounter.utils.handle.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

class SpManager(private val preferences: SharedPreferences) {

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun setLanguageChosen(isChosen: Boolean) {
        putBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, isChosen)
    }

    fun isLanguageChosen(): Boolean {
        return getBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, false)
    }

    fun setFirstOpen(isFirstOpen: Boolean) {
        putBoolean(Constant.KEY_SP_FIRST_OPEN, isFirstOpen)
    }

    fun isFirstOpen(): Boolean {
        return getBoolean(Constant.KEY_SP_FIRST_OPEN, true)
    }

    fun setOnBoardingCompleted(isCompleted: Boolean) {
        putBoolean(Constant.KEY_SP_ONBOARDING_COMPLETED, isCompleted)
    }

    fun isOnBoardingCompleted(): Boolean {
        return getBoolean(Constant.KEY_SP_ONBOARDING_COMPLETED, false)
    }

    fun setPermissionFlowCompleted(isCompleted: Boolean) {
        putBoolean(Constant.KEY_SP_PERMISSION_FLOW_COMPLETED, isCompleted)
    }

    fun isPermissionFlowCompleted(): Boolean {
        return getBoolean(Constant.KEY_SP_PERMISSION_FLOW_COMPLETED, false)
    }

    suspend fun saveStartDate(timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putLong(Constant.KEY_SP_START_DATE_TIMESTAMP, timestamp).commit()
    }

    fun getStartDate(): Long {
        return preferences.getLong(Constant.KEY_SP_START_DATE_TIMESTAMP, 0L)
    }

    val startDateFlow: Flow<Long>
        get() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == Constant.KEY_SP_START_DATE_TIMESTAMP) {
                    trySend(getStartDate())
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(getStartDate())
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun calculateDays(startDateTimestamp: Long): Long {
        return DateUtils.calculateDaysBetweenDates(startDateTimestamp, System.currentTimeMillis())
    }

    fun getUserName(): String {
        return getString(Constant.KEY_SP_USER_NAME, "Sasuke")
    }

    fun getPartnerName(): String {
        return getString(Constant.KEY_SP_PARTNER_NAME, "Naruto")
    }
}
