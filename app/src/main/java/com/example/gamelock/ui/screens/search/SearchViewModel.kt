package com.example.gamelock.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.repository.GameRepository
import com.example.gamelock.domain.model.Game
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val games: List<Game>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(AppDatabase.getDatabase(app))

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _query.debounce(500).filter { it.length >= 2 }
                .collect { searchGames(it) }
        }
    }

    fun onQueryChange(query: String) { _query.value = query }

    private fun searchGames(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.searchGames(query).fold(
                onSuccess = { _uiState.value = SearchUiState.Success(it) },
                onFailure = { _uiState.value = SearchUiState.Error(it.message ?: "Ошибка") }
            )
        }
    }
}
