package com.sweet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sweet.app.core.designsystem.SweetTheme
import com.sweet.app.core.designsystem.getAppearanceColors
import com.sweet.app.feature.navigation.SweetNavGraph
import com.sweet.app.feature.settings.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeState by themeViewModel.themeState.collectAsState()
            val systemIsDark = isSystemInDarkTheme()

            val isDarkTheme = if (themeState.followSystem) {
                systemIsDark
            } else {
                getAppearanceColors(
                    appearance = themeState.appearance,
                    catppuccinVariant = themeState.catppuccinVariant,
                    isDarkTheme = systemIsDark
                ).isDark
            }

            SweetTheme(
                appearance = themeState.appearance,
                catppuccinVariant = themeState.catppuccinVariant,
                cornerRadiusPreset = themeState.cornerRadiusPreset,
                isDarkTheme = isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SweetNavGraph(themeViewModel = themeViewModel)
                }
            }
        }
    }
}
