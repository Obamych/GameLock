package com.example.gamelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import coil.compose.LocalImageLoader
import com.example.gamelock.data.remote.RetrofitClient
import com.example.gamelock.ui.screens.detail.DetailScreen
import com.example.gamelock.ui.screens.library.LibraryScreen
import com.example.gamelock.ui.screens.search.SearchScreen
import com.example.gamelock.ui.theme.GameLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val imageLoader = remember { RetrofitClient.createImageLoader(context) }
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                GameLockTheme { GameLockApp() }
            }
        }
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "search",
                        onClick = { navController.navigate("search") { launchSingleTop = true } },
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Поиск",
                                tint = if (currentRoute == "search")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                "Поиск",
                                fontWeight = if (currentRoute == "search") FontWeight.Bold else FontWeight.Normal,
                                color = if (currentRoute == "search")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "library",
                        onClick = { navController.navigate("library") { launchSingleTop = true } },
                        icon = {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = "Библиотека",
                                tint = if (currentRoute == "library")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                "Библиотека",
                                fontWeight = if (currentRoute == "library") FontWeight.Bold else FontWeight.Normal,
                                color = if (currentRoute == "library")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
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
