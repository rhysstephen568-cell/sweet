package com.sweet.app.feature.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sweet.app.feature.settings.AppearanceSettingsScreen
import com.sweet.app.feature.settings.ThemeViewModel

object Destinations {
    const val AUTH = "auth"
    const val CHAT_LIST = "chat_list"
    const val CONVERSATION = "conversation/{chatId}"
    const val SETTINGS = "settings"
    const val APPEARANCE_SETTINGS = "appearance_settings"
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
                }
            )
        }

        composable(Destinations.CONVERSATION) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "0"
            ConversationPlaceholderScreen(
                chatId = chatId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsPlaceholderScreen(
                onNavigateToAppearance = {
                    navController.navigate(Destinations.APPEARANCE_SETTINGS)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Destinations.APPEARANCE_SETTINGS) {
            AppearanceSettingsScreen(
                viewModel = themeViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
