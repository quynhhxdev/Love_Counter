package com.example.lovecounter.component.language

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lovecounter.base.viewmodel.BaseViewModel
import com.example.lovecounter.data.model.Language
import com.example.lovecounter.domain.usecase.GetListLanguageUseCase
import com.example.lovecounter.domain.usecase.UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val getListLanguageUseCase: GetListLanguageUseCase
) : BaseViewModel() {

    private val _languages = MutableLiveData<List<Language>>()
    val languages: LiveData<List<Language>> get() = _languages

    fun loadLanguages() {
        viewModelScope.launch {
            val result = getListLanguageUseCase.execute(UseCase.Param())
            _languages.value = result
        }
    }
}
