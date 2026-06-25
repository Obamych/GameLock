package com.example.gamelock.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.gamelock.ui.theme.AccentPrimary
import com.example.gamelock.ui.theme.AccentSoft
import com.example.gamelock.ui.theme.CardBorder
import com.example.gamelock.ui.theme.DarkCard
import com.example.gamelock.ui.theme.DarkSurface
import com.example.gamelock.ui.theme.TextSecondary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gamelock.domain.model.FilterGenre
import com.example.gamelock.domain.model.FilterSort
import com.example.gamelock.domain.model.FilterStore
import com.example.gamelock.domain.model.SearchFilters
import com.example.gamelock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    current: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var storeIds by remember(current) { mutableStateOf(current.storeIds) }
    var genreSlug by remember(current) { mutableStateOf(current.genreSlug) }
    var minRating by remember(current) { mutableStateOf(current.minRating) }
    var sortBy by remember(current) { mutableStateOf(current.sortBy) }
    var genreExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Фильтры",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AccentPrimary)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Text("Платформа", style = MaterialTheme.typography.titleSmall,
                color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterStore.entries.forEach { store ->
                    FilterChip(
                        selected = store.id in storeIds,
                        onClick = {
                            storeIds = if (store.id in storeIds)
                                storeIds - store.id
                            else
                                storeIds + store.id
                        },
                        label = { Text("${store.emoji} ${store.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Text("Жанр", style = MaterialTheme.typography.titleSmall,
                color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = genreExpanded,
                onExpandedChange = { genreExpanded = it }
            ) {
                OutlinedTextField(
                    value = FilterGenre.entries.firstOrNull { it.slug == genreSlug }?.displayName ?: "Все",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = AccentPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = genreExpanded,
                    onDismissRequest = { genreExpanded = false },
                    containerColor = DarkCard
                ) {
                    DropdownMenuItem(
                        text = { Text("Все") },
                        onClick = { genreSlug = null; genreExpanded = false }
                    )
                    FilterGenre.entries.forEach { genre ->
                        DropdownMenuItem(
                            text = { Text(genre.displayName) },
                            onClick = { genreSlug = genre.slug; genreExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Text("Рейтинг от ${String.format("%.1f", minRating)}",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Slider(
                value = minRating.toFloat(),
                onValueChange = { minRating = it.toDouble() },
                valueRange = 0f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = AccentPrimary,
                    activeTrackColor = AccentPrimary,
                    inactiveTrackColor = AccentPrimary.copy(alpha = 0.2f)
                )
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Text("Сортировка", style = MaterialTheme.typography.titleSmall,
                color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterSort.entries.forEach { sort ->
                    FilterChip(
                        selected = sortBy == sort,
                        onClick = { sortBy = sort },
                        label = { Text(sort.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        storeIds = emptySet()
                        genreSlug = null
                        minRating = 0.0
                        sortBy = FilterSort.RELEVANCE
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentPrimary
                    )
                ) { Text("Сбросить") }
                Button(
                    onClick = {
                        onApply(SearchFilters(storeIds, genreSlug, minRating, sortBy))
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    )
                ) {
                    Text("Применить", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
