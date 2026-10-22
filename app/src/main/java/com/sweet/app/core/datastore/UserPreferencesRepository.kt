package com.sweet.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sweet.app.core.designsystem.CatppuccinVariant
import com.sweet.app.core.designsystem.CornerRadiusPreset
import com.sweet.app.core.designsystem.SweetAppearance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class DiscoveryScanType(val displayName: String, val description: String) {
    RADIAL("Radial Scan", "Concentric proximity tier rings based on latency"),
    PULSE_360("360° Pulse Scan", "Expanding 360-degree radar wave visualization")
}

data class UserPreferences(
    val appearance: SweetAppearance,
    val followSystem: Boolean,
    val catppuccinVariant: CatppuccinVariant,
    val fontScale: Float,
    val cornerRadiusPreset: CornerRadiusPreset,
    val discoveryScanType: DiscoveryScanType,
    val discoverableLocalNetwork: String
)

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setAppearance(appearance: SweetAppearance)
    suspend fun setFollowSystem(followSystem: Boolean)
    suspend fun setCatppuccinVariant(variant: CatppuccinVariant)
    suspend fun setFontScale(scale: Float)
    suspend fun setCornerRadiusPreset(preset: CornerRadiusPreset)
    suspend fun setDiscoveryScanType(type: DiscoveryScanType)
    suspend fun setDiscoverableLocalNetwork(option: String)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val APPEARANCE = stringPreferencesKey("appearance")
        val FOLLOW_SYSTEM = booleanPreferencesKey("follow_system")
        val CATPPUCCIN_VARIANT = stringPreferencesKey("catppuccin_variant")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CORNER_RADIUS_PRESET = stringPreferencesKey("corner_radius_preset")
        val DISCOVERY_SCAN_TYPE = stringPreferencesKey("discovery_scan_type")
        val DISCOVERABLE_LOCAL_NETWORK = stringPreferencesKey("discoverable_local_network")
    }

    override val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val appearanceName = prefs[PreferencesKeys.APPEARANCE] ?: SweetAppearance.SWEET_LIGHT.name
        val appearance = runCatching { SweetAppearance.valueOf(appearanceName) }.getOrDefault(SweetAppearance.SWEET_LIGHT)

        val followSystem = prefs[PreferencesKeys.FOLLOW_SYSTEM] ?: true

        val catVariantName = prefs[PreferencesKeys.CATPPUCCIN_VARIANT] ?: CatppuccinVariant.MOCHA.name
        val catVariant = runCatching { CatppuccinVariant.valueOf(catVariantName) }.getOrDefault(CatppuccinVariant.MOCHA)

        val fontScale = prefs[PreferencesKeys.FONT_SCALE] ?: 1.0f

        val presetName = prefs[PreferencesKeys.CORNER_RADIUS_PRESET] ?: CornerRadiusPreset.DEFAULT.name
        val preset = runCatching { CornerRadiusPreset.valueOf(presetName) }.getOrDefault(CornerRadiusPreset.DEFAULT)

        val scanTypeName = prefs[PreferencesKeys.DISCOVERY_SCAN_TYPE] ?: DiscoveryScanType.RADIAL.name
        val scanType = runCatching { DiscoveryScanType.valueOf(scanTypeName) }.getOrDefault(DiscoveryScanType.RADIAL)

        val discoverableLocalNetwork = prefs[PreferencesKeys.DISCOVERABLE_LOCAL_NETWORK] ?: "Everyone"

        UserPreferences(
            appearance = appearance,
            followSystem = followSystem,
            catppuccinVariant = catVariant,
            fontScale = fontScale,
            cornerRadiusPreset = preset,
            discoveryScanType = scanType,
            discoverableLocalNetwork = discoverableLocalNetwork
        )
    }

    override suspend fun setAppearance(appearance: SweetAppearance) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APPEARANCE] = appearance.name
        }
    }

    override suspend fun setFollowSystem(followSystem: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOLLOW_SYSTEM] = followSystem
        }
    }

    override suspend fun setCatppuccinVariant(variant: CatppuccinVariant) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CATPPUCCIN_VARIANT] = variant.name
        }
    }

    override suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FONT_SCALE] = scale
        }
    }

    override suspend fun setCornerRadiusPreset(preset: CornerRadiusPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CORNER_RADIUS_PRESET] = preset.name
        }
    }

    override suspend fun setDiscoveryScanType(type: DiscoveryScanType) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DISCOVERY_SCAN_TYPE] = type.name
        }
    }

    override suspend fun setDiscoverableLocalNetwork(option: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DISCOVERABLE_LOCAL_NETWORK] = option
        }
    }
}
