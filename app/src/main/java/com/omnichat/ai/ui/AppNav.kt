package com.omnichat.ai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnichat.ai.AppContainer
import com.omnichat.ai.OmniApp
import com.omnichat.ai.ui.chat.ChatScreen
import com.omnichat.ai.ui.history.HistoryScreen
import com.omnichat.ai.ui.providers.ProvidersScreen

@Composable
fun rememberContainer(): AppContainer {
    val ctx = LocalContext.current.applicationContext
    return remember { (ctx as OmniApp).container }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "history") {
        composable("history") {
            HistoryScreen(
                onOpen = { nav.navigate("chat/$it") },
                onProviders = { nav.navigate("providers") },
            )
        }
        composable(
            route = "chat/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            ChatScreen(chatId = entry.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
        }
        composable("providers") {
            ProvidersScreen(onBack = { nav.popBackStack() })
        }
    }
}
