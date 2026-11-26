package com.example.ui.screens

import com.example.ui.components.glassBackground
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.example.ui.theme.ChipBg
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.CinetorBlush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.example.ui.theme.OnAccent
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

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    var pullRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.itemsState) { pullRefreshing = false }
    LaunchedEffect(pullRefreshing) {
        if (pullRefreshing) {
            kotlinx.coroutines.delay(8000)
            pullRefreshing = false
        }
    }
    PullToRefreshBox(
        isRefreshing = pullRefreshing,
        onRefresh = {
            pullRefreshing = true
            onRetry()
        },
        modifier = modifier
            .fillMaxSize()
            .glassBackground()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item(key = "explore_title_search") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(CinetorBlush.copy(alpha = 0.55f), Color.Transparent)))
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
                    color = TextPrimary
                )
                Text(
                    text = "Movies, series, anime & dramas",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search titles…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = TextMuted,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = OnAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    shape = RoundedCornerShape(30.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onExecuteSearch(state.query)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = CrimsonPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.18f))
                        .testTag("explore_search_input")
                )
            }
        }

        // Live autocomplete suggestions
        if (state.suggestions.isNotEmpty()) {
            item(key = "search_suggestions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp)
                ) {
                    state.suggestions.take(6).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    onExecuteSearch(suggestion)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hot searches: numbered chips, top 3 highlighted
        if (state.hotSearches.isNotEmpty() && state.query.isBlank()) {
            item(key = "hot_searches") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trending searches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.hotSearches.take(10).forEachIndexed { idx, hot ->
                        val top = idx < 3
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (top) CinetorRedSoft else ObsidianCard)
                                .border(1.dp, if (top) CrimsonPrimary.copy(alpha = 0.35f) else ObsidianBorder, CircleShape)
                                .clickable { onExecuteSearch(hot) }
                                .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (top) CrimsonPrimary else ChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                                    color = if (top) OnAccent else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = hot,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Browse by language / country (MovieBox-style)
        item(key = "browse_language") {
            Spacer(modifier = Modifier.height(18.dp))
            com.example.ui.components.BrowseTiles("Browse by language", com.example.ui.components.BrowseCatalog.languages)
        }
        item(key = "browse_country") {
            Spacer(modifier = Modifier.height(18.dp))
            com.example.ui.components.BrowseTiles("Browse by country", com.example.ui.components.BrowseCatalog.countries)
        }

        // OTT platform brand tiles
        item(key = "ott_platforms") {
            Spacer(modifier = Modifier.height(18.dp))
            SectionHeaderRow(title = "OTT Hubs")
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filterOptions.platforms) { (key, label) ->
                    val selected = state.selectedPlatform == key
                    val brand = ottBrandColor(label)
                    Box(
                        modifier = Modifier
                            .width(132.dp)
                            .height(58.dp)
                            .shadow(if (selected) 8.dp else 3.dp, RoundedCornerShape(16.dp), spotColor = brand)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(brand, lerp(brand, Color.Black, 0.45f))))
                            .then(
                                if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(16.dp)) else Modifier
                            )
                            .clickable {
                                if (selected) onSelectPlatform(null, "All") else onSelectPlatform(key, label)
                            }
                            .testTag("platform_chip_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val domain = ottDomain(label)
                            if (domain != null) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://www.google.com/s2/favicons?domain=$domain&sz=128",
                                        contentDescription = "$label logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = brand, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Dynamic categories from the API
        if (state.categories.isNotEmpty()) {
            item(key = "api_categories") {
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories.take(40), key = { it.id }) { chip ->
                        ExplorePill(
                            label = chip.name,
                            selected = state.selectedCategoryChip?.id == chip.id,
                            onClick = { onSelectCategoryChip(chip) }
                        )
                    }
                }
            }
        }

        // Filters: segmented type control + genre + year rows
        item(key = "filter_rows") {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(ChipBg)
                    .padding(4.dp)
            ) {
                state.filterOptions.categories.forEach { (key, label) ->
                    val selected = state.selectedCategory == key && state.selectedPlatform == null && state.query.isBlank()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (selected) CrimsonPrimary else Color.Transparent)
                            .clickable { onUpdateFilter(key, null, null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (selected) OnAccent else TextPrimary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterLabelRow(
                label = "Genre",
                options = listOf("All") + state.filterOptions.genres,
                isSelected = { state.selectedGenre.equals(it, ignoreCase = true) },
                onSelect = { onUpdateFilter(null, it, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterLabelRow(
                label = "Year",
                options = listOf("All") + state.filterOptions.years,
                isSelected = { state.selectedYear == it },
                onSelect = { onUpdateFilter(null, null, it) }
            )
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
                            subtitle = ""
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
}


private fun ottBrandColor(label: String): Color {
    val l = label.lowercase()
    return when {
        "netflix" in l -> Color(0xFFE50914)
        "hoichoi" in l -> Color(0xFFE8303A)
        "prime" in l || "amazon" in l -> Color(0xFF00A8E1)
        "zee" in l -> Color(0xFF8230C6)
        "hotstar" in l || "disney" in l || "jio" in l -> Color(0xFF1F3A93)
        "sony" in l -> Color(0xFF2B2B2B)
        "chorki" in l -> Color(0xFFF15A24)
        "bongo" in l -> Color(0xFF00A651)
        "apple" in l -> Color(0xFF333333)
        "hbo" in l || "max" in l -> Color(0xFF5B2C9F)
        "crunchyroll" in l -> Color(0xFFF47521)
        "aha" in l -> Color(0xFFFF6F00)
        else -> Color(0xFF6B7280)
    }
}

@Composable
private fun ExplorePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) CrimsonPrimary else ObsidianCard)
            .border(1.dp, if (selected) CrimsonPrimary else ObsidianBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (selected) OnAccent else TextPrimary.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

@Composable
private fun FilterLabelRow(
    label: String,
    options: List<String>,
    isSelected: (String) -> Boolean,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = TextSecondary,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
        items(options) { opt ->
            ExplorePill(label = opt, selected = isSelected(opt), onClick = { onSelect(opt) })
        }
    }
}


/** Official website of each OTT service, used to show its real logo icon. */
private fun ottDomain(label: String): String? {
    val l = label.lowercase()
    return when {
        "netflix" in l -> "netflix.com"
        "hoichoi" in l -> "hoichoi.tv"
        "prime" in l || "amazon" in l -> "primevideo.com"
        "zee" in l -> "zee5.com"
        "hotstar" in l -> "hotstar.com"
        "disney" in l -> "disneyplus.com"
        "jio" in l -> "jiocinema.com"
        "sony" in l -> "sonyliv.com"
        "chorki" in l -> "chorki.com"
        "bongo" in l -> "bongobd.com"
        "apple" in l -> "tv.apple.com"
        "hbo" in l || "max" in l -> "max.com"
        "crunchyroll" in l -> "crunchyroll.com"
        "aha" in l -> "aha.video"
        "viu" in l -> "viu.com"
        "mx" in l -> "mxplayer.in"
        "binge" in l -> "binge.buzz"
        "iscreen" in l -> "iscreen.com.bd"
        "toffee" in l -> "toffeelive.com"
        "youtube" in l -> "youtube.com"
        "paramount" in l -> "paramountplus.com"
        "peacock" in l -> "peacocktv.com"
        "hulu" in l -> "hulu.com"
        else -> null
    }
}
