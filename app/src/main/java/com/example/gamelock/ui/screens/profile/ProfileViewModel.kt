package com.example.gamelock.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.local.PreferencesManager
import com.example.gamelock.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileStats(
    val total: Int = 0,
    val playing: Int = 0,
    val completed: Int = 0,
    val backlog: Int = 0
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val username: String,
        val email: String,
        val createdAt: Long,
        val stats: ProfileStats
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)
    private val userDao = db.userDao()
    private val gameDao = db.gameDao()
    private val prefs = PreferencesManager(app)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val userId: Int get() = prefs.currentUserId
    val username: String get() = prefs.currentUsername ?: ""

    init {
        loadProfile()
    }

    fun retry() { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            val user = userDao.getUserById(prefs.currentUserId)
            if (user == null) {
                _uiState.value = ProfileUiState.Error("Пользователь не найден")
                return@launch
            }
            val total = gameDao.countLibraryGames(user.id)
            val playing = gameDao.countGamesByStatus(user.id, "PLAYING")
            val completed = gameDao.countGamesByStatus(user.id, "COMPLETED")
            val backlog = gameDao.countGamesByStatus(user.id, "BACKLOG")
            _uiState.value = ProfileUiState.Success(
                username = user.username,
                email = user.email,
                createdAt = user.createdAt,
                stats = ProfileStats(total, playing, completed, backlog)
            )
        }
    }

    fun logout() {
        prefs.clear()
    }
}
