package com.example.lovecounter.data.sources

import android.Manifest
import android.os.Build
import com.example.lovecounter.R
import com.example.lovecounter.data.database.AppDatabase
import com.example.lovecounter.data.database.table.TableTestEntity
import com.example.lovecounter.data.model.Language
import com.example.lovecounter.data.model.OnBoarding
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSource @Inject constructor(
    private val appDatabase: AppDatabase
) {

    fun getListLanguage(): List<Language> {
        return listOf(
            "en" to R.drawable.ic_language_eng,
            "hi" to R.drawable.ic_language_hindi,
            "es" to R.drawable.ic_language_span,
            "fr" to R.drawable.ic_language_french,
            "pt" to R.drawable.ic_language_portuguese,
            "id" to R.drawable.ic_language_indonesian,
            "de" to R.drawable.ic_language_german,
        ).map { (languageCode, flagRes) ->
            val languageLocale = Locale.forLanguageTag(languageCode)
            Language(
                languageName = languageLocale.getDisplayName(languageLocale),
                languageCode = languageCode,
                flagRes = flagRes
            )
        }
    }

    fun getListOnBoarding(): List<OnBoarding> {
        return listOf(
            OnBoarding(
                R.string.onboarding_title_1,
                R.string.onboarding_subtitle_1,
                R.drawable.onb1
            ),
            OnBoarding(
                R.string.onboarding_title_2,
                R.string.onboarding_subtitle_2,
                R.drawable.onb2
            ),
            OnBoarding(
                R.string.onboarding_title_3,
                R.string.onboarding_subtitle_3,
                R.drawable.ic_avt_couple_watch_phone
            )
        )
    }

    suspend fun getListTableTest(): List<TableTestEntity> {
        return appDatabase.testDao().getAll()
    }
}
