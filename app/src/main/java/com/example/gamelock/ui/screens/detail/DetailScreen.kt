package com.example.gamelock.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelock.domain.model.GameStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(gameId: Int, onBack: () -> Unit, viewModel: DetailViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(gameId) { viewModel.loadGame(gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали игры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DetailUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is DetailUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            is DetailUiState.Success -> {
                val game = state.game
                var status by remember { mutableStateOf(game.userStatus) }
                var rating by remember { mutableFloatStateOf(game.userRating) }
                var review by remember { mutableStateOf(game.userReview) }

                Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                    if (game.screenshots.isNotEmpty()) {
                        val pagerState = rememberPagerState { game.screenshots.size }
                        HorizontalPager(state = pagerState) { page ->
                            AsyncImage(
                                model = game.screenshots[page], contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        AsyncImage(
                            model = game.imageUrl, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(game.name, style = MaterialTheme.typography.headlineSmall)
                        Text("⭐ ${String.format("%.1f", game.rating)} • ${game.genres ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        game.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                        }
                        Text("Статус", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                GameStatus.PLAYING to "Играю",
                                GameStatus.COMPLETED to "Прошёл",
                                GameStatus.BACKLOG to "В планах"
                            ).forEach { (s, label) ->
                                FilterChip(selected = status == s,
                                    onClick = { status = s },
                                    label = { Text(label) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Моя оценка: ${rating.toInt()}/10",
                            style = MaterialTheme.typography.titleMedium)
                        Slider(value = rating, onValueChange = { rating = it },
                            valueRange = 0f..10f, steps = 9)
                        Spacer(Modifier.height(16.dp))
                        Text("Мой отзыв", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = review, onValueChange = { review = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Напиши свои впечатления...") }
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.saveGameData(game, status, rating, review) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Сохранить") }
                    }
                }
            }
        }
    }
}
