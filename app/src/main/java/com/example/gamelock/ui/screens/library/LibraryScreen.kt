package com.example.gamelock.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelock.domain.model.GameStatus
import com.example.gamelock.ui.components.GameCard

@Composable
fun LibraryScreen(onGameClick: (Int) -> Unit, viewModel: LibraryViewModel = viewModel()) {
    var selectedStatus by remember { mutableStateOf<GameStatus?>(null) }
    val allGames by viewModel.allGames.collectAsState()
    val playing by viewModel.playingGames.collectAsState()
    val completed by viewModel.completedGames.collectAsState()
    val backlog by viewModel.backlogGames.collectAsState()

    val displayGames = when (selectedStatus) {
        GameStatus.PLAYING   -> playing
        GameStatus.COMPLETED -> completed
        GameStatus.BACKLOG   -> backlog
        else                 -> allGames
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Моя библиотека", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedStatus == null,
                onClick = { selectedStatus = null }, label = { Text("Все") })
            FilterChip(selected = selectedStatus == GameStatus.PLAYING,
                onClick = { selectedStatus = GameStatus.PLAYING }, label = { Text("Играю") })
            FilterChip(selected = selectedStatus == GameStatus.COMPLETED,
                onClick = { selectedStatus = GameStatus.COMPLETED }, label = { Text("Прошёл") })
            FilterChip(selected = selectedStatus == GameStatus.BACKLOG,
                onClick = { selectedStatus = GameStatus.BACKLOG }, label = { Text("В планах") })
        }
        Spacer(Modifier.height(12.dp))
        if (displayGames.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список пустой",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(displayGames) { game ->
                    GameCard(game = game, onClick = { onGameClick(game.id) })
                }
            }
        }
    }
}
