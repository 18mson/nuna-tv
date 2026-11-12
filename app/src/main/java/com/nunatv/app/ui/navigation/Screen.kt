package com.nunatv.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object YoutubeTv : Screen("youtube_tv", "YouTube", Icons.Default.SmartDisplay)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Player : Screen("player/{filePath}/{title}", "Player", Icons.Default.SmartDisplay) {
        fun createRoute(filePath: String, title: String): String {
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "player/$encodedPath/$encodedTitle"
        }
    }
}
