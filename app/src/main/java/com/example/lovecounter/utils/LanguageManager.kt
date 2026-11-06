package com.example.lovecounter.utils

import android.content.Context
import com.example.lovecounter.data.repositories.LocalRepository
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val spManager: SpManager,
    private val localRepository: LocalRepository,
    private val coupleProfileDataStore: CoupleProfileDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _languageCode = MutableStateFlow(DEFAULT_LANGUAGE_CODE)

    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        scope.launch {
            coupleProfileDataStore.languageCodeFlow
                .catch { error -> Logger.e("Failed to observe saved language", throwable = error) }
                .collect { languageCode ->
                    withContext(Dispatchers.Main.immediate) {
                        LocaleHelper.setLocale(appContext, languageCode)
                        _languageCode.value = languageCode
                    }
                }
        }
    }

    suspend fun changeLanguage(languageCode: String) {
        val normalizedCode = languageCode.trim().lowercase(Locale.ROOT)
        require(LANGUAGE_CODE_REGEX.matches(normalizedCode)) {
            "Invalid language code: $languageCode"
        }

        val selectedLanguage = localRepository.getListLanguage()
            .firstOrNull { it.languageCode == normalizedCode }
            ?: throw IllegalArgumentException("Unsupported language code: $normalizedCode")

        coupleProfileDataStore.saveLanguageCode(selectedLanguage.languageCode)
        spManager.setLanguageChosen(true)
        LocaleHelper.setLocale(appContext, normalizedCode)
        _languageCode.value = normalizedCode
    }

    fun localizedContext(context: Context, languageCode: String = _languageCode.value): Context {
        return LocaleHelper.setLocale(context, languageCode)
    }

    companion object {
        private const val DEFAULT_LANGUAGE_CODE = "en"
        private val LANGUAGE_CODE_REGEX = Regex("^[a-z]{2,3}([_-][a-zA-Z0-9]{2,8})*$")
    }
}
