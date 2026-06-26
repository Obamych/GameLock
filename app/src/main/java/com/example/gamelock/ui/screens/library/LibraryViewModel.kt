package com.example.gamelock.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.local.PreferencesManager
import com.example.gamelock.data.repository.GameRepository
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

enum class SortType(val displayName: String) {
    NAME("По названию"),
    RATING("По рейтингу"),
    DATE_ADDED("По дате")
}

data class LibraryStats(
    val total: Int,
    val playing: Int,
    val completed: Int,
    val backlog: Int
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(AppDatabase.getDatabase(app))
    private val prefs = PreferencesManager(app)
    private val userId: Int get() = prefs.currentUserId

    private val _sortType = MutableStateFlow(SortType.DATE_ADDED)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<GameStatus?>(null)
    val selectedStatus: StateFlow<GameStatus?> = _selectedStatus.asStateFlow()

    private val allGames: StateFlow<List<Game>> = repository.getLibraryGames(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<LibraryStats> = allGames.map { games ->
        LibraryStats(
            total = games.size,
            playing = games.count { it.userStatus == GameStatus.PLAYING },
            completed = games.count { it.userStatus == GameStatus.COMPLETED },
            backlog = games.count { it.userStatus == GameStatus.BACKLOG }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats(0, 0, 0, 0))

    val displayGames: StateFlow<List<Game>> = combine(
        allGames, _selectedStatus, _searchQuery, _sortType
    ) { games, status, query, sort ->
        var result = games
        if (status != null) result = result.filter { it.userStatus == status }
        if (query.isNotBlank()) result = result.filter { it.name.contains(query, ignoreCase = true) }
        when (sort) {
            SortType.NAME -> result.sortedBy { it.name }
            SortType.RATING -> result.sortedByDescending { it.overallRating }
            SortType.DATE_ADDED -> result.sortedByDescending { it.lastAccessed ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game, userId)
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onSortChange(sort: SortType) { _sortType.value = sort }

    fun onStatusChange(status: GameStatus?) { _selectedStatus.value = status }
}
