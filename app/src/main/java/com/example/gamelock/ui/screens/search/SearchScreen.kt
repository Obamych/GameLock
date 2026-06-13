package com.example.gamelock.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelock.ui.components.GameCard

@Composable
fun SearchScreen(onGameClick: (Int) -> Unit, viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Поиск игр...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        when (val state = uiState) {
            is SearchUiState.Idle ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Введите название игры",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            is SearchUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is SearchUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ошибка: ${state.message}",
                        color = MaterialTheme.colorScheme.error)
                }
            is SearchUiState.Success ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.games) { game ->
                        GameCard(game = game, onClick = { onGameClick(game.id) })
                    }
                }
        }
    }
}
