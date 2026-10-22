package com.sweet.app.feature.settings

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
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeState: StateFlow<UserPreferences> = preferencesRepository.userPreferences.stateIn(
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

    fun selectAppearance(appearance: SweetAppearance) {
        viewModelScope.launch {
            preferencesRepository.setAppearance(appearance)
        }
    }

    fun setFollowSystem(followSystem: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFollowSystem(followSystem)
        }
    }

    fun setCatppuccinVariant(variant: CatppuccinVariant) {
        viewModelScope.launch {
            preferencesRepository.setCatppuccinVariant(variant)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(scale)
        }
    }

    fun setCornerRadiusPreset(preset: CornerRadiusPreset) {
        viewModelScope.launch {
            preferencesRepository.setCornerRadiusPreset(preset)
        }
    }
}
