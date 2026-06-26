package com.example.gamelock.ui.screens.detail


import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelock.domain.model.GameStatus

private fun tabTitles(game: com.example.gamelock.domain.model.Game?): List<String> {
    val reviewTitle = when {
        game != null && game.steamReviews.isNotEmpty() -> "💬 Steam"
        game != null && game.redditPosts.isNotEmpty() -> "💬 Reddit"
        else -> "💬 Отзывы"
    }
    return listOf("⭐ Оценка", "📖 Описание", "🛒 Магазины", reviewTitle)
}

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
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gameId) { viewModel.loadGame(gameId) }
    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { snackbarHostState.showSnackbar("Сохранено") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Повторить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            is DetailUiState.Success -> {
                val game = state.game
                var status by remember { mutableStateOf(game.userStatus) }
                var rating by remember { mutableFloatStateOf(game.userRating) }
                var review by remember { mutableStateOf(game.userReview) }
                var showFullScreen by remember { mutableStateOf(false) }
                var fullScreenPage by remember { mutableIntStateOf(0) }
                val coroutineScope = rememberCoroutineScope()

                val pagerState = rememberPagerState(pageCount = { tabTitles(game).size })
                val titles = tabTitles(game)

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    if (game.screenshots.isNotEmpty()) {
                        val hasTrailer = game.trailerUrl != null
                        val pageCount = game.screenshots.size + (if (hasTrailer) 1 else 0)
                        val screenshotState = rememberPagerState(pageCount = { pageCount })
                        Box {
                            HorizontalPager(state = screenshotState) { page ->
                                if (page < game.screenshots.size) {
                                    AsyncImage(
                                        model = game.screenshots[page], contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(200.dp).clickable {
                                            fullScreenPage = page
                                            showFullScreen = true
                                        },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                            .clickable {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(game.trailerUrl)))
                                            }
                                    ) {
                                        AsyncImage(
                                            model = game.clipPreview ?: game.screenshots.lastOrNull(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("▶", fontSize = 48.sp, color = Color.White)
                                                Spacer(Modifier.height(4.dp))
                                                Text("Смотреть трейлер",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(pageCount) { i ->
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
                        game.released?.let {
                            Text("🗓 $it", style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        }
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
                        if (game.developers.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("👨‍💻 ${game.developers.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        if (game.publishers.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text("📢 ${game.publishers.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        if (game.tags.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                game.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DarkCard,
                                        tonalElevation = 2.dp
                                    ) {
                                        Text(tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                        game.esrbRating?.let {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (it) {
                                    "M" -> RatingMeta
                                    "E" -> Color(0xFF4CAF50)
                                    "T" -> Color(0xFFFF9800)
                                    else -> DarkCard
                                }
                            ) {
                                Text("🔞 $it", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                    }

                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = DarkSurface,
                        contentColor = AccentPrimary,
                        divider = {}
                    ) {
                        titles.forEachIndexed { index, title ->
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
                                1 -> {
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
                                        Spacer(Modifier.height(4.dp))
                                        val sourceEmoji = if (game.descriptionSource == "Steam") "🎮" else "⭐"
                                        Text("Источник: $sourceEmoji ${game.descriptionSource}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentPrimary)
                                        Spacer(Modifier.height(4.dp))
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
                                2 -> {
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
                                0 -> {
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

                                            val canSave = status != GameStatus.NONE && rating > 0f

                                            Button(
                                                onClick = { viewModel.saveGameData(game, status, rating, review) },
                                                enabled = canSave,
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentPrimary,
                                                    disabledContainerColor = AccentPrimary.copy(alpha = 0.3f)
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
                                 3 -> {
                                    val steamReviews = game.steamReviews
                                    if (steamReviews.isNotEmpty()) {
                                        steamReviews.take(20).forEach { review ->
                                            val emoji = if (review.votedUp) "👍" else "👎"
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(emoji, fontSize = 16.sp)
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(review.authorName,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.SemiBold)
                                                        Spacer(Modifier.weight(1f))
                                                        Text("+${review.votesUp}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (review.votedUp) StatusPlaying else TextMuted)
                                                    }
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(review.text,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted,
                                                        maxLines = 5,
                                                        overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    } else if (game.redditPosts.isNotEmpty()) {
                                        game.redditPosts.forEach { post ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.url))
                                                        context.startActivity(intent)
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("📢", fontSize = 14.sp)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(post.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis)
                                                    }
                                                    post.text?.let { text ->
                                                        if (text.isNotBlank()) {
                                                            Spacer(Modifier.height(6.dp))
                                                            val maxLength = 200
                                                            val displayText = if (text.length > maxLength) text.take(maxLength) + "..." else text
                                                            Text(displayText,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = TextMuted,
                                                                maxLines = 3,
                                                                overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("👤 ${post.username}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted)
                                                        Spacer(Modifier.weight(1f))
                                                        Text("🗓 ${post.created.take(10)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Нет отзывов",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted)
                                    }
                                }

                            }
                        }
                    }
                }
                if (showFullScreen) {
                    Dialog(
                        onDismissRequest = { showFullScreen = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            val fullPagerState = rememberPagerState(pageCount = { game.screenshots.size })
                            LaunchedEffect(fullScreenPage) {
                                fullPagerState.scrollToPage(fullScreenPage)
                            }
                            HorizontalPager(state = fullPagerState) { page ->
                                AsyncImage(
                                    model = game.screenshots[page],
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            IconButton(
                                onClick = { showFullScreen = false },
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                            }
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(game.screenshots.size) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (fullPagerState.currentPage == i) 10.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(if (fullPagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.4f))
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


