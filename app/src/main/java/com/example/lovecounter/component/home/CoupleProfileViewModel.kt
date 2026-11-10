package com.example.lovecounter.component.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovecounter.R
import com.example.lovecounter.data.model.PersonProfile
import com.example.lovecounter.data.repositories.AvatarRepository
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.utils.handle.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

sealed class CoupleProfileEvent {
    data object SaveSuccess : CoupleProfileEvent()
    data class ShowToast(val messageRes: Int, val formatArg: String? = null) : CoupleProfileEvent()
}

data class CoupleProfileUiState(
    val maleProfile: PersonProfile = PersonProfile(name = "Sasuke", gender = "MALE", dateOfBirth = ""),
    val femaleProfile: PersonProfile = PersonProfile(name = "Naruto", gender = "FEMALE", dateOfBirth = ""),
    val maleAvatarPath: String? = null,
    val femaleAvatarPath: String? = null,
    val isMaleTab: Boolean = true,
    val nameErrorRes: Int? = null,
    val birthdayError: String? = null,
    val isSaving: Boolean = false
) {
    val activeProfile: PersonProfile
        get() = if (isMaleTab) maleProfile else femaleProfile

    val activeAvatarPath: String?
        get() = if (isMaleTab) maleAvatarPath else femaleAvatarPath
}

@HiltViewModel
class CoupleProfileViewModel @Inject constructor(
    private val coupleProfileDataStore: CoupleProfileDataStore,
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoupleProfileUiState())
    val uiState: StateFlow<CoupleProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CoupleProfileEvent>()
    val events: SharedFlow<CoupleProfileEvent> = _events.asSharedFlow()

    fun init(isMaleTab: Boolean) {
        _uiState.update { it.copy(isMaleTab = isMaleTab) }
        loadCoupleProfile()
    }

    fun loadCoupleProfile() {
        viewModelScope.launch {
            val couple = coupleProfileDataStore.coupleProfileFlow.first()
            val avatars = avatarRepository.getAvatarPaths()
            _uiState.update {
                it.copy(
                    maleProfile = couple.maleProfile,
                    femaleProfile = couple.femaleProfile,
                    maleAvatarPath = avatars?.maleAvatarPath,
                    femaleAvatarPath = avatars?.femaleAvatarPath
                )
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { state ->
            if (state.isMaleTab) {
                state.copy(
                    maleProfile = state.maleProfile.copy(name = newName),
                    nameErrorRes = null
                )
            } else {
                state.copy(
                    femaleProfile = state.femaleProfile.copy(name = newName),
                    nameErrorRes = null
                )
            }
        }
    }

    fun onGenderChanged(newGender: String) {
        _uiState.update { state ->
            if (state.isMaleTab) {
                state.copy(maleProfile = state.maleProfile.copy(gender = newGender))
            } else {
                state.copy(femaleProfile = state.femaleProfile.copy(gender = newGender))
            }
        }
    }

    fun onBirthdayChanged(newBirthday: String) {
        _uiState.update { state ->
            if (state.isMaleTab) {
                state.copy(
                    maleProfile = state.maleProfile.copy(dateOfBirth = newBirthday),
                    birthdayError = null
                )
            } else {
                state.copy(
                    femaleProfile = state.femaleProfile.copy(dateOfBirth = newBirthday),
                    birthdayError = null
                )
            }
        }
    }

    fun onBirthdaySelectedMillis(timeMillis: Long) {
        onBirthdayChanged(DateUtils.formatDate(timeMillis, Locale.US))
    }

    fun getInitialBirthdayMillis(): Long {
        val dob = _uiState.value.activeProfile.dateOfBirth
        return DateUtils.parseDate(dob)?.timeInMillis ?: System.currentTimeMillis()
    }

    fun saveProfile(pendingAvatar: File? = null) {
        val state = _uiState.value
        val name = state.activeProfile.name.trim()

        if (name.isEmpty()) {
            _uiState.update { it.copy(nameErrorRes = R.string.name_cannot_be_empty) }
            viewModelScope.launch {
                _events.emit(CoupleProfileEvent.ShowToast(R.string.please_enter_name))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val current = coupleProfileDataStore.coupleProfileFlow.first()
                val updatedCouple = if (state.isMaleTab) {
                    current.copy(maleProfile = state.maleProfile.copy(name = name))
                } else {
                    current.copy(femaleProfile = state.femaleProfile.copy(name = name))
                }
                coupleProfileDataStore.saveCoupleProfile(updatedCouple)
                val avatarPath = pendingAvatar?.let {
                    avatarRepository.saveAvatar(state.isMaleTab, it)
                }
                if (avatarPath != null) {
                    _uiState.update {
                        if (state.isMaleTab) it.copy(maleAvatarPath = avatarPath)
                        else it.copy(femaleAvatarPath = avatarPath)
                    }
                }
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(CoupleProfileEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(
                    CoupleProfileEvent.ShowToast(
                        R.string.failed_to_save_profile,
                        e.message.orEmpty()
                    )
                )
            }
        }
    }
}
