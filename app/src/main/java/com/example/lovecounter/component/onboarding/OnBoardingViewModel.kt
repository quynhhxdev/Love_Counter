package com.example.lovecounter.component.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lovecounter.base.viewmodel.BaseViewModel
import com.example.lovecounter.data.model.OnBoarding
import com.example.lovecounter.domain.usecase.GetListOnBoardingUseCase
import com.example.lovecounter.domain.usecase.UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val getListOnBoardingUseCase: GetListOnBoardingUseCase
) : BaseViewModel() {

    private val _onBoardingList = MutableLiveData<List<OnBoarding>>()
    val onBoardingList: LiveData<List<OnBoarding>> get() = _onBoardingList

    fun loadOnBoarding() {
        viewModelScope.launch {
            val result = getListOnBoardingUseCase.execute(UseCase.Param())
            _onBoardingList.value = result
        }
    }
}
