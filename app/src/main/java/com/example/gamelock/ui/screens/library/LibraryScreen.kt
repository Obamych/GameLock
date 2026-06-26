package com.example.gamelock.ui.screens.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import com.example.gamelock.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onGameClick: (Int) -> Unit, viewModel: LibraryViewModel = viewModel()) {
    val displayGames by viewModel.displayGames.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }

    if (showDeleteDialog && gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; gameToDelete = null },
            title = { Text("Удалить игру", fontWeight = FontWeight.Bold) },
            text = { Text("Точно удалить «${gameToDelete!!.name}» из библиотеки?") },
            confirmButton = {
                TextButton(onClick = {
                    gameToDelete?.let { viewModel.deleteGame(it) }
                    showDeleteDialog = false
                    gameToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; gameToDelete = null }) {
                    Text("Отмена")
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
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
                    Brush.verticalGradient(colors = listOf(VioletDeep, DarkSurface))
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📚 Моя библиотека",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                    Box {
                        Row(
                            modifier = Modifier.clickable { showSortMenu = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sortType.displayName,
                                color = VioletLight,
                                fontSize = 13.sp
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Сортировка",
                                tint = VioletLight
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortType.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.displayName) },
                                    onClick = {
                                        viewModel.onSortChange(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (stats.total > 0) {
                    val parts = mutableListOf<String>()
                    parts.add("${stats.total} игр")
                    if (stats.playing > 0) parts.add("${stats.playing} играю")
                    if (stats.completed > 0) parts.add("${stats.completed} прошёл")
                    if (stats.backlog > 0) parts.add("${stats.backlog} в планах")
                    Text(
                        parts.joinToString(" · "),
                        fontSize = 13.sp,
                        color = VioletLight
                    )
                    Spacer(Modifier.height(12.dp))
                }

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
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Поиск в библиотеке...",
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
                    }
                }

                Spacer(Modifier.height(12.dp))

                val statuses = listOf(
                    null to "Все",
                    GameStatus.PLAYING to "Играю",
                    GameStatus.COMPLETED to "Прошёл",
                    GameStatus.BACKLOG to "В планах"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { (status, label) ->
                        val isSelected = selectedStatus == status
                        val chipColor = when (status) {
                            GameStatus.PLAYING -> StatusPlaying
                            GameStatus.COMPLETED -> StatusCompleted
                            GameStatus.BACKLOG -> StatusBacklog
                            null, GameStatus.NONE -> AccentPrimary
                        }
                        val countLabel = when (status) {
                            null -> "${stats.total}"
                            GameStatus.PLAYING -> "${stats.playing}"
                            GameStatus.COMPLETED -> "${stats.completed}"
                            GameStatus.BACKLOG -> "${stats.backlog}"
                            else -> ""
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onStatusChange(status) },
                            label = {
                                Text(
                                    if (stats.total > 0) "$label $countLabel" else label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                selectedLabelColor = chipColor,
                                containerColor = Color.Transparent,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = chipColor.copy(alpha = 0.3f),
                                selectedBorderColor = chipColor,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        if (displayGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val emptyContent: Triple<String, String, String> = when (selectedStatus) {
                    GameStatus.PLAYING -> Triple("🎮", "Нет игр в процессе", "Добавь игру и выбери статус «Играю»")
                    GameStatus.COMPLETED -> Triple("✅", "Нет пройденных игр", "")
                    GameStatus.BACKLOG -> Triple("📋", "В планах пока пусто", "")
                    else -> Triple("📚", "Библиотека пуста", "Найди игры в поиске и добавь их в библиотеку")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emptyContent.first, fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        emptyContent.second,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (emptyContent.third.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            emptyContent.third,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 80.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                displayGames.forEachIndexed { localIndex, game ->
                    item(key = game.id) {
                        AnimatedLibraryGameCard(
                            game = game,
                            index = localIndex,
                            onClick = { onGameClick(game.id) },
                            onDelete = { gameToDelete = game; showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryGameCard(
    game: Game,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    AccentPrimary.copy(alpha = 0.4f),
                    AccentPrimary.copy(alpha = 0.1f)
                )
            )
        ),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.weight(1f)) {
                Box {
                    AsyncImage(
                        model = game.imageUrl,
                        contentDescription = game.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val statusInfo: Pair<String, Color>? = when (game.userStatus) {
                        GameStatus.PLAYING -> Pair("Играю", StatusPlaying)
                        GameStatus.COMPLETED -> Pair("Прошёл", StatusCompleted)
                        GameStatus.BACKLOG -> Pair("В планах", StatusBacklog)
                        else -> null
                    }
                    statusInfo?.let { (label, color) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.85f)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    game.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!game.genres.isNullOrBlank()) {
                    Text(
                        game.genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💎", fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        String.format("%.1f", game.overallRating),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VioletLight
                    )
                }
                game.lastAccessed?.let { ts ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        relativeTime(ts),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                if (game.userRating > 0f) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 10.sp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "${String.format("%.1f", game.userRating)}",
                            fontSize = 11.sp,
                            color = VioletLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedLibraryGameCard(
    game: Game,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    LibraryGameCard(
        game = game,
        onClick = onClick,
        onDelete = onDelete,
        modifier = Modifier.graphicsLayer {
            alpha = animProgress.value
            scaleX = 0.9f + 0.1f * animProgress.value
            scaleY = 0.9f + 0.1f * animProgress.value
        }
    )
}

private fun relativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "${minutes} мин. назад"
        hours < 24 -> "${hours} ч. назад"
        days == 1L -> "вчера"
        days < 7 -> "${days} дн. назад"
        days < 30 -> "${days / 7} нед. назад"
        else -> "${days / 30} мес. назад"
    }
}
