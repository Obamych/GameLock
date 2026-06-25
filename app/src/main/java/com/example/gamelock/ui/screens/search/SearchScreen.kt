package com.example.gamelock.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelock.domain.model.FilterSort
import com.example.gamelock.domain.model.FilterStore
import com.example.gamelock.ui.components.AnimatedGameCard
import com.example.gamelock.ui.components.GameCard
import com.example.gamelock.ui.components.HeroCard
import com.example.gamelock.ui.components.SearchFiltersSheet
import com.example.gamelock.ui.components.ShimmerCard
import com.example.gamelock.ui.theme.*

private data class QuickGenre(val slug: String?, val displayName: String, val emoji: String)

private val quickGenres = listOf(
    QuickGenre(null, "Все", "🔥"),
    QuickGenre("action", "Экшн", "⚔️"),
    QuickGenre("role-playing-games-rpg", "RPG", "🧙"),
    QuickGenre("shooter", "Шутер", "🔫"),
    QuickGenre("puzzle", "Головоломки", "🧩"),
    QuickGenre("racing", "Гонки", "🏎️"),
    QuickGenre("strategy", "Стратегии", "♟️"),
    QuickGenre("adventure", "Приключения", "🗺️"),
    QuickGenre("horror", "Хоррор", "👻"),
    QuickGenre("sports", "Спорт", "⚽"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onGameClick: (Int) -> Unit, viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val activeCount by viewModel.activeFilterCount.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        SearchFiltersSheet(
            current = filters,
            onApply = { viewModel.onFiltersChanged(it) },
            onDismiss = { showSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(VioletDeep, DarkSurface)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    "🎮 GameLock",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Найди свою игру",
                    fontSize = 15.sp,
                    color = VioletLight
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBg.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 12.dp),
                            tint = AccentPrimary
                        )
                        TextField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Название игры...",
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AccentPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        BadgedBox(badge = {
                            if (activeCount > 0) {
                                Badge(containerColor = AccentPrimary) {
                                    Text("$activeCount", fontSize = 10.sp)
                                }
                            }
                        }) {
                            IconButton(onClick = { showSheet = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Фильтры",
                                    tint = if (activeCount > 0) AccentPrimary else TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickGenres) { genre ->
                        val isSelected = filters.genreSlug == genre.slug
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onGenreQuickFilter(genre.slug ?: "") },
                            label = {
                                Text(
                                    "${genre.emoji} ${genre.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = AccentPrimary,
                                containerColor = Color.Transparent,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = AccentPrimary.copy(alpha = 0.3f),
                                selectedBorderColor = AccentPrimary,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        if (activeCount > 0) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.storeIds.forEach { id ->
                    val store = FilterStore.entries.firstOrNull { it.id == id }
                    if (store != null) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleFilterStore(id) },
                            label = { Text("${store.emoji} ${store.displayName}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = AccentPrimary
                            )
                        )
                    }
                }
                filters.genreSlug?.let { slug ->
                    val genre = com.example.gamelock.domain.model.FilterGenre.entries.firstOrNull { it.slug == slug }
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearGenreFilter() },
                        label = { Text(genre?.displayName ?: slug, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
                if (filters.minRating > 0.0) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearRatingFilter() },
                        label = { Text("⭐ от ${String.format("%.1f", filters.minRating)}", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
                if (filters.sortBy != FilterSort.RELEVANCE) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearSortFilter() },
                        label = { Text(filters.sortBy.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (val state = uiState) {
            is SearchUiState.Loading -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) {
                        ShimmerCard(
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            is SearchUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Ошибка",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
            is SearchUiState.Success -> {
                if (state.games.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Попробуй изменить запрос",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp,
                            top = 8.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            HeroCard(
                                game = state.games.first(),
                                onClick = { onGameClick(state.games.first().id) }
                            )
                        }
                        items(
                            items = state.games.drop(1),
                            key = { it.id }
                        ) { game ->
                            AnimatedGameCard(
                                game = game,
                                index = state.games.indexOf(game),
                                onClick = { onGameClick(game.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
