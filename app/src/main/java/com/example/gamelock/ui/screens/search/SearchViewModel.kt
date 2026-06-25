package com.example.gamelock.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.repository.GameRepository
import com.example.gamelock.domain.model.FilterSort
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.SearchFilters
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Loading : SearchUiState()
    data class Success(val games: List<Game>, val title: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(AppDatabase.getDatabase(app))

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val _activeFilterCount = MutableStateFlow(0)
    val activeFilterCount: StateFlow<Int> = _activeFilterCount.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        fetchPopularGames()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _query
                .debounce(500)
                .collect { q ->
                    val f = _filters.value
                    if (q.length >= 2 || f.activeCount > 0)
                        searchGames(q, f)
                    else
                        fetchPopularGames()
                }
        }
    }

    fun onQueryChange(query: String) { _query.value = query }

    fun onFiltersChanged(filters: SearchFilters) {
        _filters.value = filters
        _activeFilterCount.value = filters.activeCount
        refresh()
    }

    fun onGenreQuickFilter(slug: String) {
        val current = _filters.value
        val newSlug = if (slug.isBlank() || current.genreSlug == slug) null else slug
        onFiltersChanged(current.copy(genreSlug = newSlug))
    }

    fun toggleFilterStore(storeId: Int) {
        val current = _filters.value
        onFiltersChanged(current.copy(
            storeIds = if (storeId in current.storeIds)
                current.storeIds - storeId
            else
                current.storeIds + storeId
        ))
    }

    fun clearGenreFilter() {
        onFiltersChanged(_filters.value.copy(genreSlug = null))
    }

    fun clearRatingFilter() {
        onFiltersChanged(_filters.value.copy(minRating = 0.0))
    }

    fun clearSortFilter() {
        onFiltersChanged(_filters.value.copy(sortBy = FilterSort.RELEVANCE))
    }

    private fun refresh() {
        viewModelScope.launch {
            val q = _query.value
            val f = _filters.value
            _uiState.value = SearchUiState.Loading
            if (q.length >= 2 || f.activeCount > 0) {
                repository.searchGames(q, f).fold(
                    onSuccess = { _uiState.value = SearchUiState.Success(it.sortedByDescending { it.overallRating }, "Результаты поиска") },
                    onFailure = { _uiState.value = SearchUiState.Error(it.message ?: "Ошибка") }
                )
            } else {
                repository.fetchPopularGames().fold(
                    onSuccess = { _uiState.value = SearchUiState.Success(it.sortedByDescending { it.overallRating }, "🔥 Популярные игры") },
                    onFailure = { _uiState.value = SearchUiState.Error(it.message ?: "Ошибка") }
                )
            }
        }
    }

    private fun fetchPopularGames() {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.fetchPopularGames().fold(
                onSuccess = { _uiState.value = SearchUiState.Success(it.sortedByDescending { it.overallRating }, "🔥 Популярные игры") },
                onFailure = { _uiState.value = SearchUiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    private fun searchGames(query: String, filters: SearchFilters) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.searchGames(query, filters).fold(
                onSuccess = { _uiState.value = SearchUiState.Success(it.sortedByDescending { it.overallRating }, "Результаты поиска") },
                onFailure = { _uiState.value = SearchUiState.Error(it.message ?: "Ошибка") }
            )
        }
    }
}
