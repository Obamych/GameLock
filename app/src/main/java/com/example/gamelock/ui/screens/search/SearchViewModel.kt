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
import kotlin.math.ceil

sealed class SearchUiState {
    object Loading : SearchUiState()
    data class Success(
        val games: List<Game>,
        val title: String,
        val currentPage: Int = 1,
        val totalPages: Int = 1
    ) : SearchUiState()
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

    private var currentPage = 1
    private var totalPages = 1
    private var currentQueryStr = ""
    private var currentFilters = SearchFilters()
    private var loadGeneration = 0L

    init {
        fetchPopularGames()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _query
                .debounce(500)
                .collect { q ->
                    currentQueryStr = q
                    currentFilters = _filters.value
                    if (q.length >= 2 || currentFilters.activeCount > 0)
                        searchGames(q, currentFilters)
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

    fun refresh() {
        val q = _query.value
        val f = _filters.value
        if (q.length >= 2 || f.activeCount > 0) {
            searchGames(q, f)
        } else {
            fetchPopularGames()
        }
    }

    fun goToPage(page: Int) {
        if (page < 1 || page > totalPages) return
        val state = _uiState.value
        if (state !is SearchUiState.Success) return
        currentPage = page
        loadGeneration++
        val gen = loadGeneration
        val title = state.title
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val result = if (currentQueryStr.isBlank()) {
                repository.fetchPopularGames(page = page)
            } else {
                repository.searchGames(currentQueryStr, currentFilters, page = page)
            }
            if (gen != loadGeneration) return@launch
            result.fold(
                onSuccess = { (games, hasMore, count) ->
                    totalPages = maxOf(1, ceil(count / 20.0).toInt())
                    _uiState.value = SearchUiState.Success(games, title, page, totalPages)
                },
                onFailure = {
                    currentPage--
                    _uiState.value = SearchUiState.Error(it.message ?: "Ошибка")
                }
            )
        }
    }

    private fun fetchPopularGames() {
        currentPage = 1
        loadGeneration++
        val gen = loadGeneration
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.fetchPopularGames(page = 1).fold(
                onSuccess = { (games, hasMore, count) ->
                    if (gen != loadGeneration) return@launch
                    totalPages = maxOf(1, ceil(count / 20.0).toInt())
                    _uiState.value = SearchUiState.Success(games, "🔥 Популярные игры", 1, totalPages)
                },
                onFailure = {
                    if (gen != loadGeneration) return@launch
                    _uiState.value = SearchUiState.Error(it.message ?: "Ошибка")
                }
            )
        }
    }

    private fun searchGames(query: String, filters: SearchFilters) {
        currentPage = 1
        currentQueryStr = query
        currentFilters = filters
        loadGeneration++
        val gen = loadGeneration
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.searchGames(query, filters, page = 1).fold(
                onSuccess = { (games, hasMore, count) ->
                    if (gen != loadGeneration) return@launch
                    totalPages = maxOf(1, ceil(count / 20.0).toInt())
                    _uiState.value = SearchUiState.Success(games, "Результаты поиска", 1, totalPages)
                },
                onFailure = {
                    if (gen != loadGeneration) return@launch
                    _uiState.value = SearchUiState.Error(it.message ?: "Ошибка")
                }
            )
        }
    }
}
