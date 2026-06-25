package com.example.gamelock.ui.screens.detail


import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.gamelock.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelock.domain.model.GameStatus

private val tabTitles = listOf("📖 Описание", "🛒 Магазины", "⭐ Оценка")

private fun storeEmoji(name: String): String = when {
    name.contains("Steam", ignoreCase = true) -> "🎮"
    name.contains("Epic", ignoreCase = true) -> "🕹️"
    name.contains("GOG", ignoreCase = true) -> "🔴"
    name.contains("Xbox", ignoreCase = true) -> "🎯"
    name.contains("PlayStation", ignoreCase = true) -> "🔵"
    name.contains("Nintendo", ignoreCase = true) -> "🍄"
    else -> "🛍️"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(gameId: Int, onBack: () -> Unit, viewModel: DetailViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(gameId) { viewModel.loadGame(gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали игры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DetailUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            is DetailUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            is DetailUiState.Success -> {
                val game = state.game
                var status by remember { mutableStateOf(game.userStatus) }
                var rating by remember { mutableFloatStateOf(game.userRating) }
                var review by remember { mutableStateOf(game.userReview) }
                val coroutineScope = rememberCoroutineScope()

                val pagerState = rememberPagerState(pageCount = { tabTitles.size })

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    if (game.screenshots.isNotEmpty()) {
                        val screenshotState = rememberPagerState(pageCount = { game.screenshots.size })
                        Box {
                            HorizontalPager(state = screenshotState) { page ->
                                AsyncImage(
                                    model = game.screenshots[page], contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(game.screenshots.size) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (screenshotState.currentPage == i) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (screenshotState.currentPage == i) AccentPrimary
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = game.imageUrl, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(game.name, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            game.storeRatings.forEach { sr ->
                                val bgColor = when (sr.storeName) {
                                    "Steam" -> RatingSteam
                                    "Metacritic" -> RatingMeta
                                    else -> RatingRawg
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = bgColor,
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sr.emoji, fontSize = 16.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text(sr.storeName, fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f))
                                            Text(sr.display, fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RatingOverall,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💎", fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${String.format("%.1f", game.overallRating)}",
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        game.genres?.let {
                            Spacer(Modifier.height(6.dp))
                            Text("• $it", style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary)
                        }
                    }

                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = DarkSurface,
                        contentColor = AccentPrimary,
                        divider = {}
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            when (page) {
                                0 -> {
                                    val hasRuDesc = game.description != null
                                    val hasEnDesc = game.descriptionOriginal != null
                                    if (hasRuDesc || hasEnDesc) {
                                        var showOriginal by remember { mutableStateOf(false) }
                                        val isTranslated = game.description != game.descriptionOriginal
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                when {
                                                    showOriginal -> "🇬🇧 Оригинал"
                                                    else -> "🇷🇺 Перевод"
                                                },
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Spacer(Modifier.weight(1f))
                                            if (hasRuDesc && hasEnDesc) {
                                                TextButton(onClick = { showOriginal = !showOriginal }) {
                                                    Text(
                                                        if (showOriginal) "Показать перевод"
                                                        else "Показать оригинал (EN)",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                        if (!showOriginal && isTranslated) {
                                            Text(
                                                "Переведено Google ML Kit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                            Spacer(Modifier.height(4.dp))
                                        }
                                        val desc = when {
                                            showOriginal -> game.descriptionOriginal
                                            else -> game.description
                                        }
                                        desc?.let {
                                            Text(it, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    } else {
                                        Text("Описание недоступно",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted)
                                    }
                                }
                                1 -> {
                                    Column {
                                        if (game.storeUrls.isEmpty()) {
                                            Text("Нет информации о магазинах",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextMuted,
                                                modifier = Modifier.padding(bottom = 12.dp))
                                        } else {
                                            Text("Доступно в:",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(bottom = 8.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(bottom = 16.dp)) {
                                                game.storeUrls.forEach { store ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = DarkCard
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(storeEmoji(store.name), fontSize = 16.sp)
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(store.name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = TextPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rawg.io/games/${game.id}"))
                                                    context.startActivity(intent)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.15f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("🔗", fontSize = 20.sp)
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Открыть на RAWG.io",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = AccentPrimary,
                                                        fontWeight = FontWeight.SemiBold)
                                                    Text("Все ссылки на магазины",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted)
                                                }
                                                Text("→", fontSize = 18.sp, color = AccentPrimary)
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(AccentPrimary.copy(alpha = 0.3f), AccentPrimary.copy(alpha = 0.05f))
                                            )
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Статус", style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    GameStatus.PLAYING to "Играю",
                                                    GameStatus.COMPLETED to "Прошёл",
                                                    GameStatus.BACKLOG to "В планах"
                                                ).forEach { (s, label) ->
                                                    val chipColor = when (s) {
                                                        GameStatus.PLAYING -> StatusPlaying
                                                        GameStatus.COMPLETED -> StatusCompleted
                                                        else -> StatusBacklog
                                                    }
                                                    FilterChip(
                                                        selected = status == s,
                                                        onClick = { status = s },
                                                        label = { Text(label) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = chipColor.copy(alpha = 0.25f),
                                                            selectedLabelColor = chipColor
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = status == s,
                                                            borderColor = chipColor.copy(alpha = 0.5f),
                                                            selectedBorderColor = chipColor
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))

                                            Text("Моя оценка: ${rating.toInt()}/10",
                                                style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.height(4.dp))
                                            Slider(
                                                value = rating,
                                                onValueChange = { rating = it },
                                                valueRange = 0f..10f,
                                                steps = 9,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = AccentPrimary,
                                                    activeTrackColor = AccentPrimary,
                                                    inactiveTrackColor = AccentPrimary.copy(alpha = 0.2f),
                                                    activeTickColor = Color.White,
                                                    inactiveTickColor = Color.White.copy(alpha = 0.3f)
                                                )
                                            )
                                            Spacer(Modifier.height(16.dp))

                                            Text("Мой отзыв", style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = review,
                                                onValueChange = { review = it },
                                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                                placeholder = { Text("Напиши свои впечатления...") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = AccentPrimary,
                                                    unfocusedBorderColor = CardBorder,
                                                    cursorColor = AccentPrimary,
                                                    focusedLabelColor = AccentPrimary
                                                )
                                            )
                                            Spacer(Modifier.height(20.dp))

                                            Button(
                                                onClick = { viewModel.saveGameData(game, status, rating, review) },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentPrimary
                                                )
                                            ) {
                                                Text(
                                                    "Сохранить",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
