package com.example.lovecounter.component.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lovecounter.base.viewmodel.BaseViewModel
import com.example.lovecounter.data.database.table.TableTestEntity
import com.example.lovecounter.domain.usecase.GetListTableTestUseCase
import com.example.lovecounter.domain.usecase.UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getListTableTestUseCase: GetListTableTestUseCase
) : BaseViewModel() {

    private val _tableTestList = MutableLiveData<List<TableTestEntity>>()

    fun loadData() {
        viewModelScope.launch {
            val result = getListTableTestUseCase.execute(UseCase.Param())
            _tableTestList.value = result
        }
    }
}
