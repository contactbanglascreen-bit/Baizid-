package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryChip
import com.example.data.model.MediaItem
import com.example.ui.components.ApiErrorOrKeyCard
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.PosterCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.ShimmerPosterRow
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ExploreUiState
import com.example.ui.viewmodel.UiState

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onQueryChanged: (String) -> Unit,
    onExecuteSearch: (String?) -> Unit,
    onSelectPlatform: (String?, String) -> Unit,
    onSelectCategoryChip: (CategoryChip) -> Unit,
    onUpdateFilter: (category: String?, genre: String?, year: String?) -> Unit,
    onLoadMore: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item(key = "explore_title_search") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Explore & Search",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CrimsonPrimary
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onExecuteSearch(state.query)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }
        }

        // Live autocomplete suggestions
        if (state.suggestions.isNotEmpty()) {
            item(key = "search_suggestions") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        state.suggestions.take(6).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        onExecuteSearch(suggestion)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hot searches
        if (state.hotSearches.isNotEmpty() && state.query.isBlank()) {
            item(key = "hot_searches") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hot Searches",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.hotSearches.take(12)) { hot ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianCard,
                            modifier = Modifier
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable { onExecuteSearch(hot) }
                        ) {
                            Text(
                                text = hot,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // OTT Platforms strip
        item(key = "ott_platforms") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeaderRow(title = "OTT Hubs")
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filterOptions.platforms) { (key, label) ->
                    val selected = state.selectedPlatform == key
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) onSelectPlatform(null, "All") else onSelectPlatform(key, label)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = ObsidianBackground,
                            containerColor = ObsidianCard,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = CinemaGold
                        ),
                        modifier = Modifier.testTag("platform_chip_$key")
                    )
                }
            }
        }

        // Dynamic Categories from /api/v1/categories (OTTs, Collections, Languages, Genres, WWE)
        if (state.categories.isNotEmpty()) {
            item(key = "api_categories") {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories.take(40), key = { it.id }) { chip ->
                        val selected = state.selectedCategoryChip?.id == chip.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) CrimsonPrimary.copy(alpha = 0.22f) else ObsidianSurface,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (selected) CrimsonPrimary else ObsidianBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectCategoryChip(chip) }
                        ) {
                            Text(
                                text = chip.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) TextPrimary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Multi-Filter rows: Type, Genre, Year
        item(key = "filter_rows") {
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filterOptions.categories) { (key, label) ->
                    val selected = state.selectedCategory == key && state.selectedPlatform == null && state.query.isBlank()
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateFilter(key, null, null) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = TextPrimary,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = CrimsonPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val genres = listOf("All") + state.filterOptions.genres
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    val selected = state.selectedGenre.equals(genre, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateFilter(null, genre, null) },
                        label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = CrimsonPrimary,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = CrimsonPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val years = listOf("All") + state.filterOptions.years
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(years) { year ->
                    val selected = state.selectedYear == year
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateFilter(null, null, year) },
                        label = { Text(year, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = CrimsonPrimary,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = CrimsonPrimary
                        )
                    )
                }
            }
        }

        // Results Section Header
        item(key = "catalog_header") {
            Spacer(modifier = Modifier.height(18.dp))
            SectionHeaderRow(title = state.activeCatalogTitle)
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (val itemsState = state.itemsState) {
            is UiState.Loading -> {
                item(key = "explore_shimmer") {
                    ShimmerPosterRow()
                    Spacer(modifier = Modifier.height(14.dp))
                    ShimmerPosterRow()
                }
            }

            is UiState.Error -> {
                item(key = "explore_error") {
                    ApiErrorOrKeyCard(
                        message = itemsState.message,
                        isApiKeyMissing = itemsState.isApiKeyMissing,
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Success -> {
                val list = itemsState.data
                if (list.isEmpty()) {
                    item(key = "explore_empty") {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.Search,
                            title = "No titles matched",
                            subtitle = "Try a different keyword, genre, or switch source between MovieBox & MovieLinkBD."
                        )
                    }
                } else {
                    items(list.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                PosterCard(
                                    item = item,
                                    onClick = { onOpenMedia(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (state.canLoadMore) {
                        item(key = "load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = onLoadMore,
                                    enabled = !state.isLoadingMore,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = CrimsonPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading…", color = TextPrimary)
                                    } else {
                                        Text("Load More Titles", color = TextPrimary)
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
