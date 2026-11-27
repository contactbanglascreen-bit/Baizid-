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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import com.example.ui.components.glassAccent
import com.example.ui.components.glassBackground
import com.example.ui.components.glassBubble
import com.example.ui.components.liquidGlass
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Where the 18+ section gets its titles from (set in MainActivity). */
object AdultSection {
    @Volatile var loader: (suspend (page: Int) -> List<MediaItem>)? = null
    @Volatile var searcher: (suspend (query: String) -> List<MediaItem>)? = null
}

private val adultTabs = listOf("For You", "Hottest", "Latest", "Rating", "Movies", "Series")

/** Titles kept while the app is open, so leaving and coming back doesn't load them again. */
private val adultPool = mutableStateListOf<MediaItem>()
private var adultPage = 0
private var adultEnd = false

private fun MediaItem.ratingValue(): Double = rating.substringBefore("/").trim().toDoubleOrNull() ?: 0.0
private fun MediaItem.yearValue(): Int = year.take(4).toIntOrNull() ?: 0

/**
 * The PIN-locked 18+ section. It is the only place adult titles show up; everywhere else in the
 * app keeps them hidden. Tabs: For You, Hottest, Latest, Rating, Movies, Series.
 */
@Composable
fun AdultScreen(onBack: () -> Unit, onLock: () -> Unit, onOpenMedia: (MediaItem) -> Unit, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(adultTabs.first()) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>?>(null) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { results = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(450)
        results = null
        val found = runCatching { AdultSection.searcher?.invoke(q).orEmpty() }.getOrDefault(emptyList())
        com.example.data.local.AdultTitles.addAll(found)
        results = found
    }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val grid = rememberLazyGridState()

    fun loadMore() {
        if (loading || adultEnd) return
        val loader = AdultSection.loader ?: return
        loading = true
        scope.launch {
            val next = adultPage + 1
            val got = runCatching { loader(next) }.getOrDefault(emptyList())
            val known = adultPool.map { it.source + it.id }.toHashSet()
            val fresh = got.filter { known.add(it.source + it.id) }
            com.example.data.local.AdultTitles.addAll(fresh)
            adultPool.addAll(fresh)
            adultPage = next
            if (fresh.isEmpty()) adultEnd = true
            loading = false
        }
    }

    LaunchedEffect(reload) { if (adultPool.isEmpty()) loadMore() }

    val shown by remember(tab) {
        derivedStateOf {
            val all = adultPool.toList()
            when (tab) {
                // Same order all day, new mix every day.
                "For You" -> all.shuffled(java.util.Random(LocalDate.now().toEpochDay()))
                "Hottest" -> all
                "Latest" -> all.sortedByDescending { it.yearValue() }
                "Rating" -> all.filter { it.ratingValue() > 0 }.sortedByDescending { it.ratingValue() }
                "Movies" -> all.filter { it.mediaType == MediaCategoryType.MOVIE }
                "Series" -> all.filter { it.mediaType != MediaCategoryType.MOVIE }
                else -> all
            }
        }
    }

    // Load the next page when the end of the grid comes near.
    val nearEnd by remember {
        derivedStateOf {
            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= grid.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, adultPool.size) { if (nearEnd && adultPool.isNotEmpty()) loadMore() }

    Column(modifier.fillMaxSize().glassBackground()) {
        Column(
            Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 12.dp)
        ) {
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF4D6D), Color(0xFF9D0208)))),
                    contentAlignment = Alignment.Center
                ) { Text("18+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("18+ Zone", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("PIN দিয়ে লক করা • শুধু প্রাপ্তবয়স্কদের জন্য", color = TextSecondary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    Modifier.clip(CircleShape).glassBubble(CircleShape).clickable(onClick = onLock)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().glassBubble(RoundedCornerShape(18.dp)).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(androidx.compose.material.icons.Icons.Rounded.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                androidx.compose.material3.TextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    placeholder = { Text("18+ এ খুঁজুন…", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = CinetorRed
                    )
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (query.trim().length < 2) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(adultTabs, key = { it }) { t ->
                    val sel = t == tab
                    Box(
                        Modifier.height(34.dp)
                            .then(if (sel) Modifier.glassAccent(CircleShape) else Modifier.glassBubble(CircleShape))
                            .clickable { tab = t }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t, color = if (sel) Color.White else TextPrimary, fontSize = 13.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        val searching = query.trim().length >= 2
        when {
            searching && results == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinetorRed)
            }
            searching -> {
                val found = results.orEmpty()
                if (found.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কিছু পাওয়া যায়নি", color = TextSecondary)
                } else LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(found, key = { it.source + it.id }) { item -> AdultTile(item) { onOpenMedia(item) } }
                }
            }
            adultPool.isEmpty() && loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinetorRed)
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("কিছু পাওয়া যায়নি", color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "আবার চেষ্টা করুন",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(CinetorRed).clickable {
                            adultEnd = false; adultPage = 0; reload++
                        }.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
            else -> LazyVerticalGrid(
                state = grid,
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(tab, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
                items(shown, key = { it.source + it.id }) { item -> AdultTile(item) { onOpenMedia(item) } }
                if (loading) item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CinetorRed, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultTile(item: MediaItem, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp))
                .background(Color(0x33000000))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = item.posterUrl.ifBlank { item.backdropUrl },
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                "18+",
                color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color(0xE6B00020)).padding(horizontal = 5.dp, vertical = 2.dp)
            )
            if (item.ratingValue() > 0) {
                Row(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFC94D), modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(String.format("%.1f", item.ratingValue()), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
        if (item.year.isNotBlank()) Text(item.year.take(4), color = TextMuted, fontSize = 10.5.sp)
    }
}
