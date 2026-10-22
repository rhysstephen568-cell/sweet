package com.sweet.app.feature.settings.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweet.app.core.datastore.DiscoveryScanType
import com.sweet.app.core.datastore.UserPreferences
import com.sweet.app.core.datastore.UserPreferencesRepository
import com.sweet.app.core.designsystem.CatppuccinVariant
import com.sweet.app.core.designsystem.CornerRadiusPreset
import com.sweet.app.core.designsystem.SweetAppearance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(
            appearance = SweetAppearance.SWEET_LIGHT,
            followSystem = true,
            catppuccinVariant = CatppuccinVariant.MOCHA,
            fontScale = 1.0f,
            cornerRadiusPreset = CornerRadiusPreset.DEFAULT,
            discoveryScanType = DiscoveryScanType.RADIAL,
            discoverableLocalNetwork = "Everyone"
        )
    )

    fun selectScanType(type: DiscoveryScanType) {
        viewModelScope.launch {
            preferencesRepository.setDiscoveryScanType(type)
        }
    }

    fun selectDiscoverableLocalNetwork(option: String) {
        viewModelScope.launch {
            preferencesRepository.setDiscoverableLocalNetwork(option)
        }
    }
}
