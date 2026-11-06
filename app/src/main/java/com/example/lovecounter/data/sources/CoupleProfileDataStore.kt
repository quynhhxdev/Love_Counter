package com.example.lovecounter.data.sources

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lovecounter.data.model.CoupleProfile
import com.example.lovecounter.data.model.HoroscopeSelection
import com.example.lovecounter.data.model.PersonProfile
import com.example.lovecounter.data.model.ZodiacSign
import com.example.lovecounter.utils.Constant
import com.example.lovecounter.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "couple_profile_pref",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                Constant.PREF_NAME,
                setOf(Constant.KEY_SP_CURRENT_LANGUAGE)
            )
        )
    }
)

private val LEGACY_LANGUAGE_KEY = stringPreferencesKey(Constant.KEY_SP_CURRENT_LANGUAGE)
private val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language")
private val LANGUAGE_CODE_REGEX = Regex("^[a-z]{2,3}([_-][a-zA-Z0-9]{2,8})*$")

@Singleton
class CoupleProfileDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val KEY_MALE_NAME = stringPreferencesKey("male_name")
        val KEY_MALE_GENDER = stringPreferencesKey("male_gender")
        val KEY_MALE_DOB = stringPreferencesKey("male_date_of_birth")

        val KEY_FEMALE_NAME = stringPreferencesKey("female_name")
        val KEY_FEMALE_GENDER = stringPreferencesKey("female_gender")
        val KEY_FEMALE_DOB = stringPreferencesKey("female_date_of_birth")
        val KEY_USER_HOROSCOPE = stringPreferencesKey("user_horoscope")
        val KEY_PARTNER_HOROSCOPE = stringPreferencesKey("partner_horoscope")
        suspend fun readLanguageCode(context: Context): String =
            context.dataStore.data.first().languageCode()
    }

    val languageCodeFlow: Flow<String> = context.dataStore.data.map { it.languageCode() }

    val coupleProfileFlow: Flow<CoupleProfile> = context.dataStore.data.map { preferences ->
        val maleName = preferences[KEY_MALE_NAME] ?: "Sasuke"
        val maleGender = preferences[KEY_MALE_GENDER] ?: "MALE"
        val maleDob = preferences[KEY_MALE_DOB] ?: ""

        val femaleName = preferences[KEY_FEMALE_NAME] ?: "Naruto"
        val femaleGender = preferences[KEY_FEMALE_GENDER] ?: "FEMALE"
        val femaleDob = preferences[KEY_FEMALE_DOB] ?: ""

        CoupleProfile(
            maleProfile = PersonProfile(name = maleName, gender = maleGender, dateOfBirth = maleDob),
            femaleProfile = PersonProfile(name = femaleName, gender = femaleGender, dateOfBirth = femaleDob)
        )
    }

    val horoscopeSelectionFlow: Flow<HoroscopeSelection> = context.dataStore.data.map { preferences ->
        HoroscopeSelection(
            user = ZodiacSign.fromId(preferences[KEY_USER_HOROSCOPE]) ?: ZodiacSign.ARIES,
            partner = ZodiacSign.fromId(preferences[KEY_PARTNER_HOROSCOPE]) ?: ZodiacSign.LEO
        )
    }

    suspend fun saveHoroscopeSelection(isUser: Boolean, zodiacSign: ZodiacSign) {
        context.dataStore.edit { preferences ->
            preferences[if (isUser) KEY_USER_HOROSCOPE else KEY_PARTNER_HOROSCOPE] = zodiacSign.name
        }
    }

    suspend fun saveLanguageCode(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] = languageCode
            preferences.remove(LEGACY_LANGUAGE_KEY)
        }
    }

    suspend fun saveCoupleProfile(coupleProfile: CoupleProfile) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MALE_NAME] = coupleProfile.maleProfile.name
            preferences[KEY_MALE_GENDER] = coupleProfile.maleProfile.gender
            preferences[KEY_MALE_DOB] = coupleProfile.maleProfile.dateOfBirth

            preferences[KEY_FEMALE_NAME] = coupleProfile.femaleProfile.name
            preferences[KEY_FEMALE_GENDER] = coupleProfile.femaleProfile.gender
            preferences[KEY_FEMALE_DOB] = coupleProfile.femaleProfile.dateOfBirth
        }
    }

}

private fun Preferences.languageCode(): String {
    val savedCode = this[SELECTED_LANGUAGE_KEY]
    if (savedCode != null) return savedCode

    val legacyJson = this[LEGACY_LANGUAGE_KEY] ?: return "en"
    val legacyCode = try {
        JSONObject(legacyJson).optString("languageCode")
    } catch (error: Exception) {
        Logger.e(
            "Failed to parse legacy saved language",
            throwable = error
        )
        null
    }
    return legacyCode?.takeIf { it.matches(LANGUAGE_CODE_REGEX) } ?: "en"
}