package com.example.gamelock.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.repository.GameRepository
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val game: Game) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(AppDatabase.getDatabase(app))

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadGame(gameId: Int) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            repository.getGameDetails(gameId).fold(
                onSuccess = {
                    repository.updateLastAccessed(gameId)
                    _uiState.value = DetailUiState.Success(it)
                },
                onFailure = { _uiState.value = DetailUiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    fun saveGameData(game: Game, status: GameStatus, rating: Float, review: String) {
        viewModelScope.launch {
            val updated = game.copy(userStatus = status, userRating = rating, userReview = review)
            repository.saveGame(updated)
            _uiState.value = DetailUiState.Success(updated)
        }
    }
}
