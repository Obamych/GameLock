package com.example.gamelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import coil.compose.LocalImageLoader
import com.example.gamelock.data.local.PreferencesManager
import com.example.gamelock.data.remote.RetrofitClient
import com.example.gamelock.ui.screens.auth.AuthUiState
import com.example.gamelock.ui.screens.auth.AuthViewModel
import com.example.gamelock.ui.screens.auth.LoginScreen
import com.example.gamelock.ui.screens.auth.RegisterScreen
import com.example.gamelock.ui.screens.detail.DetailScreen
import com.example.gamelock.ui.screens.library.LibraryScreen
import com.example.gamelock.ui.screens.profile.ProfileScreen
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

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val isLoggedIn = remember { prefs.isLoggedIn }

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            navController.navigate("search") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentRoute in listOf("search", "library", "profile")
    val startDest = if (isLoggedIn) "search" else "login"

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
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { launchSingleTop = true } },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Профиль",
                                tint = if (currentRoute == "profile")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                "Профиль",
                                fontWeight = if (currentRoute == "profile") FontWeight.Bold else FontWeight.Normal,
                                color = if (currentRoute == "profile")
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
        NavHost(navController = navController, startDestination = startDest,
            modifier = Modifier.padding(padding)) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = { /* handled by LaunchedEffect above */ }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = { /* handled by LaunchedEffect above */ }
                )
            }
            composable("search") {
                SearchScreen(onGameClick = { id -> navController.navigate("detail/$id") })
            }
            composable("library") {
                LibraryScreen(onGameClick = { id -> navController.navigate("detail/$id") })
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("detail/{gameId}") { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId")?.toInt() ?: return@composable
                DetailScreen(gameId = gameId, onBack = { navController.popBackStack() })
            }
        }
    }
}
