package com.example.gamelock.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.repository.GameRepository
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(AppDatabase.getDatabase(app))
    private val scope = CoroutineScope(Dispatchers.IO)

    val allGames: StateFlow<List<Game>> = repository.getLibraryGames()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playingGames: StateFlow<List<Game>> =
        repository.getGamesByStatus(GameStatus.PLAYING)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedGames: StateFlow<List<Game>> =
        repository.getGamesByStatus(GameStatus.COMPLETED)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backlogGames: StateFlow<List<Game>> =
        repository.getGamesByStatus(GameStatus.BACKLOG)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
}
