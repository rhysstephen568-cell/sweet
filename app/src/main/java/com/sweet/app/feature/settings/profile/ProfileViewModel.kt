package com.sweet.app.feature.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweet.app.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val displayName: String = "SWEET User",
    val username: String = "sweet_user",
    val bio: String = "Hey there! I am using SWEET.",
    val phoneNumber: String = "+234 800 123 4567",
    val discoverableLocalNetwork: String = "Contacts",
    val photoVisibility: String = "Everyone",
    val isUsernameAvailable: Boolean = true,
    val isCheckingUsername: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isSavedSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _usernameQuery = MutableStateFlow(_uiState.value.username)

    @OptIn(FlowPreview::class)
    val usernameValidation: StateFlow<Boolean> = _usernameQuery
        .debounce(400)
        .map { username ->
            _uiState.update { it.copy(isCheckingUsername = true) }
            val isValid = username.length in 5..32 && username.matches(Regex("^[a-zA-Z0-9_]+$"))
            _uiState.update { it.copy(isCheckingUsername = false, isUsernameAvailable = isValid) }
            isValid
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onDisplayNameChanged(name: String) {
        if (name.length <= 50) {
            _uiState.update { it.copy(displayName = name, hasUnsavedChanges = true) }
        }
    }

    fun onUsernameChanged(username: String) {
        if (username.length <= 32) {
            _uiState.update { it.copy(username = username, hasUnsavedChanges = true) }
            _usernameQuery.value = username
        }
    }

    fun onBioChanged(bio: String) {
        if (bio.length <= 140) {
            _uiState.update { it.copy(bio = bio, hasUnsavedChanges = true) }
        }
    }

    fun onDiscoverableLocalNetworkChanged(option: String) {
        _uiState.update { it.copy(discoverableLocalNetwork = option, hasUnsavedChanges = true) }
    }

    fun onPhotoVisibilityChanged(option: String) {
        _uiState.update { it.copy(photoVisibility = option, hasUnsavedChanges = true) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(hasUnsavedChanges = false, isSavedSuccess = true) }
        }
    }

    fun discardChanges() {
        _uiState.update { it.copy(hasUnsavedChanges = false) }
    }
}
