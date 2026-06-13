package com.example.gamelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.gamelock.ui.screens.detail.DetailScreen
import com.example.gamelock.ui.screens.library.LibraryScreen
import com.example.gamelock.ui.screens.search.SearchScreen
import com.example.gamelock.ui.theme.GameLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameLockTheme { GameLockApp() } }
    }
}

@Composable
fun GameLockApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "detail/{gameId}"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "search",
                        onClick = { navController.navigate("search") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Поиск") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "library",
                        onClick = { navController.navigate("library") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.LibraryBooks, null) },
                        label = { Text("Библиотека") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "search",
            modifier = Modifier.padding(padding)) {
            composable("search") {
                SearchScreen(onGameClick = { id -> navController.navigate("detail/$id") })
            }
            composable("library") {
                LibraryScreen(onGameClick = { id -> navController.navigate("detail/$id") })
            }
            composable("detail/{gameId}") { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId")?.toInt() ?: return@composable
                DetailScreen(gameId = gameId, onBack = { navController.popBackStack() })
            }
        }
    }
}
