package com.sweet.app.feature.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweet.app.core.datastore.DiscoveryScanType
import com.sweet.app.core.datastore.UserPreferences
import com.sweet.app.core.datastore.UserPreferencesRepository
import com.sweet.app.core.designsystem.CatppuccinVariant
import com.sweet.app.core.designsystem.CornerRadiusPreset
import com.sweet.app.core.designsystem.SweetAppearance
import com.sweet.app.core.network.nearby.NearbyPeer
import com.sweet.app.core.network.nearby.PeerDiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyRadarViewModel @Inject constructor(
    private val peerDiscoveryRepository: PeerDiscoveryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val peers: StateFlow<List<NearbyPeer>> = peerDiscoveryRepository.peers
    val isDiscovering: StateFlow<Boolean> = peerDiscoveryRepository.isDiscovering

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences.stateIn(
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

    private val _selectedPeer = MutableStateFlow<NearbyPeer?>(null)
    val selectedPeer: StateFlow<NearbyPeer?> = _selectedPeer.asStateFlow()

    private val _isListView = MutableStateFlow(false)
    val isListView: StateFlow<Boolean> = _isListView.asStateFlow()

    private val _blockedPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedPeerIds: StateFlow<Set<String>> = _blockedPeerIds.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                peerDiscoveryRepository.startDiscovery(
                    currentUserId = "usr_self_101",
                    displayName = "Me (You)"
                )
            }
        }
    }

    fun selectPeer(peer: NearbyPeer?) {
        _selectedPeer.value = peer
    }

    fun toggleListView() {
        _isListView.value = !_isListView.value
    }

    fun setScanType(type: DiscoveryScanType) {
        viewModelScope.launch {
            userPreferencesRepository.setDiscoveryScanType(type)
        }
    }

    fun blockPeer(userId: String) {
        _blockedPeerIds.value = _blockedPeerIds.value + userId
        if (_selectedPeer.value?.userId == userId) {
            _selectedPeer.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        peerDiscoveryRepository.stopDiscovery()
    }
}
