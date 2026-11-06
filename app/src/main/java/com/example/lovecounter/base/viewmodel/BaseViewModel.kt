package com.example.lovecounter.base.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    private val _showLoadingEvent = SingleLiveEvent<Boolean>()
    val showLoadingEvent: LiveData<Boolean> get() = _showLoadingEvent

    fun showLoading(show: Boolean) {
        _showLoadingEvent.value = show
    }

    protected fun <T : Any> processData(
        response: Result<T>,
        onSuccess: ((T?) -> Unit)?,
        onError: (() -> Unit)? = null
    ) {
        if (response.isSuccess) {
            onSuccess?.invoke(response.getOrNull())
        } else {
            onError?.invoke()
        }
    }
}
