package com.sweet.app.feature.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sweet.app.feature.chat.ChatViewModel
import com.sweet.app.feature.chat.ConversationScreen
import com.sweet.app.feature.nearby.NearbyRadarScreen
import com.sweet.app.feature.nearby.NearbyRadarViewModel
import com.sweet.app.feature.settings.AppearanceSettingsScreen
import com.sweet.app.feature.settings.ThemeViewModel
import com.sweet.app.feature.settings.about.AboutSettingsScreen
import com.sweet.app.feature.settings.discovery.DiscoverySettingsScreen
import com.sweet.app.feature.settings.discovery.DiscoveryViewModel
import com.sweet.app.feature.settings.profile.ProfileSettingsScreen
import com.sweet.app.feature.settings.profile.ProfileViewModel

object Destinations {
    const val AUTH = "auth"
    const val CHAT_LIST = "chat_list"
    const val CONVERSATION = "conversation/{chatId}"
    const val SETTINGS = "settings"
    const val PROFILE_SETTINGS = "profile_settings"
    const val APPEARANCE_SETTINGS = "appearance_settings"
    const val DISCOVERY_SETTINGS = "discovery_settings"
    const val ABOUT = "about"
    const val NEARBY_RADAR = "nearby_radar"
}

@Composable
fun SweetNavGraph(
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.CHAT_LIST
    ) {
        composable(Destinations.AUTH) {
            AuthPlaceholderScreen(
                onLoginSuccess = {
                    navController.navigate(Destinations.CHAT_LIST) {
                        popUpTo(Destinations.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.CHAT_LIST) {
            ChatListPlaceholderScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate("conversation/$chatId")
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onNavigateToNearbyRadar = {
                    navController.navigate(Destinations.NEARBY_RADAR)
                }
            )
        }

        composable(Destinations.CONVERSATION) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            ConversationScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsPlaceholderScreen(
                onNavigateToProfile = {
                    navController.navigate(Destinations.PROFILE_SETTINGS)
                },
                onNavigateToAppearance = {
                    navController.navigate(Destinations.APPEARANCE_SETTINGS)
                },
                onNavigateToDiscovery = {
                    navController.navigate(Destinations.DISCOVERY_SETTINGS)
                },
                onNavigateToAbout = {
                    navController.navigate(Destinations.ABOUT)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.PROFILE_SETTINGS) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileSettingsScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToActiveSessions = {},
                onNavigateToBlockedUsers = {},
                onNavigateToAppearance = {
                    navController.navigate(Destinations.APPEARANCE_SETTINGS)
                }
            )
        }

        composable(Destinations.APPEARANCE_SETTINGS) {
            AppearanceSettingsScreen(
                viewModel = themeViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.DISCOVERY_SETTINGS) {
            val discoveryViewModel: DiscoveryViewModel = hiltViewModel()
            DiscoverySettingsScreen(
                viewModel = discoveryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.ABOUT) {
            AboutSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.NEARBY_RADAR) {
            val nearbyViewModel: NearbyRadarViewModel = hiltViewModel()
            NearbyRadarScreen(
                viewModel = nearbyViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToDiscoverySettings = {
                    navController.navigate(Destinations.DISCOVERY_SETTINGS)
                },
                onStartChat = { userId ->
                    navController.navigate("conversation/$userId")
                }
            )
        }
    }
}
